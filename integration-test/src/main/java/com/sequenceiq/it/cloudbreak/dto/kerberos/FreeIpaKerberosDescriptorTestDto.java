package com.sequenceiq.it.cloudbreak.dto.kerberos;

import com.sequenceiq.freeipa.api.v1.kerberos.model.create.FreeIpaKerberosDescriptor;
import com.sequenceiq.it.cloudbreak.Prototype;
import com.sequenceiq.it.cloudbreak.context.TestContext;

@Prototype
public class FreeIpaKerberosDescriptorTestDto extends AbstractKerberosDescriptorTestDto<FreeIpaKerberosDescriptor, FreeIpaKerberosDescriptorTestDto> {

    protected FreeIpaKerberosDescriptorTestDto(TestContext testContext) {
        super(new FreeIpaKerberosDescriptor(), testContext);
    }

    @Override
    public FreeIpaKerberosDescriptorTestDto valid() {
        return withAdminUrl("http://someurl.com")
                .withUrl("http://someadminurl.com")
                .withRealm("REALM.FREEIPA.COM")
                .withPassword("freeipapassword")
                .withPrincipal("kerberosPrincipal");
    }

    public FreeIpaKerberosDescriptorTestDto withUrl(String value) {
        getRequest().setUrl(value);
        return this;
    }

    public FreeIpaKerberosDescriptorTestDto withAdminUrl(String value) {
        getRequest().setAdminUrl(value);
        return this;
    }

    public FreeIpaKerberosDescriptorTestDto withRealm(String value) {
        getRequest().setRealm(value);
        return this;
    }

    public FreeIpaKerberosDescriptorTestDto withDomain(String value) {
        getRequest().setDomain(value);
        return this;
    }
}
