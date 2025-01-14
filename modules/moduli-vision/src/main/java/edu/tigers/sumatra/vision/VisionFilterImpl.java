/*
 * Copyright (c) 2009 - 2021, DHBW Mannheim - TIGERs Mannheim
 */

package edu.tigers.sumatra.vision;

import com.github.g3force.configurable.ConfigRegistration;
import com.github.g3force.configurable.Configurable;
import edu.tigers.sumatra.cam.data.CamCalibration;
import edu.tigers.sumatra.cam.data.CamDetectionFrame;
import edu.tigers.sumatra.cam.data.CamGeometry;
import edu.tigers.sumatra.drawable.DrawableAnnotation;
import edu.tigers.sumatra.drawable.DrawableArrow;
import edu.tigers.sumatra.drawable.DrawableCircle;
import edu.tigers.sumatra.drawable.DrawablePoint;
import edu.tigers.sumatra.drawable.IDrawableShape;
import edu.tigers.sumatra.drawable.ShapeMap;
import edu.tigers.sumatra.ids.BotID;
import edu.tigers.sumatra.math.rectangle.IRectangle;
import edu.tigers.sumatra.math.vector.IVector2;
import edu.tigers.sumatra.math.vector.IVector3;
import edu.tigers.sumatra.math.vector.Vector2;
import edu.tigers.sumatra.math.vector.Vector2f;
import edu.tigers.sumatra.thread.NamedThreadFactory;
import edu.tigers.sumatra.util.Safe;
import edu.tigers.sumatra.vision.BallFilter.BallFilterOutput;
import edu.tigers.sumatra.vision.BallFilterPreprocessor.BallFilterPreprocessorOutput;
import edu.tigers.sumatra.vision.ViewportArchitect.IViewportArchitect;
import edu.tigers.sumatra.vision.data.EVisionFilterShapesLayer;
import edu.tigers.sumatra.vision.data.FilteredVisionBall;
import edu.tigers.sumatra.vision.data.FilteredVisionBot;
import edu.tigers.sumatra.vision.data.FilteredVisionFrame;
import edu.tigers.sumatra.vision.data.IBallModelIdentificationObserver;
import edu.tigers.sumatra.vision.data.KickEvent;
import edu.tigers.sumatra.vision.kick.estimators.IBallModelIdentResult;
import edu.tigers.sumatra.vision.tracker.BallTracker;
import edu.tigers.sumatra.vision.tracker.RobotTracker;
import lombok.extern.log4j.Log4j2;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;


/**
 * Vision filter implementation.
 */
