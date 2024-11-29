package com.sequenceiq.freeipa.api.v1.ldap.model;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.sequenceiq.cloudbreak.auth.crn.CrnResourceDescriptor;
import com.sequenceiq.cloudbreak.structuredevent.json.Base64Deserializer;
import com.sequenceiq.cloudbreak.structuredevent.json.Base64Serializer;
import com.sequenceiq.cloudbreak.validation.ValidCrn;
import com.sequenceiq.freeipa.api.v1.ldap.doc.LdapConfigModelDescription;
import com.sequenceiq.service.api.doc.ModelDescriptions;

import io.swagger.v3.oas.annotations.media.Schema;

public abstract class LdapConfigBase {
    @Pattern(regexp = "(^[a-z][-a-z0-9]*[a-z0-9]$)",
            message = "The name can only contain lowercase alphanumeric characters and hyphens and has start with an alphanumeric character")
    @Size(max = 100, min = 1, message = "The length of the ldap config's name has to be in range of 1 to 100")
    @Schema(description = ModelDescriptions.NAME, requiredMode = Schema.RequiredMode.REQUIRED)
    private String name;

    @Size(max = 1000, message = "The length of the ldap config's description has to be in range of 0 to 1000")
    @Schema(description = ModelDescriptions.DESCRIPTION)
    private String description;

    @NotNull
    @Schema(description = LdapConfigModelDescription.SERVER_HOST, requiredMode = Schema.RequiredMode.REQUIRED)
    private String host;

    @NotNull
    @Max(65535)
    @Min(1)
    @Schema(description = LdapConfigModelDescription.SERVER_PORT, requiredMode = Schema.RequiredMode.REQUIRED)
    private Integer port;

    @Schema(description = LdapConfigModelDescription.PROTOCOL)
    private String protocol = "ldap";

    @NotNull
    @Schema(description = LdapConfigModelDescription.USER_SEARCH_BASE, requiredMode = Schema.RequiredMode.REQUIRED)
    private String userSearchBase;

    @NotNull
    @Schema(description = LdapConfigModelDescription.USER_DN_PATTERN, requiredMode = Schema.RequiredMode.REQUIRED)
    private String userDnPattern;

    @Schema(description = LdapConfigModelDescription.GROUP_SEARCH_BASE)
    private String groupSearchBase;

    @Schema(description = LdapConfigModelDescription.USER_NAME_ATTRIBUTE)
    private String userNameAttribute;

    @Schema(description = LdapConfigModelDescription.DOMAIN)
    private String domain;

    @Schema(description = LdapConfigModelDescription.DIRECTORY_TYPE, allowableValues = "LDAP,ACTIVE_DIRECTORY")
    private DirectoryType directoryType = DirectoryType.ACTIVE_DIRECTORY;

    @Schema(description = LdapConfigModelDescription.USER_OBJECT_CLASS)
    private String userObjectClass;

    @Schema(description = LdapConfigModelDescription.GROUP_OBJECT_CLASS)
    private String groupObjectClass;

    @Schema(description = LdapConfigModelDescription.GROUP_ID_ATTRIBUTE)
    private String groupNameAttribute;

    @Schema(description = LdapConfigModelDescription.GROUP_MEMBER_ATTRIBUTE)
    private String groupMemberAttribute;

    @Schema(description = LdapConfigModelDescription.ADMIN_GROUP)
    private String adminGroup;

    @Schema(description = LdapConfigModelDescription.CERTIFICATE)
    @JsonSerialize(using = Base64Serializer.class)
    @JsonDeserialize(using = Base64Deserializer.class)
    private String certificate;

    @ValidCrn(resource = CrnResourceDescriptor.ENVIRONMENT)
    @NotEmpty
    @Schema(description = ModelDescriptions.ENVIRONMENT_CRN, requiredMode = Schema.RequiredMode.REQUIRED)
    private String environmentCrn;

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

    public String getEnvironmentCrn() {
        return environmentCrn;
    }

    public void setEnvironmentCrn(String environmentCrn) {
        this.environmentCrn = environmentCrn;
    }
}
