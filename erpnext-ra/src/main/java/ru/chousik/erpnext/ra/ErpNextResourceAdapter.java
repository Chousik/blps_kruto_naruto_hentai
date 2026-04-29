package ru.chousik.erpnext.ra;

import jakarta.resource.ResourceException;
import jakarta.resource.spi.ActivationSpec;
import jakarta.resource.spi.BootstrapContext;
import jakarta.resource.spi.ResourceAdapter;
import jakarta.resource.spi.ResourceAdapterInternalException;
import java.util.Objects;
import javax.transaction.xa.XAResource;

public class ErpNextResourceAdapter implements ResourceAdapter {
    @Override
    public void start(BootstrapContext bootstrapContext) throws ResourceAdapterInternalException {
    }

    @Override
    public void stop() {
    }

    @Override
    public void endpointActivation(jakarta.resource.spi.endpoint.MessageEndpointFactory messageEndpointFactory,
                                   ActivationSpec activationSpec) throws ResourceException {
        throw new ResourceException("Inbound endpoints are not supported");
    }

    @Override
    public void endpointDeactivation(jakarta.resource.spi.endpoint.MessageEndpointFactory messageEndpointFactory,
                                     ActivationSpec activationSpec) {
    }

    @Override
    public XAResource[] getXAResources(ActivationSpec[] activationSpecs) throws ResourceException {
        return new XAResource[0];
    }

    @Override
    public int hashCode() {
        return Objects.hash(ErpNextResourceAdapter.class);
    }

    @Override
    public boolean equals(Object obj) {
        return obj != null && getClass() == obj.getClass();
    }
}
