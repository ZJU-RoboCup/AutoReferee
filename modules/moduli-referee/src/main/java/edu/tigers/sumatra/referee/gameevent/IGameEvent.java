/*
 * Copyright (c) 2009 - 2020, DHBW Mannheim - TIGERs Mannheim
 */
package edu.tigers.sumatra.referee.gameevent;


import edu.tigers.sumatra.referee.proto.SslGcGameEvent;


/**
 * This interface is implemented by all GameEvent types in {@link EGameEvent}
 * The implementations are located in edu.tigers.sumatra.referee.events.data
 * All Events are read only after creation
 */
public interface IGameEvent
{
	/**
	 * @return The type of the stored game Event
	 */
	EGameEvent getType();


	/**
	 * Receive a message ready for transmitting
	 *
	 * @return The internal values of the gameEvent compiled to a Protobuf message
	 */
	SslGcGameEvent.GameEvent toProtobuf();


	/**
	 * @return a formatted string describing the event in human language (used in the game log)
	 */
	String toString();


	String getDescription();
}
