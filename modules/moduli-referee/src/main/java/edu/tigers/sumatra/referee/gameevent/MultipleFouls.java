/*
 * Copyright (c) 2009 - 2020, DHBW Mannheim - TIGERs Mannheim
 */

package edu.tigers.sumatra.referee.gameevent;

import com.sleepycat.persist.model.Persistent;
import edu.tigers.sumatra.ids.ETeamColor;
import edu.tigers.sumatra.referee.proto.SslGcGameEvent;
import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;


@Persistent
public class MultipleFouls extends AGameEvent
{
	private final ETeamColor team;


	@SuppressWarnings("unsued") // used by berkeley
	protected MultipleFouls()
	{
		team = null;
	}


	/**
	 * Default conversion constructor. Note: Called by reflection!
	 *
	 * @param event a protobuf event
	 */
	public MultipleFouls(SslGcGameEvent.GameEvent event)
	{
		super(event);
		team = toTeamColor(event.getMultipleFouls().getByTeam());
	}


	public MultipleFouls(final ETeamColor team)
	{
		super(EGameEvent.MULTIPLE_FOULS);
		this.team = team;
	}


	@Override
	public SslGcGameEvent.GameEvent toProtobuf()
	{
		SslGcGameEvent.GameEvent.Builder builder = SslGcGameEvent.GameEvent.newBuilder();
		builder.setType(SslGcGameEvent.GameEvent.Type.MULTIPLE_FOULS);
		builder.getMultipleFoulsBuilder()
				.setByTeam(getTeam(team));

		return builder.build();
	}


	public ETeamColor getTeam()
	{
		return team;
	}


	@Override
	public String getDescription()
	{
		return "Multiple fouls collected by " + team;
	}


	@Override
	public boolean equals(final Object o)
	{
		if (this == o)
			return true;

		if (o == null || getClass() != o.getClass())
			return false;

		final MultipleFouls that = (MultipleFouls) o;

		return new EqualsBuilder()
				.appendSuper(super.equals(o))
				.append(team, that.team)
				.isEquals();
	}


	@Override
	public int hashCode()
	{
		return new HashCodeBuilder(17, 37)
				.appendSuper(super.hashCode())
				.append(team)
				.toHashCode();
	}
}