@Log4j2
public class VisionFilterImpl extends AVisionFilter
		implements IViewportArchitect, IBallModelIdentificationObserver
{
	@Configurable(defValue = "0.0125", comment = "Publish frequency (requires restart)")
	private static double publishDt = 0.0125;

	static
	{
		ConfigRegistration.registerClass("vision", VisionFilterImpl.class);
	}

	private final BallFilterPreprocessor ballFilterPreprocessor = new BallFilterPreprocessor();
	private final BallFilter ballFilter = new BallFilter();
	private final QualityInspector qualityInspector = new QualityInspector();
	private final ViewportArchitect viewportArchitect = new ViewportArchitect();
	private final RobotQualityInspector robotQualityInspector = new RobotQualityInspector();

	private Map<Integer, CamFilter> cams = new ConcurrentHashMap<>();
	private FilteredVisionFrame lastFrame = FilteredVisionFrame.createEmptyFrame();
	private KickEvent lastKickEvent;
	private BallFilterOutput lastBallFilterOutput = new BallFilterOutput(
			lastFrame.getBall(),
			lastFrame.getBall().getPos(),
			new BallFilterPreprocessorOutput(null, null, null)
	);

	private ScheduledExecutorService publisherExecutor;


	private void publish()
	{
		lastFrame = constructFilteredVisionFrame(lastFrame);
		var extrapolatedFrame = extrapolateFilteredFrame(lastFrame, lastFrame.getTimestamp());
		publishFilteredVisionFrame(extrapolatedFrame);
	}


	private FilteredVisionFrame extrapolateFilteredFrame(final FilteredVisionFrame frame, final long timestampFuture)
	{
		final long timestampNow = frame.getTimestamp();

		if (timestampFuture < timestampNow)
		{
			return frame;
		}

		List<FilteredVisionBot> extrapolatedBots = frame.getBots().stream()
				.map(b -> b.extrapolate(timestampNow, timestampFuture))
				.collect(Collectors.toList());

		// construct extrapolated vision frame
		return FilteredVisionFrame.builder()
				.withId(frame.getId())
				.withTimestamp(timestampFuture)
				.withBall(frame.getBall().extrapolate(timestampNow, timestampFuture))
				.withBots(extrapolatedBots)
				.withKickEvent(frame.getKickEvent().orElse(null))
				.withKickFitState(frame.getKickFitState().orElse(null))
				.withShapeMap(frame.getShapeMap())
				.build();
	}


	@Override
	protected void updateCamDetectionFrame(final CamDetectionFrame camDetectionFrame)
	{
		if (camDetectionFrame.gettCapture() <= 0)
		{
			// skip negative timestamps. They can produce unexpected behavior
			return;
		}
		processCamDetectionFrame(camDetectionFrame);
		if (publisherExecutor == null)
		{
			publish();
		}
	}


	private void processCamDetectionFrame(CamDetectionFrame camDetectionFrame)
	{
		int camId = camDetectionFrame.getCameraId();

		// let viewport architect adjust
		viewportArchitect.newDetectionFrame(camDetectionFrame);

		// add camera if it does not exist yet
		var camFilter = cams.computeIfAbsent(camId, CamFilter::new);

		// set viewport
		camFilter.updateViewport(viewportArchitect.getViewport(camId));

		// update robot infos on all camera filters
		camFilter.setRobotInfoMap(getRobotInfoMap());

		// set latest ball info on all camera filters (to generate virtual balls from barrier info)
		camFilter.setBallInfo(lastBallFilterOutput);

		// update camera filter with new detection frame
		camFilter.update(camDetectionFrame, lastFrame);

		// update robot quality inspector
		camDetectionFrame.getRobots().forEach(robotQualityInspector::addDetection);
	}


	private FilteredVisionFrame constructFilteredVisionFrame(FilteredVisionFrame lastFrame)
	{
		// remove old camera filters
		long avgTimestamp = (long) cams.values().stream().mapToLong(CamFilter::getTimestamp).average().orElse(0);
		cams.values().removeIf(f -> Math.abs(avgTimestamp - f.getTimestamp()) / 1e9 > 0.5);

		long timestamp = cams.values().stream().mapToLong(CamFilter::getTimestamp).max().orElse(lastFrame.getTimestamp());

		// use newest timestamp to prevent negative delta time in filtered frames
		timestamp = Math.max(lastFrame.getTimestamp(), timestamp);

		// merge all camera filters (robots on multiple cams)
		List<FilteredVisionBot> mergedRobots = mergeRobots(cams.values(), timestamp);

		// update robot quality inspector
		robotQualityInspector.prune(timestamp);
		final double averageDt = cams.values().stream().mapToDouble(CamFilter::getAverageFrameDt).max().orElse(0.01);
		robotQualityInspector.updateAverageDt(averageDt);

		// filter merged robots by quality
		List<FilteredVisionBot> filteredRobots = mergedRobots.stream()
				.filter(b -> robotQualityInspector.passesQualityInspection(b.getBotID()))
				.collect(Collectors.toList());

		// check robot quality
		qualityInspector.inspectRobots(cams.values(), timestamp);

		// merge all balls and select primary one
		FilteredVisionBall ball = selectAndMergeBall(cams.values(), timestamp, filteredRobots, lastFrame.getBall());

		// construct filtered vision frame
		FilteredVisionFrame frame = FilteredVisionFrame.builder()
				.withId(lastFrame.getId() + 1)
				.withTimestamp(timestamp)
				.withBall(ball)
				.withBots(filteredRobots)
				.withKickEvent(lastKickEvent)
				.withKickFitState(lastBallFilterOutput.getPreprocessorOutput().getKickFitState().orElse(null))
				.withShapeMap(new ShapeMap())
				.build();

		// forward frame for inspection
		qualityInspector.inspectFilteredVisionFrame(frame);

		// Update active cameras in viewport architect
		viewportArchitect.updateCameras(cams.keySet());

		// add debug and info shapes for visualizer
		frame.getShapeMap().get(EVisionFilterShapesLayer.VIEWPORT_SHAPES).addAll(viewportArchitect.getInfoShapes());
		frame.getShapeMap().get(EVisionFilterShapesLayer.QUALITY_SHAPES).addAll(qualityInspector.getInfoShapes());
		frame.getShapeMap().get(EVisionFilterShapesLayer.CAM_INFO_SHAPES).addAll(getCamInfoShapes());
		frame.getShapeMap().get(EVisionFilterShapesLayer.BALL_TRACKER_SHAPES_IMPORTANT)
				.addAll(ballFilterPreprocessor.getShapes());
		frame.getShapeMap().get(EVisionFilterShapesLayer.ROBOT_TRACKER_SHAPES)
				.addAll(getRobotTrackerShapes(timestamp));
		frame.getShapeMap().get(EVisionFilterShapesLayer.BALL_TRACKER_SHAPES)
				.addAll(getBallTrackerShapes(timestamp));
		frame.getShapeMap().get(EVisionFilterShapesLayer.ROBOT_QUALITY_INSPECTOR)
				.addAll(getRobotQualityInspectorShapes(mergedRobots));
		frame.getShapeMap().get(EVisionFilterShapesLayer.VISION_FRAME)
				.addAll(getVisionFrameShapes(frame));

		return frame;
	}


	private Collection<? extends IDrawableShape> getVisionFrameShapes(FilteredVisionFrame frame)
	{
		List<IDrawableShape> shapes = new ArrayList<>();
		frame.getKickEvent().ifPresent(event -> shapes
				.add(new DrawablePoint(event.getPosition(), Color.red)
						.withSize(50)));

		frame.getKickFitState().ifPresent(state -> shapes
				.add(new DrawableArrow(state.getPos().getXYVector(), state.getVel().getXYVector())
						.setColor(Color.magenta)));
		return shapes;
	}


	private List<FilteredVisionBot> mergeRobots(final Collection<CamFilter> camFilters, final long timestamp)
	{
		// just get all RobotTracker in one list
		List<RobotTracker> allTrackers = camFilters.stream()
				.flatMap(f -> f.getValidRobots().values().stream())
				.collect(Collectors.toList());

		// group trackers by BotID
		Map<BotID, List<RobotTracker>> trackersById = allTrackers.stream()
				.collect(Collectors.groupingBy(RobotTracker::getBotId));

		List<FilteredVisionBot> mergedBots = new ArrayList<>();

		// merge all trackers in each group and get filtered vision bot from it
		for (Entry<BotID, List<RobotTracker>> entry : trackersById.entrySet())
		{
			mergedBots.add(RobotTracker.mergeRobotTrackers(entry.getKey(), entry.getValue(), timestamp));
		}

		return mergedBots;
	}


	private FilteredVisionBall selectAndMergeBall(
			Collection<CamFilter> camFilters,
			long timestamp,
			List<FilteredVisionBot> mergedRobots,
			FilteredVisionBall lastBall)
	{
		List<BallTracker> allTrackers = camFilters.stream()
				.flatMap(f -> f.getBalls().stream())
				.collect(Collectors.toList());

		BallFilterPreprocessorOutput preOutput = ballFilterPreprocessor.update(lastBall, allTrackers,
				mergedRobots, getRobotInfoMap(), timestamp);

		lastKickEvent = preOutput.getKickEvent().orElse(null);

		lastBallFilterOutput = ballFilter.update(preOutput, lastBall, timestamp);

		return lastBallFilterOutput.getFilteredBall();
	}


	@Override
	public void onNewCameraGeometry(final CamGeometry geometry)
	{
		processGeometryFrame(geometry);
	}


	private void processGeometryFrame(final CamGeometry geometry)
	{
		for (CamCalibration c : geometry.getCalibrations().values())
		{
			int camId = c.getCameraId();
			CamFilter camFilter = cams.get(camId);
			if (camFilter != null)
			{
				camFilter.update(c);
			}
		}

		// forward to quality inspector for sanity checks
		qualityInspector.inspectCameraGeometry(geometry);

		// and to camera architect to lay out viewports
		viewportArchitect.newCameraGeometry(geometry);

		for (CamFilter c : cams.values())
		{
			c.update(geometry.getField());
		}
	}


	@Override
	protected void start()
	{
		super.start();

		viewportArchitect.addObserver(this);
		ballFilterPreprocessor.addObserver(this);

		boolean useThreads = getSubnodeConfiguration().getBoolean("useThreads", true);

		if (useThreads)
		{
			publisherExecutor = Executors
					.newSingleThreadScheduledExecutor(new NamedThreadFactory("VisionFilter Publisher"));
			publisherExecutor
					.scheduleAtFixedRate(() -> Safe.run(this::publish), 0, (long) (publishDt * 1e9), TimeUnit.NANOSECONDS);
			log.info("Using threaded VisionFilter");
		}
	}


	@Override
	protected void stop()
	{
		super.stop();
		if (publisherExecutor != null)
		{
			publisherExecutor.shutdown();
			publisherExecutor = null;
		}
		cams.clear();
		viewportArchitect.removeObserver(this);
		ballFilterPreprocessor.removeObserver(this);
		ballFilterPreprocessor.clear();
		lastFrame = FilteredVisionFrame.createEmptyFrame();
	}


	@Override
	public void resetBall(final IVector3 pos, final IVector3 vel)
	{
		ballFilter.resetBall(pos.getXYVector());
		ballFilterPreprocessor.clear();
	}


	@Override
	public void setModelIdentification(final boolean enable)
	{
		ballFilterPreprocessor.setDoModelIdentification(enable);
	}


	@Override
	public void onClearCamFrame()
	{
		super.onClearCamFrame();
		cams.clear();
		ballFilterPreprocessor.clear();
		lastFrame = FilteredVisionFrame.createEmptyFrame();
	}


	@Override
	public void onViewportUpdated(final int cameraId, final IRectangle viewport)
	{
		publishUpdatedViewport(cameraId, viewport);
	}


	@Override
	public void onBallModelIdentificationResult(final IBallModelIdentResult ident)
	{
		publishBallModelIdentification(ident);
	}


	private List<IDrawableShape> getCamInfoShapes()
	{
		return cams.values().stream()
				.flatMap(c -> c.getInfoShapes().stream())
				.collect(Collectors.toList());
	}


	private List<IDrawableShape> getRobotTrackerShapes(final long timestamp)
	{
		return cams.values().stream()
				.flatMap(c -> c.getRobotTrackerShapes(timestamp).stream())
				.collect(Collectors.toList());
	}


	private List<IDrawableShape> getBallTrackerShapes(final long timestamp)
	{
		List<IDrawableShape> shapes = new ArrayList<>();

		for (CamFilter camFilter : cams.values())
		{
			for (BallTracker tracker : camFilter.getBalls())
			{
				IVector2 pos = tracker.getPosition(timestamp);

				DrawableCircle ballPos = new DrawableCircle(pos, 60, Color.WHITE);
				shapes.add(ballPos);

				DrawableAnnotation camId = new DrawableAnnotation(pos, Integer.toString(camFilter.getCamId()), true);
				camId.withOffset(Vector2.fromY(-100));
				camId.setColor(Color.WHITE);
				shapes.add(camId);

				DrawableAnnotation unc = new DrawableAnnotation(pos,
						String.format("%.2f",
								tracker.getFilter().getPositionUncertainty().getLength() * tracker.getUncertainty()));
				unc.withOffset(Vector2.fromX(-80));
				unc.setColor(Color.WHITE);
				shapes.add(unc);

				DrawableAnnotation age = new DrawableAnnotation(pos,
						String.format("%d: %.3fs", camFilter.getCamId(),
								(timestamp - tracker.getLastUpdateTimestamp()) * 1e-9));
				age.withOffset(Vector2.fromXY(120, (camFilter.getCamId() * 45.0) - 100.0));
				age.setColor(Color.GREEN);
				shapes.add(age);
			}

			shapes.addAll(camFilter.getBallInfoShapes());
		}

		shapes.addAll(ballFilter.getShapes());

		return shapes;
	}


	private Collection<IDrawableShape> getRobotQualityInspectorShapes(final List<FilteredVisionBot> bots)
	{
		List<IDrawableShape> shapes = new ArrayList<>();
		for (FilteredVisionBot bot : bots)
		{
			BotID botID = bot.getBotID();
			long maxNumDetections = (long) robotQualityInspector.getPossibleDetections();
			long numDetections = robotQualityInspector.getNumDetections(botID);
			long percentage = Math.round(100.0 * numDetections / maxNumDetections);
			String text = numDetections + "/" + maxNumDetections + "=" + percentage + "%";
			shapes.add(new DrawableAnnotation(bot.getPos(), text)
					.withOffset(Vector2f.fromY(200))
					.withCenterHorizontally(true));
		}
		return shapes;
	}
}
