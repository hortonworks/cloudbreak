package com.sequenceiq.cloudbreak.domain;


import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

import org.hibernate.annotations.Type;

import com.sequenceiq.cloudbreak.common.type.DirectoryType;

@Entity
@Table(name = "ldapconfig", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"account", "name"})
})
@NamedQueries({
        @NamedQuery(
                name = "LdapConfig.findForUser",
                query = "SELECT c FROM LdapConfig c "
                        + "WHERE c.owner= :owner"),
        @NamedQuery(
                name = "LdapConfig.findPublicInAccountForUser",
                query = "SELECT c FROM LdapConfig c "
                        + "WHERE (c.account= :account AND c.publicInAccount= true) "
                        + "OR c.owner= :owner"),
        @NamedQuery(
                name = "LdapConfig.findAllInAccount",
                query = "SELECT c FROM LdapConfig c "
                        + "WHERE c.account= :account "),
        @NamedQuery(
                name = "LdapConfig.findByNameForUser",
                query = "SELECT c FROM LdapConfig c "
                        + "WHERE c.name= :name and c.owner= :owner "),
        @NamedQuery(
                name = "LdapConfig.findByNameInAccount",
                query = "SELECT c FROM LdapConfig c WHERE c.name= :name and c.account= :account")
})
public class LdapConfig implements ProvisionEntity {

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

    @Type(type = "encrypted_string")
    @Column(nullable = false)
    private String bindPassword;

    @Column(nullable = false)
    private String userSearchBase;

    private String userSearchFilter;

    private String userSearchAttribute;

    private String groupSearchBase;

    private String groupSearchFilter;

    private String principalRegex;

    private String domain;

    @Enumerated(EnumType.STRING)
    private DirectoryType directoryType;

    private String userObjectClass;

    private String groupObjectClass;

    private String groupIdAttribute;

    private String groupMemberAttribute;

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

    public String getUserSearchBase() {
        return userSearchBase;
    }

    public void setUserSearchBase(String userSearchBase) {
        this.userSearchBase = userSearchBase;
    }

    public String getUserSearchFilter() {
        return userSearchFilter;
    }

    public void setUserSearchFilter(String userSearchFilter) {
        this.userSearchFilter = userSearchFilter;
    }

    public String getGroupSearchBase() {
        return groupSearchBase;
    }

    public void setGroupSearchBase(String groupSearchBase) {
        this.groupSearchBase = groupSearchBase;
    }

    public String getGroupSearchFilter() {
        return groupSearchFilter;
    }

    public void setGroupSearchFilter(String groupSearchFilter) {
        this.groupSearchFilter = groupSearchFilter;
    }

    public String getPrincipalRegex() {
        return principalRegex;
    }

    public void setPrincipalRegex(String principalRegex) {
        this.principalRegex = principalRegex;
    }

    public String getUserSearchAttribute() {
        return userSearchAttribute;
    }

    public void setUserSearchAttribute(String userSearchAttribute) {
        this.userSearchAttribute = userSearchAttribute;
    }

    public String getDomain() {
        return domain;
    }

    public void setDomain(String domain) {
        this.domain = domain;
    }

    public DirectoryType getDirectoryType() {
        return directoryType;
    }

    public void setDirectoryType(DirectoryType directoryType) {
        this.directoryType = directoryType;
    }

    public String getUserObjectClass() {
        return userObjectClass;
    }

    public void setUserObjectClass(String userObjectClass) {
        this.userObjectClass = userObjectClass;
    }

    public String getGroupObjectClass() {
        return groupObjectClass;
    }

    public void setGroupObjectClass(String groupObjectClass) {
        this.groupObjectClass = groupObjectClass;
    }

    public String getGroupIdAttribute() {
        return groupIdAttribute;
    }

    public void setGroupIdAttribute(String groupIdAttribute) {
        this.groupIdAttribute = groupIdAttribute;
    }

    public String getGroupMemberAttribute() {
        return groupMemberAttribute;
    }

    public void setGroupMemberAttribute(String groupMemberAttribute) {
        this.groupMemberAttribute = groupMemberAttribute;
    }
}