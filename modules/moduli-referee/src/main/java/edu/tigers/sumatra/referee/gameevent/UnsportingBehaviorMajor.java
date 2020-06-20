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
public class UnsportingBehaviorMajor extends AGameEvent
{
	private final ETeamColor team;
	private final String reason;


	@SuppressWarnings("unsued") // used by berkeley
	protected UnsportingBehaviorMajor()
	{
		reason = null;
		team = null;
	}

	/**
	 * Default conversion constructor. Note: Called by reflection!
	 *
	 * @param event a protobuf event
	 */
	public UnsportingBehaviorMajor(SslGcGameEvent.GameEvent event)
	{
		super(event);
		team = toTeamColor(event.getUnsportingBehaviorMajor().getByTeam());
		reason = event.getUnsportingBehaviorMajor().getReason();
	}


	public UnsportingBehaviorMajor(final ETeamColor team, final String reason)
	{
		super(EGameEvent.UNSPORTING_BEHAVIOR_MAJOR);
		this.team = team;
		this.reason = reason;
	}


	@Override
	public SslGcGameEvent.GameEvent toProtobuf()
	{
		SslGcGameEvent.GameEvent.Builder builder = SslGcGameEvent.GameEvent.newBuilder();
		builder.setType(SslGcGameEvent.GameEvent.Type.UNSPORTING_BEHAVIOR_MAJOR);
		builder.getUnsportingBehaviorMajorBuilder()
				.setByTeam(getTeam(team))
				.setReason(reason);

		return builder.build();
	}


	public ETeamColor getTeam()
	{
		return team;
	}


	public String getReason()
	{
		return reason;
	}


	@Override
	public String getDescription()
	{
		return "Major unsporting behavior by " + team + ": " + reason;
	}


	@Override
	public boolean equals(final Object o)
	{
		if (this == o)
			return true;

		if (o == null || getClass() != o.getClass())
			return false;

		final UnsportingBehaviorMajor that = (UnsportingBehaviorMajor) o;

		return new EqualsBuilder()
				.appendSuper(super.equals(o))
				.append(team, that.team)
				.append(reason, that.reason)
				.isEquals();
	}


	@Override
	public int hashCode()
	{
		return new HashCodeBuilder(17, 37)
				.appendSuper(super.hashCode())
				.append(team)
				.append(reason)
				.toHashCode();
	}
}
