package com.sequenceiq.cloudbreak.api.model;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import javax.validation.constraints.Min;
import javax.validation.constraints.Max;

import com.sequenceiq.cloudbreak.doc.ModelDescriptions;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel
public class LdapConfigBase implements JsonEntity {

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

    @ApiModelProperty(value = ModelDescriptions.LdapConfigModelDescription.IS_SERVER_SSL, required = true)
    private Boolean serverSSL;

    @ApiModelProperty(value = ModelDescriptions.LdapConfigModelDescription.BIND_DN)
    private String bindDn;

    @ApiModelProperty(value = ModelDescriptions.LdapConfigModelDescription.BIND_PASSWORD)
    private String bindPassword;

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

    public Boolean getServerSSL() {
        return serverSSL;
    }

    public void setServerSSL(Boolean serverSSL) {
        this.serverSSL = serverSSL;
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
}
