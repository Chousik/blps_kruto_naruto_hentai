package ru.chousik.erpnext.ra;

import jakarta.resource.ResourceException;
import jakarta.resource.spi.ConnectionManager;
import jakarta.resource.spi.ConnectionRequestInfo;
import jakarta.resource.spi.ManagedConnection;
import jakarta.resource.spi.ManagedConnectionFactory;

import java.io.PrintWriter;
import java.util.Objects;
import java.util.Set;
import javax.security.auth.Subject;

public class ErpNextManagedConnectionFactory implements ManagedConnectionFactory {
    private String baseUrl;
    private String apiKey;
    private String apiSecret;
    private transient PrintWriter logWriter;

    @Override
    public Object createConnectionFactory(ConnectionManager cxManager) throws ResourceException {
        return new ErpNextConnectionFactoryImpl(this, cxManager);
    }

    @Override
    public Object createConnectionFactory() throws ResourceException {
        return new ErpNextConnectionFactoryImpl(this, new ErpNextConnectionManager());
    }

    @Override
    public ManagedConnection createManagedConnection(Subject subject, ConnectionRequestInfo cxRequestInfo)
        throws ResourceException {
        return new ErpNextManagedConnection(this, baseUrl, apiKey, apiSecret);
    }

    @Override
    public ManagedConnection matchManagedConnections(Set connectionSet, Subject subject, ConnectionRequestInfo cxRequestInfo)
        throws ResourceException {
        for (Object candidate : connectionSet) {
            if (candidate instanceof ErpNextManagedConnection) {
                return (ManagedConnection) candidate;
            }
        }
        return null;
    }

    @Override
    public void setLogWriter(PrintWriter out) {
        this.logWriter = out;
    }

    @Override
    public PrintWriter getLogWriter() {
        return logWriter;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    public String getApiSecret() {
        return apiSecret;
    }

    public void setApiSecret(String apiSecret) {
        this.apiSecret = apiSecret;
    }

    @Override
    public int hashCode() {
        return Objects.hash(baseUrl, apiKey, apiSecret);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        ErpNextManagedConnectionFactory that = (ErpNextManagedConnectionFactory) obj;
        return Objects.equals(baseUrl, that.baseUrl)
            && Objects.equals(apiKey, that.apiKey)
            && Objects.equals(apiSecret, that.apiSecret);
    }
}
