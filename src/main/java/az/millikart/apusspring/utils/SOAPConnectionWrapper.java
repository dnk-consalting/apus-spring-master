package az.millikart.apusspring.utils;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import java.io.IOException;

// Switch to jakarta SOAP API and remove internal sun.* handler usage
import jakarta.xml.soap.SOAPConnection;
import jakarta.xml.soap.SOAPException;
import jakarta.xml.soap.SOAPMessage;

import java.net.*;

public class SOAPConnectionWrapper extends SOAPConnection {

    private SOAPConnection connection;
    private SSLContext context;
    private Proxy proxy;

    public SOAPConnectionWrapper(
            SOAPConnection connection
    ) {
        this.connection = connection;
    }

    public SOAPConnectionWrapper(
            SOAPConnection connection,
            Proxy proxy
    ) {
        this.connection = connection;
        this.proxy = proxy;
    }

    public void setSSLContext(
            SSLContext context
    ) {
        this.context = context;
    }

    @Override
    public SOAPMessage call(
            SOAPMessage message, Object endpoint
    ) throws SOAPException {
        try {
            // Support both String and URL endpoint types
            final URL url = (endpoint instanceof URL)
                    ? (URL) endpoint
                    : new URL(String.valueOf(endpoint));

            return connection.call(message, new URL(
                    url.getProtocol(),
                    url.getHost(),
                    url.getPort(),
                    url.getFile(),
                    new URLStreamHandler() {
                        @Override
                        protected URLConnection openConnection(URL u) throws IOException {
                            URL target = new URL(u.toString());
                            URLConnection rawConn = (proxy != null) ? target.openConnection(proxy) : target.openConnection();
                            if (!(rawConn instanceof HttpsURLConnection)) {
                                return rawConn;
                            }
                            HttpsURLConnection https = (HttpsURLConnection) rawConn;
                            if (context != null) {
                                try {
                                    https.setSSLSocketFactory(context.getSocketFactory());
                                } catch (Exception e) {
                                    throw new IOException(e);
                                }
                            }
                            return https;
                        }
                    }));
        } catch (MalformedURLException e) {
            throw new SOAPException(e);
        }
    }

    @Override
    public void close() throws SOAPException {
        connection.close();
    }
}
