/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package io.donbee.jade.imtp.leap.nio;

//#J2ME_EXCLUDE_FILE

import io.donbee.jade.imtp.leap.JICP.Connection;
import io.donbee.jade.imtp.leap.JICP.ConnectionFactory;
import io.donbee.jade.mtp.TransportAddress;
import java.io.IOException;
import java.net.Socket;

/**
 * This class provides a {@link ConnectionFactory} that will construct {@link NIOJICPSConnection NIOJICPSConnections}.
 * Before the NIOJICPSConnections can be used {@link NIOJICPSConnection#init(java.nio.channels.SelectionKey)} must be called.
 * @author eduard
 */
public class NIOJICPSPeer extends NIOJICPPeer {

    public ConnectionFactory getConnectionFactory() {
        return new ConnectionFactory() {

            public Connection createConnection(Socket s) {
                return new NIOJICPSConnection();
            }

            public Connection createConnection(TransportAddress ta) throws IOException {
                return new NIOJICPSConnection();
            }
        };
    }



}
