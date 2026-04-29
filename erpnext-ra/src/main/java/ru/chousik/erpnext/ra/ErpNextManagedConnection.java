package ru.chousik.erpnext.ra;

import jakarta.resource.ResourceException;
import jakarta.resource.spi.ConnectionEvent;
import jakarta.resource.spi.ConnectionEventListener;
import jakarta.resource.spi.ConnectionRequestInfo;
import jakarta.resource.spi.LocalTransaction;
import jakarta.resource.spi.ManagedConnection;
import jakarta.resource.spi.ManagedConnectionMetaData;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import javax.security.auth.Subject;
import javax.transaction.xa.XAResource;

public class ErpNextManagedConnection implements ManagedConnection {
    private final ErpNextManagedConnectionFactory mcf;
    private final String baseUrl;
    private final String apiKey;
    private final String apiSecret;
    private final List<ConnectionEventListener> listeners = new ArrayList<>();
    private PrintWriter logWriter;

    public ErpNextManagedConnection(ErpNextManagedConnectionFactory mcf, String baseUrl, String apiKey, String apiSecret) {
        this.mcf = mcf;
        this.baseUrl = baseUrl;
        this.apiKey = apiKey;
        this.apiSecret = apiSecret;
    }

    @Override
    public Object getConnection(Subject subject, ConnectionRequestInfo cxRequestInfo) throws ResourceException {
        return new ErpNextConnectionImpl(this, baseUrl, apiKey, apiSecret);
    }

    @Override
    public void destroy() {
    }

    @Override
    public void cleanup() {
    }

    @Override
    public void associateConnection(Object connection) throws ResourceException {
        if (!(connection instanceof ErpNextConnectionImpl)) {
            throw new ResourceException("Unsupported connection handle");
        }
    }

    @Override
    public void addConnectionEventListener(ConnectionEventListener listener) {
        listeners.add(listener);
    }

    @Override
    public void removeConnectionEventListener(ConnectionEventListener listener) {
        listeners.remove(listener);
    }

    @Override
    public XAResource getXAResource() throws ResourceException {
        throw new ResourceException("XA is not supported");
    }

    @Override
    public LocalTransaction getLocalTransaction() throws ResourceException {
        throw new ResourceException("Local transaction is not supported");
    }

    @Override
    public ManagedConnectionMetaData getMetaData() {
        return new ManagedConnectionMetaData() {
            @Override
            public String getEISProductName() {
                return "ERPNext";
            }

            @Override
            public String getEISProductVersion() {
                return "unknown";
            }

            @Override
            public int getMaxConnections() {
                return 0;
            }

            @Override
            public String getUserName() {
                return "token";
            }
        };
    }

    @Override
    public void setLogWriter(PrintWriter out) {
        this.logWriter = out;
    }

    @Override
    public PrintWriter getLogWriter() {
        return logWriter;
    }

    void fireConnectionClosed() {
        ConnectionEvent event = new ConnectionEvent(this, ConnectionEvent.CONNECTION_CLOSED);
        for (ConnectionEventListener listener : listeners) {
            listener.connectionClosed(event);
        }
    }
}
