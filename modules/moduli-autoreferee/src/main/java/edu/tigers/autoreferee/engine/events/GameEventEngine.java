/*
 * Copyright (c) 2009 - 2018, DHBW Mannheim - TIGERs Mannheim
 */
package edu.tigers.autoreferee.engine.events;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.log4j.Logger;

import com.github.g3force.instanceables.InstanceableClass;

import edu.tigers.autoreferee.IAutoRefFrame;
import edu.tigers.sumatra.referee.data.GameState;


/**
 * The engine consults the {@link IGameEventDetector}s.
 */
public class GameEventEngine
{
	private static final Logger log = Logger.getLogger(GameEventEngine.class.getName());
	private final List<IGameEventDetector> allDetectors = new ArrayList<>();
	private Set<EGameEventDetectorType> activeDetectors;
	
	
	public GameEventEngine()
	{
		for (EGameEventDetectorType eCalc : EGameEventDetectorType.values())
		{
			if (eCalc.getInstanceableClass().getImpl() != null)
			{
				try
				{
					IGameEventDetector inst = (IGameEventDetector) eCalc.getInstanceableClass().newDefaultInstance();
					allDetectors.add(inst);
				} catch (InstanceableClass.NotCreateableException e)
				{
					log.error("Could not instantiate calculator: " + eCalc, e);
				}
			}
		}
		activeDetectors = new HashSet<>(Arrays.asList(EGameEventDetectorType.values()));
	}
	
	
	/**
	 * @param detectorTypes
	 */
	public void setActiveDetectors(final Set<EGameEventDetectorType> detectorTypes)
	{
		this.activeDetectors = detectorTypes;
	}
	
	
	/**
	 * reset
	 */
	public void reset()
	{
		allDetectors.forEach(IGameEventDetector::reset);
	}
	
	
	/**
	 * @param frame
	 * @return
	 */
	public List<IGameEvent> update(final IAutoRefFrame frame)
	{
		GameState currentState = frame.getGameState();
		GameState lastState = frame.getPreviousFrame().getGameState();
		
		/*
		 * Retrieve all rules which are active in the current gamestate
		 */
		List<IGameEventDetector> detectors = this.allDetectors.stream()
				.filter(d -> activeDetectors.contains(d.getType()))
				.filter(d -> d.isActiveIn(currentState.getState()))
				.collect(Collectors.toList());
		
		/*
		 * Reset the detectors which have now become active
		 */
		detectors.stream()
				.filter(detector -> !detector.isActiveIn(lastState.getState()))
				.forEach(IGameEventDetector::reset);
		
		List<IGameEvent> gameEvents = new ArrayList<>();
		for (IGameEventDetector detector : detectors)
		{
			Optional<IGameEvent> result = detector.update(frame);
			result.ifPresent(gameEvents::add);
		}
		
		return gameEvents;
	}
}
