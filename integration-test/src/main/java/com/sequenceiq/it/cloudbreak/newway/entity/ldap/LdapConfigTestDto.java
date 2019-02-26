package com.sequenceiq.it.cloudbreak.newway.entity.ldap;

import static com.sequenceiq.it.cloudbreak.newway.util.ResponseUtil.getErrorMessage;

import java.util.List;
import java.util.stream.Collectors;

import javax.ws.rs.WebApplicationException;

import com.sequenceiq.cloudbreak.api.endpoint.v4.ldaps.DirectoryType;
import com.sequenceiq.cloudbreak.api.endpoint.v4.ldaps.LdapConfigV4Endpoint;
import com.sequenceiq.cloudbreak.api.endpoint.v4.ldaps.requests.LdapV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.ldaps.responses.LdapV4Response;
import com.sequenceiq.it.cloudbreak.newway.RandomNameCreator;
import com.sequenceiq.it.cloudbreak.newway.entity.AbstractCloudbreakEntity;
import com.sequenceiq.it.cloudbreak.newway.CloudbreakClient;
import com.sequenceiq.it.cloudbreak.newway.Prototype;
import com.sequenceiq.it.cloudbreak.newway.context.Purgable;
import com.sequenceiq.it.cloudbreak.newway.context.TestContext;

@Prototype
public class LdapConfigTestDto extends AbstractCloudbreakEntity<LdapV4Request, LdapV4Response, LdapConfigTestDto> implements Purgable<LdapV4Response> {

    public LdapConfigTestDto(TestContext testContext) {
        super(new LdapV4Request(), testContext);
    }

    @Override
    public void cleanUp(TestContext context, CloudbreakClient cloudbreakClient) {
        LOGGER.info("Cleaning up resource with name: {}", getName());
        try {
            cloudbreakClient.getCloudbreakClient().ldapConfigV4Endpoint().delete(cloudbreakClient.getWorkspaceId(), getName());
        } catch (WebApplicationException ignore) {
            LOGGER.info("Something happend.");
        }
    }

    public String getName() {
        return getRequest().getName();
    }

    public LdapConfigTestDto valid() {
        return withName(getNameCreator().getRandomNameForResource())
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

    public LdapConfigTestDto withRequest(LdapV4Request request) {
        setRequest(request);
        return this;
    }

    public LdapConfigTestDto withName(String name) {
        getRequest().setName(name);
        setName(name);
        return this;
    }

    public LdapConfigTestDto withUserDnPattern(String userDnPattern) {
        getRequest().setUserDnPattern(userDnPattern);
        return this;
    }

    public LdapConfigTestDto withBindPassword(String bindPassword) {
        getRequest().setBindPassword(bindPassword);
        return this;
    }

    public LdapConfigTestDto withAdminGroup(String adminGroup) {
        getRequest().setAdminGroup(adminGroup);
        return this;
    }

    public LdapConfigTestDto withBindDn(String bindDn) {
        getRequest().setBindDn(bindDn);
        return this;
    }

    public LdapConfigTestDto withDescription(String description) {
        getRequest().setDescription(description);
        return this;
    }

    public LdapConfigTestDto withDirectoryType(DirectoryType directoryType) {
        getRequest().setDirectoryType(directoryType);
        return this;
    }

    public LdapConfigTestDto withDomain(String domain) {
        getRequest().setDomain(domain);
        return this;
    }

    public LdapConfigTestDto withGroupMemberAttribute(String groupMemberAttribute) {
        getRequest().setGroupMemberAttribute(groupMemberAttribute);
        return this;
    }

    public LdapConfigTestDto withGroupNameAttribute(String groupNameAttribute) {
        getRequest().setGroupNameAttribute(groupNameAttribute);
        return this;
    }

    public LdapConfigTestDto withGroupObjectClass(String groupObjectClass) {
        getRequest().setGroupObjectClass(groupObjectClass);
        return this;
    }

    public LdapConfigTestDto withGroupSearchBase(String groupSearchBase) {
        getRequest().setGroupSearchBase(groupSearchBase);
        return this;
    }

    public LdapConfigTestDto withProtocol(String protocol) {
        getRequest().setProtocol(protocol);
        return this;
    }

    public LdapConfigTestDto withServerPort(Integer serverPort) {
        getRequest().setPort(serverPort);
        return this;
    }

    public LdapConfigTestDto withServerHost(String serverHost) {
        getRequest().setHost(serverHost);
        return this;
    }

    public LdapConfigTestDto withUserNameAttribute(String userNameAttribute) {
        getRequest().setUserNameAttribute(userNameAttribute);
        return this;
    }

    public LdapConfigTestDto withUserObjectClass(String userObjectClass) {
        getRequest().setUserObjectClass(userObjectClass);
        return this;
    }

    public LdapConfigTestDto withUserSearchBase(String userSearchBase) {
        getRequest().setUserSearchBase(userSearchBase);
        return this;
    }

    @Override
    public List<LdapV4Response> getAll(CloudbreakClient client) {
        LdapConfigV4Endpoint ldapConfigV4Endpoint = client.getCloudbreakClient().ldapConfigV4Endpoint();
        return ldapConfigV4Endpoint.list(client.getWorkspaceId(), null, false).getResponses()
                .stream()
                .filter(s -> s.getName() != null)
                .collect(Collectors.toList());
    }

    @Override
    public boolean deletable(LdapV4Response entity) {
        return entity.getName().startsWith(RandomNameCreator.PREFIX);
    }

    @Override
    public void delete(TestContext testContext, LdapV4Response entity, CloudbreakClient client) {
        try {
            client.getCloudbreakClient().ldapConfigV4Endpoint().delete(client.getWorkspaceId(), entity.getName());
        } catch (Exception e) {
            LOGGER.warn("Something went wrong on {} purge. {}", entity.getName(), getErrorMessage(e), e);
        }
    }

    @Override
    public int order() {
        return 500;
    }
}