/*
 * Copyright (c) 2009 - 2018, DHBW Mannheim - TIGERs Mannheim
 */

package edu.tigers.sumatra.ids;


import lombok.AllArgsConstructor;
import lombok.Getter;


/**
 * All AI teams.
 */
@AllArgsConstructor
@Getter
public enum EAiTeam
{
	BLUE(ETeamColor.BLUE),
	YELLOW(ETeamColor.YELLOW),

	;

	private ETeamColor teamColor;


	/**
	 * Get the primary team of the given team color
	 *
	 * @param teamColor
	 * @return
	 */
	public static EAiTeam primary(ETeamColor teamColor)
	{
		if (teamColor == ETeamColor.BLUE)
		{
			return BLUE;
		} else if (teamColor == ETeamColor.YELLOW)
		{
			return YELLOW;
		}
		throw new IllegalArgumentException("Can not map team color: " + teamColor);
	}


	/**
	 * Check if color of aiteam matches color
	 *
	 * @param color
	 * @return
	 */
	public boolean matchesColor(ETeamColor color)
	{
		return color == teamColor;
	}
}
