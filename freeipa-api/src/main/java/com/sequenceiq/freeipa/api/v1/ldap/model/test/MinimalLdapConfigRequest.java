package com.sequenceiq.freeipa.api.v1.ldap.model.test;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import com.sequenceiq.freeipa.api.v1.ldap.doc.LdapConfigModelDescription;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "MinimalLdapConfigV1Request")
public class MinimalLdapConfigRequest {

    @NotNull
    @Schema(description = LdapConfigModelDescription.SERVER_HOST, required = true)
    private String host;

    @NotNull
    @Max(65535)
    @Min(1)
    @Schema(description = LdapConfigModelDescription.SERVER_PORT, required = true)
    private Integer port;

    @Schema(description = LdapConfigModelDescription.PROTOCOL)
    private String protocol = "ldap";

    @NotNull
    @Schema(description = LdapConfigModelDescription.BIND_DN, required = true)
    private String bindDn;

    @NotNull
    @Schema(description = LdapConfigModelDescription.BIND_PASSWORD, required = true)
    private String bindPassword;

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
}
