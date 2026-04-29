package ru.chousik.erpnext.ra;

import jakarta.resource.ResourceException;
import jakarta.resource.spi.ConnectionManager;
import jakarta.resource.spi.ConnectionRequestInfo;
import jakarta.resource.spi.ManagedConnection;
import jakarta.resource.spi.ManagedConnectionFactory;

import java.io.PrintWriter;
import java.net.URI;
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
        throw new ResourceException(
            "Standalone ERPNext ConnectionFactory is not supported; use a container-managed ConnectionManager"
        );
    }

    @Override
    public ManagedConnection createManagedConnection(Subject subject, ConnectionRequestInfo cxRequestInfo)
        throws ResourceException {
        String resolvedBaseUrl = resolveConfiguredValue(baseUrl, "ERPNEXT_BASE_URL");
        String resolvedApiKey = resolveConfiguredValue(apiKey, "ERPNEXT_API_KEY");
        String resolvedApiSecret = resolveConfiguredValue(apiSecret, "ERPNEXT_API_SECRET");

        validateBaseUrl(resolvedBaseUrl);
        requireConfigured("ApiKey", resolvedApiKey);
        requireConfigured("ApiSecret", resolvedApiSecret);

        return new ErpNextManagedConnection(resolvedBaseUrl, resolvedApiKey, resolvedApiSecret);
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
        this.baseUrl = normalizeConfiguredValue(baseUrl);
    }

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = normalizeConfiguredValue(apiKey);
    }

    public String getApiSecret() {
        return apiSecret;
    }

    public void setApiSecret(String apiSecret) {
        this.apiSecret = normalizeConfiguredValue(apiSecret);
    }

    private static String normalizeConfiguredValue(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private static String resolveConfiguredValue(String value, String fallbackEnvName) {
        String normalized = normalizeConfiguredValue(value);
        if (normalized == null) {
            return normalizeConfiguredValue(System.getenv(fallbackEnvName));
        }

        if (normalized.startsWith("${") && normalized.endsWith("}")) {
            String expression = normalized.substring(2, normalized.length() - 1);
            String defaultValue = null;
            int defaultSeparatorIndex = expression.indexOf(':');
            if (defaultSeparatorIndex >= 0) {
                defaultValue = expression.substring(defaultSeparatorIndex + 1);
                expression = expression.substring(0, defaultSeparatorIndex);
            }

            String resolved = null;
            if (expression.startsWith("env.")) {
                resolved = System.getenv(expression.substring(4));
            } else {
                resolved = System.getProperty(expression);
                if (resolved == null) {
                    resolved = System.getenv(expression);
                }
            }

            if (resolved == null || resolved.isBlank()) {
                resolved = defaultValue;
            }

            if (resolved == null || resolved.isBlank()) {
                resolved = System.getenv(fallbackEnvName);
            }

            return normalizeConfiguredValue(resolved);
        }

        return normalized;
    }

    private static void requireConfigured(String propertyName, String value) throws ResourceException {
        if (value == null) {
            throw new ResourceException("ERPNext RA property '" + propertyName + "' is not configured");
        }
    }

    private static void validateBaseUrl(String baseUrl) throws ResourceException {
        requireConfigured("BaseUrl", baseUrl);
        try {
            URI uri = URI.create(baseUrl);
            if (uri.getScheme() == null || uri.getScheme().isBlank()) {
                throw new ResourceException("ERPNext RA BaseUrl must be an absolute URL, got '" + baseUrl + "'");
            }
        } catch (IllegalArgumentException ex) {
            throw new ResourceException("ERPNext RA BaseUrl is invalid: '" + baseUrl + "'", ex);
        }
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
