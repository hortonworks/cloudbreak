package com.sequenceiq.it.cloudbreak.dto.kerberos;

import javax.ws.rs.core.Response;

import com.sequenceiq.cloudbreak.api.endpoint.v4.kerberos.requests.KerberosTypeBase;
import com.sequenceiq.it.cloudbreak.dto.AbstractCloudbreakTestDto;
import com.sequenceiq.it.cloudbreak.context.TestContext;

public abstract class AbstractKerberosDescriptorTestDto<R extends KerberosTypeBase, E extends AbstractKerberosDescriptorTestDto>
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
