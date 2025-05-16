package ir.donbee.jade.imtp.leap.http;

//#J2ME_EXCLUDE_FILE

import ir.donbee.jade.imtp.leap.SSLHelper;
import ir.donbee.jade.mtp.TransportAddress;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import javax.net.ssl.HttpsURLConnection;

/**
 *
 * @author Eduard Drenth: Logica, 30-sep-2009
 * 
 */
class HTTPSClientConnection extends HTTPClientConnection {

    {
        System.setProperty("https.cipherSuites",(String) SSLHelper.supportedKeys.get(0));
    }

    public HTTPSClientConnection(TransportAddress ta) {
        super(ta);
    }

    protected String getProtocol() {
        return "https://";
    }

    protected HttpURLConnection open(String url) throws MalformedURLException, IOException {
        HttpsURLConnection hc = (HttpsURLConnection)new URL(url).openConnection();

        return hc;
    }

}
