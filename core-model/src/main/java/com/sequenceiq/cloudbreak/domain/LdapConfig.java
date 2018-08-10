package com.sequenceiq.cloudbreak.domain;

import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;

import com.sequenceiq.cloudbreak.api.model.DirectoryType;
import com.sequenceiq.cloudbreak.authorization.OrganizationResource;
import com.sequenceiq.cloudbreak.domain.converter.EncryptionConverter;
import com.sequenceiq.cloudbreak.domain.organization.Organization;
import com.sequenceiq.cloudbreak.domain.organization.OrganizationAwareResource;

@Entity
@Table(name = "ldapconfig")
public class LdapConfig implements ProvisionEntity, OrganizationAwareResource {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO, generator = "ldapconfig_generator")
    @SequenceGenerator(name = "ldapconfig_generator", sequenceName = "ldapconfig_id_seq", allocationSize = 1)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(length = 1000000, columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false)
    private String account;

    @Column(nullable = false)
    private String owner;

    @Column(nullable = false)
    private boolean publicInAccount;

    @Column(nullable = false)
    private String serverHost;

    @Column(nullable = false)
    private Integer serverPort;

    @Column(nullable = false)
    private String protocol;

    @Column(nullable = false)
    private String bindDn;

    @Convert(converter = EncryptionConverter.class)
    @Column(nullable = false)
    private String bindPassword;

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

    @ManyToOne
    private Organization organization;

    public LdapConfig copyWithoutOrganization() {
        LdapConfig copy = new LdapConfig();
        copy.setId(id);
        copy.setName(name);
        copy.setDescription(description);
        copy.setAccount(account);
        copy.setOwner(owner);
        copy.setPublicInAccount(publicInAccount);
        copy.setServerHost(serverHost);
        copy.setServerPort(serverPort);
        copy.setProtocol(protocol);
        copy.setBindDn(bindDn);
        copy.setBindPassword(bindPassword);
        copy.setDirectoryType(directoryType);
        copy.setUserSearchBase(userSearchBase);
        copy.setUserDnPattern(userDnPattern);
        copy.setUserNameAttribute(userNameAttribute);
        copy.setUserObjectClass(userObjectClass);
        copy.setGroupSearchBase(groupSearchBase);
        copy.setGroupNameAttribute(groupNameAttribute);
        copy.setGroupObjectClass(groupObjectClass);
        copy.setGroupMemberAttribute(groupMemberAttribute);
        copy.setDomain(domain);
        copy.setAdminGroup(adminGroup);
        return copy;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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

    public String getAccount() {
        return account;
    }

    public void setAccount(String account) {
        this.account = account;
    }

    public String getOwner() {
        return owner;
    }

    public void setOwner(String owner) {
        this.owner = owner;
    }

    public boolean isPublicInAccount() {
        return publicInAccount;
    }

    public void setPublicInAccount(boolean publicInAccount) {
        this.publicInAccount = publicInAccount;
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
        return bindDn;
    }

    public void setBindDn(String bindDn) {
        this.bindDn = bindDn;
    }

    public String getBindPassword() {
        return bindPassword;
    }

    public void setBindPassword(String bindPassword) {
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

    public String getUserDnPattern() {
        return userDnPattern;
    }

    public void setUserDnPattern(String userDnPattern) {
        this.userDnPattern = userDnPattern;
    }

    @Override
    public Organization getOrganization() {
        return organization;
    }

    @Override
    public void setOrganization(Organization organization) {
        this.organization = organization;
    }

    @Override
    public OrganizationResource getResource() {
        return OrganizationResource.LDAP;
    }
}