package com.sequenceiq.freeipa.api.v1.ldap.model;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.sequenceiq.cloudbreak.structuredevent.json.Base64Deserializer;
import com.sequenceiq.cloudbreak.structuredevent.json.Base64Serializer;
import com.sequenceiq.freeipa.api.v1.ldap.doc.LdapConfigModelDescription;
import com.sequenceiq.service.api.doc.ModelDescriptions;

import io.swagger.annotations.ApiModelProperty;

public abstract class LdapConfigBase {
    @Pattern(regexp = "(^[a-z][-a-z0-9]*[a-z0-9]$)",
            message = "The name can only contain lowercase alphanumeric characters and hyphens and has start with an alphanumeric character")
    @Size(max = 100, min = 1, message = "The length of the ldap config's name has to be in range of 1 to 100")
    @ApiModelProperty(value = ModelDescriptions.NAME, required = true)
    private String name;

    @Size(max = 1000, message = "The length of the ldap config's description has to be in range of 0 to 1000")
    @ApiModelProperty(ModelDescriptions.DESCRIPTION)
    private String description;

    @NotNull
    @ApiModelProperty(value = LdapConfigModelDescription.SERVER_HOST, required = true)
    private String host;

    @NotNull
    @Max(65535)
    @Min(1)
    @ApiModelProperty(value = LdapConfigModelDescription.SERVER_PORT, required = true)
    private Integer port;

    @ApiModelProperty(LdapConfigModelDescription.PROTOCOL)
    private String protocol = "ldap";

    @NotNull
    @ApiModelProperty(value = LdapConfigModelDescription.USER_SEARCH_BASE, required = true)
    private String userSearchBase;

    @NotNull
    @ApiModelProperty(value = LdapConfigModelDescription.USER_DN_PATTERN, required = true)
    private String userDnPattern;

    @ApiModelProperty(LdapConfigModelDescription.GROUP_SEARCH_BASE)
    private String groupSearchBase;

    @ApiModelProperty(LdapConfigModelDescription.USER_NAME_ATTRIBUTE)
    private String userNameAttribute;

    @ApiModelProperty(LdapConfigModelDescription.DOMAIN)
    private String domain;

    @ApiModelProperty(value = LdapConfigModelDescription.DIRECTORY_TYPE, allowableValues = "LDAP,ACTIVE_DIRECTORY")
    private DirectoryType directoryType = DirectoryType.ACTIVE_DIRECTORY;

    @ApiModelProperty(LdapConfigModelDescription.USER_OBJECT_CLASS)
    private String userObjectClass;

    @ApiModelProperty(LdapConfigModelDescription.GROUP_OBJECT_CLASS)
    private String groupObjectClass;

    @ApiModelProperty(LdapConfigModelDescription.GROUP_ID_ATTRIBUTE)
    private String groupNameAttribute;

    @ApiModelProperty(LdapConfigModelDescription.GROUP_MEMBER_ATTRIBUTE)
    private String groupMemberAttribute;

    @ApiModelProperty(LdapConfigModelDescription.ADMIN_GROUP)
    private String adminGroup;

    @ApiModelProperty(LdapConfigModelDescription.CERTIFICATE)
    @JsonSerialize(using = Base64Serializer.class)
    @JsonDeserialize(using = Base64Deserializer.class)
    private String certificate;

    @NotNull
    @ApiModelProperty(value = ModelDescriptions.ENVIRONMENT_CRN, required = true)
    private String environmentId;

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

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public Integer getPort() {
        return port;
    }

    public void setPort(Integer port) {
        this.port = port;
    }

    public String getProtocol() {
        return protocol;
    }

    public void setProtocol(String protocol) {
        this.protocol = protocol;
    }

    public String getUserSearchBase() {
        return userSearchBase;
    }

    public void setUserSearchBase(String userSearchBase) {
        this.userSearchBase = userSearchBase;
    }

    public String getGroupSearchBase() {
        return groupSearchBase;
    }

    public void setGroupSearchBase(String groupSearchBase) {
        this.groupSearchBase = groupSearchBase;
    }

    public String getUserNameAttribute() {
        return userNameAttribute;
    }

    public void setUserNameAttribute(String userNameAttribute) {
        this.userNameAttribute = userNameAttribute;
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

    public String getGroupNameAttribute() {
        return groupNameAttribute;
    }

    public void setGroupNameAttribute(String groupNameAttribute) {
        this.groupNameAttribute = groupNameAttribute;
    }

    public String getGroupMemberAttribute() {
        return groupMemberAttribute;
    }

    public void setGroupMemberAttribute(String groupMemberAttribute) {
        this.groupMemberAttribute = groupMemberAttribute;
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

    public String getCertificate() {
        return certificate;
    }

    public void setCertificate(String certificate) {
        this.certificate = certificate;
    }

    public String getEnvironmentId() {
        return environmentId;
    }

    public void setEnvironmentId(String environmentId) {
        this.environmentId = environmentId;
    }
}
