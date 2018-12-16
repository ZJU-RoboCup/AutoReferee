/*
 * Copyright (c) 2009 - 2018, DHBW Mannheim - TIGERs Mannheim
 */
package edu.tigers.autoreferee.engine.log;

import java.text.DecimalFormat;

import edu.tigers.autoreferee.engine.RefboxRemoteCommand;
import edu.tigers.sumatra.referee.data.RefereeMsg;


/**
 * @author "Lukas Magel"
 */
public class GameLogFormatter
{
	private static final DecimalFormat posFormat = new DecimalFormat("###0.00");
	
	
	private GameLogFormatter()
	{
	}
	
	
	/**
	 * @param cmd
	 * @return
	 */
	public static String formatCommand(final RefboxRemoteCommand cmd)
	{
		StringBuilder builder = new StringBuilder();
		switch (cmd.getType())
		{
			case CARD:
				builder.append(cmd.getCardType());
				builder.append(" | ");
				builder.append(cmd.getCardTeam());
				break;
			case COMMAND:
				builder.append(cmd.getCommand());
				cmd.getKickPos().ifPresent(pos -> {
					builder.append(" @Pos: ");
					builder.append(posFormat.format(pos.x()));
					builder.append(" | ");
					builder.append(posFormat.format(pos.y()));
				});
				break;
			case GAME_EVENT_ONLY:
				builder.append("reset game event");
				break;
			default:
				throw new IllegalArgumentException("Can not format command type: " + cmd.getType());
		}
		return builder.toString();
	}
	
	
	/**
	 * @param msg
	 * @return
	 */
	public static String formatRefMsg(final RefereeMsg msg)
	{
		return msg.getCommandCounter() + " " + msg.getCommand();
	}
}
