/*
 * Copyright (c) 2009 - 2020, DHBW Mannheim - TIGERs Mannheim
 */

package edu.tigers.sumatra.wp.data;

import com.sleepycat.persist.model.Persistent;
import edu.tigers.sumatra.geometry.Geometry;
import edu.tigers.sumatra.ids.AObjectID;
import edu.tigers.sumatra.ids.BallID;
import edu.tigers.sumatra.math.vector.IVector2;
import edu.tigers.sumatra.math.vector.IVector3;
import edu.tigers.sumatra.vision.data.BallTrajectoryState;
import edu.tigers.sumatra.vision.data.FilteredVisionBall;
import edu.tigers.sumatra.wp.ball.trajectory.BallFactory;
import edu.tigers.sumatra.wp.ball.trajectory.IBallTrajectory;
import edu.tigers.sumatra.wp.ball.trajectory.IChipBallConsultant;
import edu.tigers.sumatra.wp.ball.trajectory.IStraightBallConsultant;
import org.apache.commons.lang.Validate;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static edu.tigers.sumatra.vision.data.BallTrajectoryState.builder;


/**
 * A tracked (filtered, predicted) ball.
 */
@Persistent
public class TrackedBall implements ITrackedBall
{
	private final long timestamp;
	private final BallTrajectoryState state;
	private final long lastVisibleTimestamp;

	// cache some fields on demand to increase performance
	private transient IVector2 pos;
	private transient IVector2 vel;
	private transient IBallTrajectory ballTrajectory;
	private transient IStraightBallConsultant straightBallConsultant;
	private transient IChipBallConsultant chipBallConsultant;


	/**
	 * Create an empty tracked ball.
	 */
	private TrackedBall()
	{
		state = new BallTrajectoryState();
		lastVisibleTimestamp = 0;
		timestamp = 0;
	}


	/**
	 * @param timestamp
	 * @param state
	 * @param lastVisibleTimestamp
	 */
	private TrackedBall(final long timestamp, final BallTrajectoryState state, final long lastVisibleTimestamp)
	{
		this.timestamp = timestamp;
		this.state = state;
		this.lastVisibleTimestamp = lastVisibleTimestamp;

		Validate.isTrue(Double.isFinite(state.getVSwitchToRoll()) && (state.getVSwitchToRoll() >= 0));
	}


	/**
	 * @return an empty tracked ball
	 */
	public static TrackedBall createStub()
	{
		return new TrackedBall();
	}


	/**
	 * Create a tracked ball from a BallTrajectoryState.<br>
	 * The last visible timestamp is set to <code>timestamp</code> and therefore always visible.
	 *
	 * @param timestamp [ns]
	 * @param state     State in milli units.
	 * @return
	 */
	public static TrackedBall fromTrajectoryStateVisible(final long timestamp, final BallTrajectoryState state)
	{
		return new TrackedBall(timestamp, state, timestamp);
	}


	/**
	 * Create a tracked ball from a FilteredVisionBall.
	 *
	 * @param timestamp
	 * @param ball
	 * @return
	 */
	public static TrackedBall fromFilteredVisionBall(final long timestamp, final FilteredVisionBall ball)
	{
		BallTrajectoryState state = builder()
				.withPos(ball.getPos())
				.withVel(ball.getVel())
				.withAcc(ball.getAcc())
				.withVSwitchToRoll(ball.getVSwitch())
				.withChipped(ball.isChipped())
				.withSpin(ball.getSpin())
				.build();
		return new TrackedBall(timestamp, state, ball.getLastVisibleTimestamp());
	}


	@Override
	public long getTimestamp()
	{
		return timestamp;
	}


	@Override
	public IBallTrajectory getTrajectory()
	{
		if (ballTrajectory == null)
		{
			ballTrajectory = BallFactory.createTrajectory(state);
		}
		return ballTrajectory;
	}


	@Override
	public TrackedBall mirrored()
	{
		return new TrackedBall(timestamp, state.mirrored(), lastVisibleTimestamp);
	}


	@Override
	public AObjectID getId()
	{
		return BallID.instance();
	}


	@Override
	public double getRpm()
	{
		// v = r × RPM × 0.10472
		// RPM = v / (r x 0.10472)
		return getVel().getLength2() / ((Geometry.getBallRadius() / 1000.0) * 0.10472);
	}


	@Override
	public IVector2 getPos()
	{
		if (pos == null)
		{
			pos = getPos3().getXYVector();
		}
		return pos;
	}


	@Override
	public IVector2 getVel()
	{
		if (vel == null)
		{
			vel = getVel3().getXYVector();
		}
		return vel;
	}


	@Override
	public IVector2 getAcc()
	{
		return getAcc3().getXYVector();
	}


	@Override
	public IVector3 getPos3()
	{
		return state.getPos().getXYZVector();
	}


	@Override
	public IVector3 getVel3()
	{
		return state.getVel().multiplyNew(0.001).getXYZVector();
	}


	@Override
	public IVector3 getAcc3()
	{
		return state.getAcc().multiplyNew(0.001).getXYZVector();
	}


	@Override
	public double invisibleFor()
	{
		return (getTimestamp() - lastVisibleTimestamp) * 1e-9;
	}


	@Override
	public boolean isOnCam(final double seconds)
	{
		return invisibleFor() <= seconds;
	}


	@Override
	public double getHeight()
	{
		return getPos3().z();
	}


	@Override
	public IStraightBallConsultant getStraightConsultant()
	{
		if (straightBallConsultant == null)
		{
			straightBallConsultant = BallFactory.createStraightConsultant();
		}
		return straightBallConsultant;
	}


	@Override
	public IChipBallConsultant getChipConsultant()
	{
		if (chipBallConsultant == null)
		{
			chipBallConsultant = BallFactory.createChipConsultant();
		}
		return chipBallConsultant;
	}


	@Override
	public long getLastVisibleTimestamp()
	{
		return lastVisibleTimestamp;
	}


	@Override
	public boolean isChipped()
	{
		return state.isChipped();
	}


	@Override
	public BallTrajectoryState getState()
	{
		return state;
	}


	@Override
	public double getQuality()
	{
		final double invisibleTime = (timestamp - lastVisibleTimestamp) / 1e9;
		return 1 - Math.min(1, invisibleTime / 0.2);
	}


	@Override
	public List<Number> getNumberList()
	{
		List<Number> numbers = new ArrayList<>();
		numbers.add(timestamp);
		numbers.addAll(getPos3().getNumberList());
		numbers.addAll(getVel3().getNumberList());
		numbers.addAll(getAcc3().getNumberList());
		numbers.add(lastVisibleTimestamp);
		numbers.add(state.getVSwitchToRoll());
		numbers.add(state.isChipped() ? 1 : 0);
		return numbers;
	}


	@Override
	public List<String> getHeaders()
	{
		return Arrays.asList("timestamp", "pos_x", "pos_y", "pos_z", "vel_x", "vel_y", "vel_z", "acc_x", "acc_y", "acc_z",
				"lastVisibleTimestamp", "vSwitchToRoll", "chipped");
	}


	@Override
	public String toString()
	{
		return new ToStringBuilder(this, ToStringStyle.SHORT_PREFIX_STYLE)
				.append("pos", getPos3())
				.append("vel", getVel3())
				.toString();
	}
}
