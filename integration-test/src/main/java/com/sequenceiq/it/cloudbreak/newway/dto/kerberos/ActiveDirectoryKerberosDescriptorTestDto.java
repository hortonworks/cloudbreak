package com.sequenceiq.it.cloudbreak.newway.dto.kerberos;

import com.sequenceiq.cloudbreak.api.endpoint.v4.kerberos.requests.ActiveDirectoryKerberosDescriptor;
import com.sequenceiq.it.cloudbreak.newway.Prototype;
import com.sequenceiq.it.cloudbreak.newway.context.TestContext;

@Prototype
public class ActiveDirectoryKerberosDescriptorTestDto
        extends AbstractKerberosDescriptorTestDto<ActiveDirectoryKerberosDescriptor, ActiveDirectoryKerberosDescriptorTestDto> {

    protected ActiveDirectoryKerberosDescriptorTestDto(TestContext testContext) {
        super(new ActiveDirectoryKerberosDescriptor(), testContext);
    }

    @Override
    public ActiveDirectoryKerberosDescriptorTestDto valid() {
        return withTpcAllowed(true)
                .withPrincipal("admin/principal")
                .withPassword("kerberosPassword")
                .withUrl("someurl.com")
                .withAdminUrl("admin.url.com")
                .withRealm("REALM.AD.COM")
                .withLdapUrl("otherurl.com")
                .withContainerDn("{}");
    }

    public ActiveDirectoryKerberosDescriptorTestDto withUrl(String value) {
        getRequest().setUrl(value);
        return this;
    }

    public ActiveDirectoryKerberosDescriptorTestDto withAdminUrl(String value) {
        getRequest().setAdminUrl(value);
        return this;
    }

    public ActiveDirectoryKerberosDescriptorTestDto withRealm(String value) {
        getRequest().setRealm(value);
        return this;
    }

    public ActiveDirectoryKerberosDescriptorTestDto withLdapUrl(String value) {
        getRequest().setLdapUrl(value);
        return this;
    }

    public ActiveDirectoryKerberosDescriptorTestDto withContainerDn(String value) {
        getRequest().setContainerDn(value);
        return this;
    }

    public ActiveDirectoryKerberosDescriptorTestDto withDomain(String value) {
        getRequest().setDomain(value);
        return this;
    }
}
