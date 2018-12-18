package com.sequenceiq.it.cloudbreak.newway;

import java.util.List;
import java.util.stream.Collectors;

import javax.ws.rs.WebApplicationException;

import com.sequenceiq.cloudbreak.api.endpoint.v3.LdapConfigV3Endpoint;
import com.sequenceiq.cloudbreak.api.model.DirectoryType;
import com.sequenceiq.cloudbreak.api.model.ldap.LdapConfigRequest;
import com.sequenceiq.cloudbreak.api.model.ldap.LdapConfigResponse;
import com.sequenceiq.it.cloudbreak.newway.context.Purgable;
import com.sequenceiq.it.cloudbreak.newway.context.TestContext;

public class LdapConfigEntity extends AbstractCloudbreakEntity<LdapConfigRequest, LdapConfigResponse, LdapConfigEntity> implements Purgable<LdapConfigResponse> {
    public static final String LDAP_CONFIG = "LDAP_CONFIG";

    public LdapConfigEntity(String newId) {
        super(newId);
        setRequest(new LdapConfigRequest());
    }

    public LdapConfigEntity() {
        this(LDAP_CONFIG);
    }

    public LdapConfigEntity(TestContext testContext) {
        super(new LdapConfigRequest(), testContext);
    }

    @Override
    public void cleanUp(TestContext context, CloudbreakClient cloudbreakClient) {
        LOGGER.info("Cleaning up resource with name: {}", getName());
        try {
            cloudbreakClient.getCloudbreakClient().ldapConfigV3Endpoint().deleteInWorkspace(cloudbreakClient.getWorkspaceId(), getName());
        } catch (WebApplicationException ignore) {
            LOGGER.info("Something happend.");
        }
    }

    public LdapConfigEntity valid() {
        return withName(getNameCreator().getRandomNameForMock())
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

    public LdapConfigEntity withRequest(LdapConfigRequest request) {
        setRequest(request);
        return this;
    }

    public LdapConfigEntity withName(String name) {
        getRequest().setName(name);
        setName(name);
        return this;
    }

    public LdapConfigEntity withUserDnPattern(String userDnPattern) {
        getRequest().setUserDnPattern(userDnPattern);
        return this;
    }

    public LdapConfigEntity withBindPassword(String bindPassword) {
        getRequest().setBindPassword(bindPassword);
        return this;
    }

    public LdapConfigEntity withAdminGroup(String adminGroup) {
        getRequest().setAdminGroup(adminGroup);
        return this;
    }

    public LdapConfigEntity withBindDn(String bindDn) {
        getRequest().setBindDn(bindDn);
        return this;
    }

    public LdapConfigEntity withDescription(String description) {
        getRequest().setDescription(description);
        return this;
    }

    public LdapConfigEntity withDirectoryType(DirectoryType directoryType) {
        getRequest().setDirectoryType(directoryType);
        return this;
    }

    public LdapConfigEntity withDomain(String domain) {
        getRequest().setDomain(domain);
        return this;
    }

    public LdapConfigEntity withGroupMemberAttribute(String groupMemberAttribute) {
        getRequest().setGroupMemberAttribute(groupMemberAttribute);
        return this;
    }

    public LdapConfigEntity withGroupNameAttribute(String groupNameAttribute) {
        getRequest().setGroupNameAttribute(groupNameAttribute);
        return this;
    }

    public LdapConfigEntity withGroupObjectClass(String groupObjectClass) {
        getRequest().setGroupObjectClass(groupObjectClass);
        return this;
    }

    public LdapConfigEntity withGroupSearchBase(String groupSearchBase) {
        getRequest().setGroupSearchBase(groupSearchBase);
        return this;
    }

    public LdapConfigEntity withProtocol(String protocol) {
        getRequest().setProtocol(protocol);
        return this;
    }

    public LdapConfigEntity withServerPort(Integer serverPort) {
        getRequest().setServerPort(serverPort);
        return this;
    }

    public LdapConfigEntity withServerHost(String serverHost) {
        getRequest().setServerHost(serverHost);
        return this;
    }

    public LdapConfigEntity withUserNameAttribute(String userNameAttribute) {
        getRequest().setUserNameAttribute(userNameAttribute);
        return this;
    }

    public LdapConfigEntity withUserObjectClass(String userObjectClass) {
        getRequest().setUserObjectClass(userObjectClass);
        return this;
    }

    public LdapConfigEntity withUserSearchBase(String userSearchBase) {
        getRequest().setUserSearchBase(userSearchBase);
        return this;
    }

    @Override
    public List<LdapConfigResponse> getAll(CloudbreakClient client) {
        LdapConfigV3Endpoint ldapConfigV3Endpoint = client.getCloudbreakClient().ldapConfigV3Endpoint();
        return ldapConfigV3Endpoint.listConfigsByWorkspace(client.getWorkspaceId(), null, null).stream()
                .filter(s -> s.getName() != null)
                .collect(Collectors.toList());
    }

    @Override
    public boolean deletable(LdapConfigResponse entity) {
        return entity.getName().startsWith("mock-");
    }

    @Override
    public void delete(LdapConfigResponse entity, CloudbreakClient client) {
        try {
            client.getCloudbreakClient().ldapConfigV3Endpoint().deleteInWorkspace(client.getWorkspaceId(), entity.getName());
        } catch (Exception e) {
            LOGGER.warn("Something went wrong on {} purge. {}", entity.getName(), e.getMessage(), e);
        }
    }

    @Override
    public int order() {
        return 500;
    }
}