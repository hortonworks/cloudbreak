package com.sequenceiq.it.cloudbreak.dto.ldap;

import javax.inject.Inject;
import javax.ws.rs.WebApplicationException;

import com.sequenceiq.freeipa.api.v1.ldap.model.DirectoryType;
import com.sequenceiq.freeipa.api.v1.ldap.model.create.CreateLdapConfigRequest;
import com.sequenceiq.freeipa.api.v1.ldap.model.describe.DescribeLdapConfigResponse;
import com.sequenceiq.it.cloudbreak.CloudbreakClient;
import com.sequenceiq.it.cloudbreak.Prototype;
import com.sequenceiq.it.cloudbreak.client.LdapTestClient;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.AbstractFreeIPATestDto;
import com.sequenceiq.it.cloudbreak.dto.environment.EnvironmentTestDto;

@Prototype
public class LdapTestDto extends AbstractFreeIPATestDto<CreateLdapConfigRequest, DescribeLdapConfigResponse, LdapTestDto> {

    @Inject
    private LdapTestClient ldapTestClient;

    public LdapTestDto(TestContext testContext) {
        super(new CreateLdapConfigRequest(), testContext);
    }

    @Override
    public void cleanUp(TestContext context, CloudbreakClient cloudbreakClient) {
        LOGGER.info("Cleaning up ldapconfig with name: {}", getName());
        try {
            when(ldapTestClient.deleteV1());
        } catch (WebApplicationException ignore) {
            LOGGER.info("Something happend.");
        }
    }

    public String getName() {
        return getRequest().getEnvironmentCrn();
    }

    public LdapTestDto valid() {
        return withName(getResourceProperyProvider().getName())
                .withEnvironment(EnvironmentTestDto.class)
                .withDescription(getResourceProperyProvider().getDescription("LDAP"))
                .withBindPassword("bindPassword")
                .withAdminGroup("group")
                .withBindDn("bindDn")
                .withDescription("descrition")
                .withDirectoryType(DirectoryType.LDAP)
                .withDomain("domain")
                .withGroupMemberAttribute("memberAttribute")
                .withGroupNameAttribute("nameAttribute")
                .withGroupObjectClass("groupObjectClass")
                .withGroupSearchBase("groupSearchBase")
                .withProtocol("http")
                .withServerPort(1234)
                .withServerHost("host")
                .withUserNameAttribute("userNameAttribute")
                .withUserObjectClass("userObjectClass")
                .withUserSearchBase("userSearchBase")
                .withUserDnPattern("userDnPattern");
    }

    public LdapTestDto withRequest(CreateLdapConfigRequest request) {
        setRequest(request);
        return this;
    }

    public LdapTestDto withName(String name) {
        getRequest().setName(name);
        setName(name);
        return this;
    }

    public LdapTestDto withEnvironmentCrn(String environmentCrn) {
        getRequest().setEnvironmentCrn(environmentCrn);
        return this;

    }

    public LdapTestDto withEnvironment(Class<EnvironmentTestDto> environmentKey) {
        return withEnvironmentKey(environmentKey.getSimpleName());
    }

    public LdapTestDto withEnvironmentKey(String environmentKey) {
        EnvironmentTestDto env = getTestContext().get(environmentKey);
        if (env == null) {
            throw new IllegalArgumentException("Env is null with given key: " + environmentKey);
        }
        return withEnvironmentCrn(env.getResponse().getCrn());
    }

    public LdapTestDto withUserDnPattern(String userDnPattern) {
        getRequest().setUserDnPattern(userDnPattern);
        return this;
    }

    public LdapTestDto withBindPassword(String bindPassword) {
        getRequest().setBindPassword(bindPassword);
        return this;
    }

    public LdapTestDto withAdminGroup(String adminGroup) {
        getRequest().setAdminGroup(adminGroup);
        return this;
    }

    public LdapTestDto withBindDn(String bindDn) {
        getRequest().setBindDn(bindDn);
        return this;
    }

    public LdapTestDto withDescription(String description) {
        getRequest().setDescription(description);
        return this;
    }

    public LdapTestDto withDirectoryType(DirectoryType directoryType) {
        getRequest().setDirectoryType(directoryType);
        return this;
    }

    public LdapTestDto withDomain(String domain) {
        getRequest().setDomain(domain);
        return this;
    }

    public LdapTestDto withGroupMemberAttribute(String groupMemberAttribute) {
        getRequest().setGroupMemberAttribute(groupMemberAttribute);
        return this;
    }

    public LdapTestDto withGroupNameAttribute(String groupNameAttribute) {
        getRequest().setGroupNameAttribute(groupNameAttribute);
        return this;
    }

    public LdapTestDto withGroupObjectClass(String groupObjectClass) {
        getRequest().setGroupObjectClass(groupObjectClass);
        return this;
    }

    public LdapTestDto withGroupSearchBase(String groupSearchBase) {
        getRequest().setGroupSearchBase(groupSearchBase);
        return this;
    }

    public LdapTestDto withProtocol(String protocol) {
        getRequest().setProtocol(protocol);
        return this;
    }

    public LdapTestDto withServerPort(Integer serverPort) {
        getRequest().setPort(serverPort);
        return this;
    }

    public LdapTestDto withServerHost(String serverHost) {
        getRequest().setHost(serverHost);
        return this;
    }

    public LdapTestDto withUserNameAttribute(String userNameAttribute) {
        getRequest().setUserNameAttribute(userNameAttribute);
        return this;
    }

    public LdapTestDto withUserObjectClass(String userObjectClass) {
        getRequest().setUserObjectClass(userObjectClass);
        return this;
    }

    public LdapTestDto withUserSearchBase(String userSearchBase) {
        getRequest().setUserSearchBase(userSearchBase);
        return this;
    }

    @Override
    public int order() {
        return 500;
    }
}
