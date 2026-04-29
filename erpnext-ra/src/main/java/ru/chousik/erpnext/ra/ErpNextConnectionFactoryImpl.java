package ru.chousik.erpnext.ra;

import jakarta.resource.ResourceException;
import jakarta.resource.cci.Connection;
import jakarta.resource.cci.ConnectionFactory;
import jakarta.resource.cci.ConnectionSpec;
import jakarta.resource.cci.RecordFactory;
import jakarta.resource.cci.ResourceAdapterMetaData;
import jakarta.resource.spi.ConnectionManager;

import javax.naming.Reference;
import java.io.Serializable;

public class ErpNextConnectionFactoryImpl implements ConnectionFactory, Serializable {
    private ErpNextManagedConnectionFactory mcf;
    private ConnectionManager connectionManager;
    private final RecordFactory recordFactory = new SimpleRecordFactory();
    private Reference reference;

    public ErpNextConnectionFactoryImpl() {
    }

    public ErpNextConnectionFactoryImpl(ErpNextManagedConnectionFactory mcf, ConnectionManager connectionManager) {
        this.mcf = mcf;
        this.connectionManager = connectionManager;
    }

    @Override
    public Connection getConnection() throws ResourceException {
        return (Connection) connectionManager.allocateConnection(mcf, null);
    }

    @Override
    public Connection getConnection(ConnectionSpec properties) throws ResourceException {
        return getConnection();
    }

    @Override
    public ResourceAdapterMetaData getMetaData() {
        return new ResourceAdapterMetaData() {
            @Override
            public String getAdapterVersion() {
                return "1.0.0";
            }

            @Override
            public String getAdapterVendorName() {
                return "ru.chousik";
            }

            @Override
            public String getAdapterName() {
                return "ERPNext RA";
            }

            @Override
            public String getAdapterShortDescription() {
                return "ERPNext outbound resource adapter";
            }

            @Override
            public String getSpecVersion() {
                return "2.1";
            }

            @Override
            public String[] getInteractionSpecsSupported() {
                return new String[0];
            }

            @Override
            public boolean supportsExecuteWithInputAndOutputRecord() {
                return true;
            }

            @Override
            public boolean supportsExecuteWithInputRecordOnly() {
                return true;
            }

            @Override
            public boolean supportsLocalTransactionDemarcation() {
                return false;
            }
        };
    }

    @Override
    public RecordFactory getRecordFactory() throws ResourceException {
        return recordFactory;
    }

    @Override
    public void setReference(Reference reference) {
        this.reference = reference;
    }

    @Override
    public Reference getReference() {
        return reference;
    }
}
