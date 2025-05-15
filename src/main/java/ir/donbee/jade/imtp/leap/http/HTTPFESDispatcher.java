package ir.donbee.jade.imtp.leap.http;

//#J2ME_EXCLUDE_FILE

import ir.donbee.jade.imtp.leap.JICP.Connection;
import ir.donbee.jade.mtp.TransportAddress;

/**
 *
 * @author Eduard Drenth: Logica, 30-sep-2009
 * 
 */
public class HTTPFESDispatcher extends HTTPFEDispatcher {

    protected Connection getConnection(TransportAddress ta) {
        return new HTTPSClientConnection(ta);
    }


}
