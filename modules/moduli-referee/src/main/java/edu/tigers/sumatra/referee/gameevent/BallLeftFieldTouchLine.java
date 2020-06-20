/*
 * Copyright (c) 2009 - 2020, DHBW Mannheim - TIGERs Mannheim
 */

package edu.tigers.sumatra.referee.gameevent;

import com.sleepycat.persist.model.Persistent;
import edu.tigers.sumatra.ids.BotID;
import edu.tigers.sumatra.ids.ETeamColor;
import edu.tigers.sumatra.math.vector.IVector2;
import edu.tigers.sumatra.referee.proto.SslGcGameEvent;
import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;


@Persistent
public class BallLeftFieldTouchLine extends AGameEvent
{
	private final ETeamColor team;
	private final Integer bot;
	private final IVector2 location;


	@SuppressWarnings("unsued") // used by berkeley
	protected BallLeftFieldTouchLine()
	{
		team = null;
		bot = 0;
		location = null;
	}


	/**
	 * Default conversion constructor. Note: Called by reflection!
	 *
	 * @param event a protobuf event
	 */
	public BallLeftFieldTouchLine(SslGcGameEvent.GameEvent event)
	{
		super(event);
		this.team = toTeamColor(event.getBallLeftFieldTouchLine().getByTeam());
		this.bot = event.getBallLeftFieldTouchLine().getByBot();
		this.location = toVector(event.getBallLeftFieldTouchLine().getLocation());
	}


	public BallLeftFieldTouchLine(BotID bot, IVector2 location)
	{
		super(EGameEvent.BALL_LEFT_FIELD_TOUCH_LINE);
		this.team = bot.getTeamColor();
		this.bot = bot.getNumber();
		this.location = location;
	}


	public BallLeftFieldTouchLine(ETeamColor team, IVector2 location)
	{
		super(EGameEvent.BALL_LEFT_FIELD_TOUCH_LINE);
		this.team = team;
		this.bot = null;
		this.location = location;
	}


	@Override
	public SslGcGameEvent.GameEvent toProtobuf()
	{
		SslGcGameEvent.GameEvent.Builder builder = SslGcGameEvent.GameEvent.newBuilder();

		builder.setType(SslGcGameEvent.GameEvent.Type.BALL_LEFT_FIELD_TOUCH_LINE);
		builder.getBallLeftFieldTouchLineBuilder()
				.setByTeam(getTeam(team))
				.setLocation(getLocationFromVector(location));

		if (bot != null)
		{
			builder.getBallLeftFieldTouchLineBuilder().setByBot(bot);
		}

		return builder.build();
	}


	@Override
	public String getDescription()
	{
		return String.format("Ball left field @ %s by bot %d %s via touch line", formatVector(location), bot, team);
	}


	@Override
	public boolean equals(final Object o)
	{
		if (this == o)
			return true;

		if (o == null || getClass() != o.getClass())
			return false;

		final BallLeftFieldTouchLine that = (BallLeftFieldTouchLine) o;

		return new EqualsBuilder()
				.appendSuper(super.equals(o))
				.append(bot, that.bot)
				.append(team, that.team)
				.append(location, that.location)
				.isEquals();
	}


	@Override
	public int hashCode()
	{
		return new HashCodeBuilder(17, 37)
				.appendSuper(super.hashCode())
				.append(team)
				.append(bot)
				.append(location)
				.toHashCode();
	}
}
