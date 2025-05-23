package ir.donbee.jade.imtp.leap.nio;

//#J2ME_EXCLUDE_FILE

import ir.donbee.jade.imtp.leap.JICP.Connection;
import ir.donbee.jade.imtp.leap.JICP.ConnectionFactory;
import ir.donbee.jade.imtp.leap.TransportProtocol;
import ir.donbee.jade.imtp.leap.http.HTTPSProtocol;
import ir.donbee.jade.mtp.TransportAddress;
import java.io.IOException;
import java.net.Socket;

/**
 *
 * @author Eduard Drenth: Logica, 12-jul-2009
 * 
 */
public class NIOHTTPSPeer  extends NIOHTTPPeer {

    public ConnectionFactory getConnectionFactory() {
        return new ConnectionFactory() {

            public Connection createConnection(Socket s) {
                return new NIOHTTPSConnection();
            }

            public Connection createConnection(TransportAddress ta) throws IOException {
                return new NIOHTTPSConnection();
            }
        };
    }

    public TransportProtocol getProtocol() {
        return HTTPSProtocol.getInstance();
    }

}
