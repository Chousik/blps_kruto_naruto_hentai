package ru.chousik.erpnext.ra;

import jakarta.resource.ResourceException;
import jakarta.resource.spi.ConnectionManager;
import jakarta.resource.spi.ConnectionRequestInfo;
import jakarta.resource.spi.ManagedConnection;
import jakarta.resource.spi.ManagedConnectionFactory;

public class ErpNextConnectionManager implements ConnectionManager {
    @Override
    public Object allocateConnection(ManagedConnectionFactory mcf, ConnectionRequestInfo cxRequestInfo) throws ResourceException {
        ManagedConnection mc = mcf.createManagedConnection(null, cxRequestInfo);
        return mc.getConnection(null, cxRequestInfo);
    }
}
