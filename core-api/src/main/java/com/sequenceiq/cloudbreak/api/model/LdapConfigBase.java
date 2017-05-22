package com.sequenceiq.cloudbreak.api.model;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import javax.validation.constraints.Min;
import javax.validation.constraints.Max;

import com.sequenceiq.cloudbreak.common.type.DirectoryType;
import com.sequenceiq.cloudbreak.doc.ModelDescriptions;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel
public abstract class LdapConfigBase implements JsonEntity {

    @Size(max = 100, min = 1, message = "The length of the ldap config's name has to be in range of 1 to 100")
    @NotNull
    @ApiModelProperty(value = ModelDescriptions.NAME, required = true)
    private String name;

    @Size(max = 1000)
    @ApiModelProperty(value = ModelDescriptions.DESCRIPTION)
    private String description;

    @NotNull
    @ApiModelProperty(value = ModelDescriptions.LdapConfigModelDescription.SERVER_HOST, required = true)
    private String serverHost;

    @NotNull
    @Max(65535)
    @Min(1)
    @ApiModelProperty(value = ModelDescriptions.LdapConfigModelDescription.SERVER_PORT, required = true)
    private Integer serverPort;

    @ApiModelProperty(value = ModelDescriptions.LdapConfigModelDescription.PROTOCOL)
    private String protocol = "ldap";

    @NotNull
    @ApiModelProperty(value = ModelDescriptions.LdapConfigModelDescription.BIND_DN, required = true)
    private String bindDn;

    @NotNull
    @ApiModelProperty(value = ModelDescriptions.LdapConfigModelDescription.USER_SEARCH_BASE, required = true)
    private String userSearchBase;

    @ApiModelProperty(value = ModelDescriptions.LdapConfigModelDescription.USER_SEARCH_FILTER)
    private String userSearchFilter;

    @ApiModelProperty(value = ModelDescriptions.LdapConfigModelDescription.GROUP_SEARCH_BASE)
    private String groupSearchBase;

    @ApiModelProperty(value = ModelDescriptions.LdapConfigModelDescription.GROUP_SEARCH_FILTER)
    private String groupSearchFilter;

    @ApiModelProperty(value = ModelDescriptions.LdapConfigModelDescription.PRINCIPAL_REGEX)
    private String principalRegex;

    @ApiModelProperty(value = ModelDescriptions.LdapConfigModelDescription.USER_SEARCH_ATTRIBUTE)
    private String userSearchAttribute;

    @ApiModelProperty(value = ModelDescriptions.LdapConfigModelDescription.DOMAIN)
    private String domain;

    @ApiModelProperty(value = ModelDescriptions.LdapConfigModelDescription.DIRECTORY_TYPE)
    private DirectoryType directoryType;

    @ApiModelProperty(value = ModelDescriptions.LdapConfigModelDescription.USER_OBJECT_CLASS)
    private String userObjectClass;

    @ApiModelProperty(value = ModelDescriptions.LdapConfigModelDescription.GROUP_OBJECT_CLASS)
    private String groupObjectClass;

    @ApiModelProperty(value = ModelDescriptions.LdapConfigModelDescription.GROUP_ID_ATTRIBUTE)
    private String groupIdAttribute;

    @ApiModelProperty(value = ModelDescriptions.LdapConfigModelDescription.GROUP_MEMBER_ATTRIBUTE)
    private String groupMemberAttribute;

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
