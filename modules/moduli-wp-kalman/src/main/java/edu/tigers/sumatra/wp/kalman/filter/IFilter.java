package edu.tigers.sumatra.wp.kalman.filter;

import edu.tigers.sumatra.wp.kalman.data.AMotionResult;
import edu.tigers.sumatra.wp.kalman.data.AWPCamObject;
import edu.tigers.sumatra.wp.kalman.data.IControl;
import edu.tigers.sumatra.wp.kalman.data.PredictionContext;
import edu.tigers.sumatra.wp.kalman.motionModels.IMotionModel;


/**
 *
 */
public interface IFilter
{
	/**
	 * @param motionModel
	 * @param context
	 * @param firstTimestamp
	 * @param firstObservation
	 */
	void init(IMotionModel motionModel, PredictionContext context, final long firstTimestamp,
			AWPCamObject firstObservation);
			
			
	/**
	 * @return
	 */
	long getTimestamp();
	
	
	/**
	 * @param timestamp
	 * @return
	 */
	AMotionResult getPrediction(final long timestamp);
	
	
	/**
	 * @param timestamp
	 * @param observation
	 */
	void observation(long timestamp, AWPCamObject observation);
	
	
	/**
	 * @param control
	 */
	void setControl(IControl control);
	
	
	/**
	 * @return
	 */
	int getId();
}
