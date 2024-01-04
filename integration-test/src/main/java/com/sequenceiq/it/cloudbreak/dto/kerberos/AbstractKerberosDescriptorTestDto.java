package com.sequenceiq.it.cloudbreak.dto.kerberos;

import jakarta.ws.rs.core.Response;

import com.sequenceiq.freeipa.api.v1.kerberos.model.create.KerberosDescriptorBase;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.AbstractCloudbreakTestDto;

public abstract class AbstractKerberosDescriptorTestDto<R extends KerberosDescriptorBase, E extends AbstractKerberosDescriptorTestDto>
        extends AbstractCloudbreakTestDto<R, Response, E> {

    protected AbstractKerberosDescriptorTestDto(R request, TestContext testContext) {
        super(request, testContext);
    }

    public E withTpcAllowed(Boolean value) {
        getRequest().setTcpAllowed(value);
        return (E) this;
    }

    public E withPrincipal(String value) {
        getRequest().setPrincipal(value);
        return (E) this;
    }

    public E withPassword(String value) {
        getRequest().setPassword(value);
        return (E) this;
    }
}
