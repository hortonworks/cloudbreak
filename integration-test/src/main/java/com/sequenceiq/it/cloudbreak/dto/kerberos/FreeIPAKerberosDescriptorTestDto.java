package com.sequenceiq.it.cloudbreak.dto.kerberos;

import com.sequenceiq.freeipa.api.v1.kerberos.model.create.FreeIPAKerberosDescriptor;
import com.sequenceiq.it.cloudbreak.Prototype;
import com.sequenceiq.it.cloudbreak.context.TestContext;

@Prototype
public class FreeIPAKerberosDescriptorTestDto extends AbstractKerberosDescriptorTestDto<FreeIPAKerberosDescriptor, FreeIPAKerberosDescriptorTestDto> {

    protected FreeIPAKerberosDescriptorTestDto(TestContext testContext) {
        super(new FreeIPAKerberosDescriptor(), testContext);
    }

    @Override
    public FreeIPAKerberosDescriptorTestDto valid() {
        return withAdminUrl("http://someurl.com")
                .withUrl("http://someadminurl.com")
                .withRealm("REALM.FREEIPA.COM")
                .withPassword("freeipapassword")
                .withPrincipal("kerberosPrincipal");
    }

    public FreeIPAKerberosDescriptorTestDto withUrl(String value) {
        getRequest().setUrl(value);
        return this;
    }

    public FreeIPAKerberosDescriptorTestDto withAdminUrl(String value) {
        getRequest().setAdminUrl(value);
        return this;
    }

    public FreeIPAKerberosDescriptorTestDto withRealm(String value) {
        getRequest().setRealm(value);
        return this;
    }

    public FreeIPAKerberosDescriptorTestDto withDomain(String value) {
        getRequest().setDomain(value);
        return this;
    }
}
