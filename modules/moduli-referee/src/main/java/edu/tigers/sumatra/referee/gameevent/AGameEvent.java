/*
 * Copyright (c) 2009 - 2020, DHBW Mannheim - TIGERs Mannheim
 */

package edu.tigers.sumatra.referee.gameevent;


import com.sleepycat.persist.model.Persistent;
import edu.tigers.sumatra.ids.BotID;
import edu.tigers.sumatra.ids.ETeamColor;
import edu.tigers.sumatra.math.vector.IVector2;
import edu.tigers.sumatra.math.vector.Vector2;
import edu.tigers.sumatra.referee.proto.SslGcCommon;
import edu.tigers.sumatra.referee.proto.SslGcGameEvent;
import edu.tigers.sumatra.referee.proto.SslGcGeometry;
import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;


@Persistent
public abstract class AGameEvent implements IGameEvent
{
	EGameEvent type;


	@SuppressWarnings("unsued") // used by berkeley
	protected AGameEvent()
	{
		type = null;
	}


	protected AGameEvent(final EGameEvent type)
	{
		this.type = type;
	}


	protected AGameEvent(final SslGcGameEvent.GameEvent gameEvent)
	{
		this.type = EGameEvent.fromProto(gameEvent.getType());
	}


	@Override
	public EGameEvent getType()
	{
		return type;
	}


	protected SslGcCommon.Team getTeam(ETeamColor color)
	{
		if (color == ETeamColor.YELLOW)
		{
			return SslGcCommon.Team.YELLOW;
		} else if (color == ETeamColor.BLUE)
		{
			return SslGcCommon.Team.BLUE;
		} else
		{
			throw new IllegalArgumentException("Team should be either yellow or blue: " + color);
		}
	}


	protected SslGcCommon.Team getTeam(BotID id)
	{
		return getTeam(id.getTeamColor());
	}


	protected SslGcGeometry.Vector2 getLocationFromVector(IVector2 location)
	{
		return SslGcGeometry.Vector2.newBuilder().setX((float) (location.x() / 1000.f))
				.setY((float) location.y() / 1000.f)
				.build();
	}


	protected String formatVector(IVector2 vec)
	{
		return String.format("(%.3f | %.3f)", vec.x(), vec.y());
	}


	protected static IVector2 toVector(SslGcGeometry.Vector2 location)
	{
		if (location == null)
		{
			return null;
		}
		return Vector2.fromXY(location.getX() * 1000, location.getY() * 1000);
	}


	protected static double toDistance(double distance)
	{
		return distance * 1000;
	}


	protected static ETeamColor toTeamColor(SslGcCommon.Team team)
	{
		switch (team)
		{
			case YELLOW:
				return ETeamColor.YELLOW;
			case BLUE:
				return ETeamColor.BLUE;
			case UNKNOWN:
			default:
				return ETeamColor.NEUTRAL;
		}
	}


	@Override
	public boolean equals(final Object o)
	{
		if (this == o)
			return true;

		if (o == null || getClass() != o.getClass())
			return false;

		final AGameEvent that = (AGameEvent) o;

		return new EqualsBuilder()
				.append(type, that.type)
				.isEquals();
	}


	@Override
	public int hashCode()
	{
		return new HashCodeBuilder(17, 37)
				.append(type)
				.toHashCode();
	}
}
