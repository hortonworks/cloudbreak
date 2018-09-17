package com.sequenceiq.it.cloudbreak.newway;

import com.sequenceiq.cloudbreak.api.model.DirectoryType;
import com.sequenceiq.cloudbreak.api.model.ldap.LdapConfigRequest;
import com.sequenceiq.cloudbreak.api.model.ldap.LdapConfigResponse;

public class LdapConfigEntity extends AbstractCloudbreakEntity<LdapConfigRequest, LdapConfigResponse, LdapConfigEntity> {
    public static final String LDAP_CONFIG = "LDAP_CONFIG";

    LdapConfigEntity(String newId) {
        super(newId);
        setRequest(new LdapConfigRequest());
    }

    LdapConfigEntity() {
        this(LDAP_CONFIG);
    }

    public LdapConfigEntity withName(String name) {
        getRequest().setName(name);
        setName(name);
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
}