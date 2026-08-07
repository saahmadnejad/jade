package io.donbee.jade.core.faultRecovery;

import io.donbee.jade.core.ServiceException;
import io.donbee.jade.core.ServiceHelper;

public interface FaultRecoveryHelper extends ServiceHelper {
	public static final String SERVICE_NAME = "io.donbee.jade.core.faultRecovery.FaultRecovery";
	
	void reattach() throws ServiceException;

}
