package com.sequenceiq.cloudbreak.api.endpoint.v4.kerberos.responses;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.sequenceiq.cloudbreak.doc.ModelDescriptions.StackModelDescription;
import com.sequenceiq.secret.model.SecretResponse;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(Include.NON_NULL)
public class KerberosV4Response extends KerberosV4ResponseBase {

    @ApiModelProperty(StackModelDescription.KERBEROS_ADMIN)
    private SecretResponse admin;

    @ApiModelProperty(StackModelDescription.KERBEROS_PASSWORD)
    private SecretResponse password;

    @ApiModelProperty(StackModelDescription.KERBEROS_PRINCIPAL)
    private SecretResponse principal;

    @ApiModelProperty(StackModelDescription.DESCRIPTOR)
    private SecretResponse descriptor;

    @ApiModelProperty(StackModelDescription.KRB_5_CONF)
    private SecretResponse krb5Conf;

    public SecretResponse getAdmin() {
        return admin;
    }

    public void setAdmin(SecretResponse admin) {
        this.admin = admin;
    }

    public SecretResponse getPassword() {
        return password;
    }

    public void setPassword(SecretResponse password) {
        this.password = password;
    }

    public SecretResponse getPrincipal() {
        return principal;
    }

    public void setPrincipal(SecretResponse principal) {
        this.principal = principal;
    }

    public SecretResponse getDescriptor() {
        return descriptor;
    }

    public void setDescriptor(SecretResponse descriptor) {
        this.descriptor = descriptor;
    }

    public SecretResponse getKrb5Conf() {
        return krb5Conf;
    }

    public void setKrb5Conf(SecretResponse krb5Conf) {
        this.krb5Conf = krb5Conf;
    }
}
