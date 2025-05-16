package ir.donbee.jade.imtp.leap.JICP;

//#MIDP_EXCLUDE_FILE

import ir.donbee.jade.imtp.leap.TransportProtocol;

/**
   Classes implementing this interface must provide methods to retrieve
   a Protocol object and a ConnectionFactory object. 
   @author Giovanni Caire - TILAB
 */
public interface ProtocolManager {
  TransportProtocol getProtocol();
  ConnectionFactory getConnectionFactory();
}  
