package ir.donbee.jade.core.faultRecovery;

import ir.donbee.jade.core.ServiceException;
import ir.donbee.jade.core.ServiceHelper;

public interface FaultRecoveryHelper extends ServiceHelper {
	public static final String SERVICE_NAME = "ir.donbee.jade.core.faultRecovery.FaultRecovery";
	
	void reattach() throws ServiceException;

}
