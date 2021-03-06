package com.neuronrobotics.sdk.commands.neuronrobotics.dyio;

import com.neuronrobotics.sdk.common.BowlerAbstractCommand;
import com.neuronrobotics.sdk.common.BowlerMethod;

// TODO: Auto-generated Javadoc
/**
 * The Class PowerCommand.
 */
public class PowerCommand extends BowlerAbstractCommand {
	
	/**
	 * Instantiates a new power command.
	 */
	public PowerCommand() {
		setOpCode("_pwr");
		setMethod(BowlerMethod.GET);
	}
	
	/**
	 * This method will disable the brownout detect for the DyIO.
	 *
	 * @param disableBrownOutDetect the disable brown out detect
	 */
	public PowerCommand(boolean disableBrownOutDetect) {
		setOpCode("_pwr");
		setMethod(BowlerMethod.CRITICAL);
		getCallingDataStorage().add(disableBrownOutDetect?1:0);
	}
}
