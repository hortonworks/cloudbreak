package com.sequenceiq.it.cloudbreak.newway.dto.kerberos;

import com.sequenceiq.cloudbreak.api.endpoint.v4.kerberos.requests.FreeIPAKerberosDescriptor;
import com.sequenceiq.it.cloudbreak.newway.Prototype;
import com.sequenceiq.it.cloudbreak.newway.context.TestContext;

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
