/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package ir.donbee.jade.imtp.leap.JICP;

//#J2ME_EXCLUDE_FILE

import ir.donbee.jade.mtp.TransportAddress;
import java.io.IOException;

/**
 * class for setting up a secure FrontEndDispatcher
 * @author eduard
 */
public class FrontEndSDispatcher extends FrontEndDispatcher {

    /**
     *
     * @param ta
     * @return a {@link JICPSConnection secure JICPConnection}
     * @throws IOException
     */
    protected JICPConnection getConnection(TransportAddress ta) throws IOException {
        return new JICPSConnection(ta, false, (int) connectionTimeout, bindHost, bindPort);
    }

}
