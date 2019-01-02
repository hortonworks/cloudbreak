package com.sequenceiq.cloudbreak.api.endpoint.v4.kerberos.responses;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.responses.SecretV4Response;
import com.sequenceiq.cloudbreak.doc.ModelDescriptions.StackModelDescription;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(Include.NON_NULL)
public class KerberosV4Response extends KerberosV4ResponseBase {

    @ApiModelProperty(StackModelDescription.KERBEROS_ADMIN)
    private SecretV4Response admin;

    @ApiModelProperty(StackModelDescription.KERBEROS_PASSWORD)
    private SecretV4Response password;

    @ApiModelProperty(StackModelDescription.KERBEROS_PRINCIPAL)
    private SecretV4Response principal;

    @ApiModelProperty(StackModelDescription.DESCRIPTOR)
    private SecretV4Response descriptor;

    @ApiModelProperty(StackModelDescription.KRB_5_CONF)
    private SecretV4Response krb5Conf;

    public SecretV4Response getAdmin() {
        return admin;
    }

    public void setAdmin(SecretV4Response admin) {
        this.admin = admin;
    }

    public SecretV4Response getPassword() {
        return password;
    }

    public void setPassword(SecretV4Response password) {
        this.password = password;
    }

    public SecretV4Response getPrincipal() {
        return principal;
    }

    public void setPrincipal(SecretV4Response principal) {
        this.principal = principal;
    }

    public SecretV4Response getDescriptor() {
        return descriptor;
    }

    public void setDescriptor(SecretV4Response descriptor) {
        this.descriptor = descriptor;
    }

    public SecretV4Response getKrb5Conf() {
        return krb5Conf;
    }

    public void setKrb5Conf(SecretV4Response krb5Conf) {
        this.krb5Conf = krb5Conf;
    }
}
