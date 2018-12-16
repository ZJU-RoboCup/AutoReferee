/*
 * Copyright (c) 2009 - 2018, DHBW Mannheim - TIGERs Mannheim
 */

package edu.tigers.autoreferee.engine.events.data;

import edu.tigers.autoreferee.engine.events.EGameEvent;
import edu.tigers.sumatra.gamecontroller.SslGameEvent2019;
import edu.tigers.sumatra.math.vector.IVector2;


public class NoProgressInGame extends AGameEvent
{
	private final double time;
	private final IVector2 location;
	
	
	/**
	 * @param pos
	 * @param time [s]
	 */
	public NoProgressInGame(IVector2 pos, double time)
	{
		this.location = pos;
		this.time = time;
	}
	
	
	@Override
	public EGameEvent getType()
	{
		return EGameEvent.NO_PROGRESS_IN_GAME;
	}
	
	
	@Override
	public SslGameEvent2019.GameEvent toProtobuf()
	{
		SslGameEvent2019.GameEvent.Builder eventBuilder = SslGameEvent2019.GameEvent.newBuilder();
		eventBuilder.getNoProgressInGameBuilder().setTime((float) time).setLocation(getLocationFromVector(location));
		return eventBuilder.build();
	}
	
	
	@Override
	public String toString()
	{
		return String.format("No progress in Game for %.2f s @ %s", time, formatVector(location));
	}
}
