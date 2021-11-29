package com.sequenceiq.freeipa.api.v1.freeipa.stack.model;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;

import com.sequenceiq.freeipa.api.v1.freeipa.stack.doc.FreeIpaModelDescriptions.FreeIpaServerSettingsModelDescriptions;

import io.swagger.annotations.ApiModelProperty;

public abstract class FreeIpaServerBase {

    public static final String DOMAIN_MATCHER = "(?:[a-z0-9](?:[a-z0-9-]{0,61}[a-z0-9])?\\.)+[a-z0-9][a-z0-9-]{0,61}[a-z0-9]";

    public static final String HOSTNAME_MATCHER = "^[a-z0-9][a-z0-9-]{0,61}[a-z0-9]$";

    @NotNull
    @Pattern(regexp = DOMAIN_MATCHER, message = "Invalid FreeIpa domain format")
    @ApiModelProperty(FreeIpaServerSettingsModelDescriptions.DOMAIN)
    private String domain;

    @NotNull
    @Pattern(regexp = HOSTNAME_MATCHER, message = "Invalid FreeIpa hostname format")
    @ApiModelProperty(FreeIpaServerSettingsModelDescriptions.HOSTNAME)
    private String hostname;

    @ApiModelProperty(FreeIpaServerSettingsModelDescriptions.ADMIN_GROUP_NAME)
    private String adminGroupName;

    public String getDomain() {
        return domain;
    }

    public void setDomain(String domain) {
        this.domain = domain;
    }

    public String getHostname() {
        return hostname;
    }

    public void setHostname(String hostname) {
        this.hostname = hostname;
    }

    public String getAdminGroupName() {
        return adminGroupName;
    }

    public void setAdminGroupName(String adminGroupName) {
        this.adminGroupName = adminGroupName;
    }

    @Override
    public String toString() {
        return "FreeIpaServerBase{"
                + "domain='" + domain + '\''
                + ", hostname='" + hostname + '\''
                + ", adminGroupName='" + adminGroupName + " '\'"
                + '}';
    }
}
