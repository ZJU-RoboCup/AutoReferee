/*
 * Copyright (c) 2009 - 2018, DHBW Mannheim - TIGERs Mannheim
 */

package edu.tigers.autoreferee.engine.events.impl;

import java.util.EnumSet;
import java.util.Optional;

import edu.tigers.autoreferee.engine.events.EGameEventDetectorType;
import edu.tigers.autoreferee.engine.events.IGameEvent;
import edu.tigers.autoreferee.engine.events.data.NoProgressInGame;
import edu.tigers.sumatra.math.vector.IVector2;
import edu.tigers.sumatra.referee.data.EGameState;


/**
 * Check if there is no progress in a running game.
 */
public class NoProgressDetector extends AGameEventDetector
{
	private static final double DISTANCE_TOLERANCE = 100;
	private static final double TIMEOUT = 10.0;
	
	private long lastTime;
	private boolean raised;
	private IVector2 lastBallPos = null;
	
	
	public NoProgressDetector()
	{
		super(EGameEventDetectorType.NO_PROGRESS, EnumSet.of(EGameState.RUNNING));
	}
	
	
	@Override
	protected void doPrepare()
	{
		lastTime = 0;
		raised = false;
		lastBallPos = frame.getWorldFrame().getBall().getPos();
	}
	
	
	@Override
	protected Optional<IGameEvent> doUpdate()
	{
		IVector2 ballPos = frame.getWorldFrame().getBall().getPos();
		
		if (frame.getWorldFrame().getBall().isOnCam() && ballPos.distanceTo(lastBallPos) > DISTANCE_TOLERANCE)
		{
			lastTime = 0;
			lastBallPos = ballPos;
			raised = false;
			return Optional.empty();
		}
		if (lastTime == 0)
		{
			lastTime = frame.getTimestamp();
		}
		
		if (!raised && (frame.getTimestamp() - lastTime) / 1e9 > TIMEOUT)
		{
			IGameEvent violation = new NoProgressInGame(ballPos, (frame.getTimestamp() - lastTime) / 1e9);
			lastTime = 0;
			raised = true;
			return Optional.of(violation);
		}
		
		return Optional.empty();
	}
}
