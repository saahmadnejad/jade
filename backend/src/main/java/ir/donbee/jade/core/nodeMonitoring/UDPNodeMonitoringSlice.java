package ir.donbee.jade.core.nodeMonitoring;

//#J2ME_EXCLUDE_FILE
//#APIDOC_EXCLUDE_FILE

import ir.donbee.jade.core.Service;
import ir.donbee.jade.core.IMTPException;
import ir.donbee.jade.core.ServiceException;

public interface UDPNodeMonitoringSlice extends Service.Slice {
	static final String H_ACTIVATEUDP = "H-ACTIVATEUDP";
	static final String H_DEACTIVATEUDP = "H-DEACTIVATEUDP";
	
	/*
	 * Request a given node to start sending UDP packets
	 */
	void activateUDP(String label, String host, int port, int pingDelay, long key) throws IMTPException, ServiceException;
	
	/*
	 * Request a given node to stop sending UDP packets
	 */
	void deactivateUDP(String label, long key) throws IMTPException;
}
