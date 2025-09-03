package com.sequenceiq.freeipa.ldap;

import java.util.Objects;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;

import com.sequenceiq.cloudbreak.auth.security.AuthResource;
import com.sequenceiq.cloudbreak.common.archive.ArchivableResource;
import com.sequenceiq.cloudbreak.common.dal.model.AccountAwareResource;
import com.sequenceiq.cloudbreak.service.secret.SecretGetter;
import com.sequenceiq.cloudbreak.service.secret.SecretMarker;
import com.sequenceiq.cloudbreak.service.secret.SecretSetter;
import com.sequenceiq.cloudbreak.service.secret.SecretValue;
import com.sequenceiq.cloudbreak.service.secret.domain.Secret;
import com.sequenceiq.cloudbreak.service.secret.domain.SecretToString;
import com.sequenceiq.freeipa.api.v1.ldap.model.DirectoryType;

@Entity
public class LdapConfig implements ArchivableResource, AuthResource, AccountAwareResource {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO, generator = "ldapconfig_generator")
    @SequenceGenerator(name = "ldapconfig_generator", sequenceName = "ldapconfig_id_seq", allocationSize = 1)
    private Long id;

    @Column(nullable = false)
    private String resourceCrn;

    @Column(nullable = false)
    private String name;

    @Column(length = 1000000, columnDefinition = "TEXT")
    private String description;

    private String accountId;

    private String environmentCrn;

    /**
     * practically works as a postfix after 'ldapbind-'
     * @see com.sequenceiq.freeipa.service.binduser.LdapBindUserNameProvider
     */
    private String clusterName;

    private boolean archived;

    private Long deletionTimestamp = -1L;

    @Column(nullable = false)
    private String serverHost;

    @Column(nullable = false)
    private Integer serverPort;

    @Column(nullable = false)
    private String protocol;

    @Column(nullable = false)
    @Convert(converter = SecretToString.class)
    @SecretValue
    private Secret bindDn = Secret.EMPTY;

    @Column(nullable = false)
    @Convert(converter = SecretToString.class)
    @SecretValue
    private Secret bindPassword = Secret.EMPTY;

    @Enumerated(EnumType.STRING)
    private DirectoryType directoryType;

    @Column(nullable = false)
    private String userSearchBase;

    private String userDnPattern;

    private String userNameAttribute;

    private String userObjectClass;

    private String groupSearchBase;

    private String groupNameAttribute;

    private String groupObjectClass;

    private String groupMemberAttribute;

    private String domain;

    private String adminGroup;

    private String userGroup;

    private String certificate;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getResourceCrn() {
        return resourceCrn;
    }

    public void setResourceCrn(String resourceCrn) {
        this.resourceCrn = resourceCrn;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    @Override
    public String getAccountId() {
        return accountId;
    }

    @Override
    public void setAccountId(String accountId) {
        this.accountId = accountId;
    }

    public String getEnvironmentCrn() {
        return environmentCrn;
    }

    public void setEnvironmentCrn(String environmentCrn) {
        this.environmentCrn = environmentCrn;
    }

    @Override
    public void setDeletionTimestamp(Long timestampMillisecs) {
        deletionTimestamp = timestampMillisecs;
    }

    @Override
    public void setArchived(boolean archived) {
        this.archived = archived;
    }

    public boolean isArchived() {
        return archived;
    }

    public Long getDeletionTimestamp() {
        return deletionTimestamp;
    }

    @Override
    public void unsetRelationsToEntitiesToBeDeleted() {
    }

    public String getServerHost() {
        return serverHost;
    }

    public void setServerHost(String serverHost) {
        this.serverHost = serverHost;
    }

    public Integer getServerPort() {
        return serverPort;
    }

    public void setServerPort(Integer serverPort) {
        this.serverPort = serverPort;
    }

    public String getProtocol() {
        return protocol;
    }

    public void setProtocol(String protocol) {
        this.protocol = protocol;
    }

    public String getBindDn() {
        return bindDn.getRaw();
    }

    public String getBindDnSecret() {
        return bindDn.getSecret();
    }

    public void setBindDn(String bindDn) {
        this.bindDn = new Secret(bindDn);
    }

    public String getBindPassword() {
        return bindPassword.getRaw();
    }

    @SecretGetter(marker = SecretMarker.LDAP_CONFIG_BIND_PWD)
    public String getBindPasswordSecret() {
        return bindPassword.getSecret();
    }

    public void setBindPassword(String bindPassword) {
        this.bindPassword = new Secret(bindPassword);
    }

    @SecretSetter(marker = SecretMarker.LDAP_CONFIG_BIND_PWD)
    public void setBindPasswordSecret(Secret bindPassword) {
        this.bindPassword = bindPassword;
    }

    public DirectoryType getDirectoryType() {
        return directoryType;
    }

    public void setDirectoryType(DirectoryType directoryType) {
        this.directoryType = directoryType;
    }

    public String getUserSearchBase() {
        return userSearchBase;
    }

    public void setUserSearchBase(String userSearchBase) {
        this.userSearchBase = userSearchBase;
    }

    public String getUserNameAttribute() {
        return userNameAttribute;
    }

    public void setUserNameAttribute(String userNameAttribute) {
        this.userNameAttribute = userNameAttribute;
    }

    public String getUserObjectClass() {
        return userObjectClass;
    }

    public void setUserObjectClass(String userObjectClass) {
        this.userObjectClass = userObjectClass;
    }

    public String getGroupSearchBase() {
        return groupSearchBase;
    }

    public void setGroupSearchBase(String groupSearchBase) {
        this.groupSearchBase = groupSearchBase;
    }

    public String getGroupNameAttribute() {
        return groupNameAttribute;
    }

    public void setGroupNameAttribute(String groupNameAttribute) {
        this.groupNameAttribute = groupNameAttribute;
    }

    public String getGroupObjectClass() {
        return groupObjectClass;
    }

    public void setGroupObjectClass(String groupObjectClass) {
        this.groupObjectClass = groupObjectClass;
    }

    public String getGroupMemberAttribute() {
        return groupMemberAttribute;
    }

    public void setGroupMemberAttribute(String groupMemberAttribute) {
        this.groupMemberAttribute = groupMemberAttribute;
    }

    public String getDomain() {
        return domain;
    }

    public void setDomain(String domain) {
        this.domain = domain;
    }

    public String getAdminGroup() {
        return adminGroup;
    }

    public void setAdminGroup(String adminGroup) {
        this.adminGroup = adminGroup;
    }

    public String getUserGroup() {
        return userGroup;
    }

    public void setUserGroup(String userGroup) {
        this.userGroup = userGroup;
    }

    public String getUserDnPattern() {
        return userDnPattern;
    }

    public void setUserDnPattern(String userDnPattern) {
        this.userDnPattern = userDnPattern;
    }

    public String getCertificate() {
        return certificate;
    }

    public void setCertificate(String certificate) {
        this.certificate = certificate;
    }

    public String getClusterName() {
        return clusterName;
    }

    public void setClusterName(String clusterName) {
        this.clusterName = clusterName;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        LdapConfig that = (LdapConfig) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "LdapConfig{" +
                "id=" + id +
                ", resourceCrn='" + resourceCrn + '\'' +
                ", name='" + name + '\'' +
                ", description='" + description + '\'' +
                ", accountId='" + accountId + '\'' +
                ", environmentCrn='" + environmentCrn + '\'' +
                ", clusterName='" + clusterName + '\'' +
                ", archived=" + archived +
                ", deletionTimestamp=" + deletionTimestamp +
                ", serverHost='" + serverHost + '\'' +
                ", serverPort=" + serverPort +
                ", protocol='" + protocol + '\'' +
                ", directoryType=" + directoryType +
                ", userSearchBase='" + userSearchBase + '\'' +
                ", userDnPattern='" + userDnPattern + '\'' +
                ", userNameAttribute='" + userNameAttribute + '\'' +
                ", userObjectClass='" + userObjectClass + '\'' +
                ", groupSearchBase='" + groupSearchBase + '\'' +
                ", groupNameAttribute='" + groupNameAttribute + '\'' +
                ", groupObjectClass='" + groupObjectClass + '\'' +
                ", groupMemberAttribute='" + groupMemberAttribute + '\'' +
                ", domain='" + domain + '\'' +
                ", adminGroup='" + adminGroup + '\'' +
                ", userGroup='" + userGroup + '\'' +
                ", certificate='" + certificate + '\'' +
                '}';
    }
}
