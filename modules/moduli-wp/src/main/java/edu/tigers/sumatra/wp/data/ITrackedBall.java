/*
 * Copyright (c) 2009 - 2020, DHBW Mannheim - TIGERs Mannheim
 */

package edu.tigers.sumatra.wp.data;

import edu.tigers.sumatra.data.collector.IExportable;
import edu.tigers.sumatra.math.vector.IVector3;
import edu.tigers.sumatra.vision.data.BallTrajectoryState;
import edu.tigers.sumatra.wp.ball.trajectory.IBallTrajectory;
import edu.tigers.sumatra.wp.ball.trajectory.IChipBallConsultant;
import edu.tigers.sumatra.wp.ball.trajectory.IStraightBallConsultant;


/**
 *
 */
public interface ITrackedBall extends ITrackedObject, IExportable
{
	@Override
	ITrackedBall mirrored();


	/**
	 * Get theoretical RPM based on ball velocity
	 * <a href="http://www.endmemo.com/physics/rpmlinear.php">http://www.endmemo.com/physics/rpmlinear.php</a>
	 *
	 * @return
	 */
	double getRpm();


	/**
	 * @return [mm, mm, mm]
	 */
	IVector3 getPos3();


	/**
	 * @return [m/s, m/s, rad/s]
	 */
	IVector3 getVel3();


	/**
	 * @return [m/s², m/s², rad/s²]
	 */
	IVector3 getAcc3();


	/**
	 * @return the time [s] that the ball is invisible (0 means it is visible)
	 */
	double invisibleFor();


	/**
	 * Check if the ball was visible within the given seconds
	 *
	 * @param seconds within this time horizon
	 * @return true, if the ball is detected by any camera
	 */
	boolean isOnCam(double seconds);


	/**
	 * @return the height of the ball
	 */
	double getHeight();


	/**
	 * Get timestamp when this ball was last seen on a camera.
	 *
	 * @return
	 */
	long getLastVisibleTimestamp();


	/**
	 * Is this a chipped ball?<br/>
	 * This does not necessarily mean it is currently in the air!
	 *
	 * @return
	 */
	boolean isChipped();


	/**
	 * @return the current ball state
	 */
	BallTrajectoryState getState();


	/**
	 * @return the trajectory of the ball that returns the ball state at any time
	 */
	IBallTrajectory getTrajectory();


	/**
	 * @return the consultant for straight balls
	 */
	IStraightBallConsultant getStraightConsultant();


	/**
	 * @return the consulatant for chip balls
	 */
	IChipBallConsultant getChipConsultant();
}
