package ru.chousik.erpnext.ra;

import jakarta.resource.NotSupportedException;
import jakarta.resource.ResourceException;
import jakarta.resource.cci.Connection;
import jakarta.resource.cci.ConnectionMetaData;
import jakarta.resource.cci.Interaction;
import jakarta.resource.cci.LocalTransaction;
import jakarta.resource.cci.ResultSetInfo;

import java.io.Serializable;

public class ErpNextConnectionImpl implements Connection, Serializable {
    private final ErpNextManagedConnection managedConnection;
    private final String baseUrl;
    private final String apiKey;
    private final String apiSecret;
    private boolean closed = false;

    public ErpNextConnectionImpl(
        ErpNextManagedConnection managedConnection,
        String baseUrl,
        String apiKey,
        String apiSecret
    ) {
        this.managedConnection = managedConnection;
        this.baseUrl = baseUrl;
        this.apiKey = apiKey;
        this.apiSecret = apiSecret;
    }

    @Override
    public Interaction createInteraction() throws ResourceException {
        throw new NotSupportedException("CCI Interaction is not used");
    }

    @Override
    public LocalTransaction getLocalTransaction() throws ResourceException {
        throw new NotSupportedException("Local transaction is not supported");
    }

    @Override
    public ConnectionMetaData getMetaData() {
        return new ConnectionMetaData() {
            @Override
            public String getEISProductName() {
                return "ERPNext";
            }

            @Override
            public String getEISProductVersion() {
                return "unknown";
            }

            @Override
            public String getUserName() {
                return "token";
            }
        };
    }

    @Override
    public ResultSetInfo getResultSetInfo() throws ResourceException {
        throw new NotSupportedException("ResultSetInfo is not supported");
    }

    @Override
    public void close() {
        if (!closed) {
            closed = true;
            managedConnection.fireConnectionClosed();
        }
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public String getApiKey() {
        return apiKey;
    }

    public String getApiSecret() {
        return apiSecret;
    }
}
