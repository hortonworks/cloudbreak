package com.sequenceiq.cloudbreak.api.endpoint.v4.kerberos.requests;

import static com.sequenceiq.cloudbreak.doc.ModelDescriptions.StackModelDescription.DESCRIPTOR;
import static com.sequenceiq.cloudbreak.doc.ModelDescriptions.StackModelDescription.KRB_5_CONF;

import com.sequenceiq.cloudbreak.type.KerberosType;
import com.sequenceiq.cloudbreak.validation.ValidAmbariKerberosDescriptor;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel
@ValidAmbariKerberosDescriptor
public class AmbariKerberosDescriptor extends KerberosTypeBase {

    @ApiModelProperty(value = DESCRIPTOR, required = true)
    private String descriptor;

    @ApiModelProperty(value = KRB_5_CONF, required = true)
    private String krb5Conf;

    @ApiModelProperty(hidden = true)
    @Override
    public KerberosType getType() {
        return KerberosType.AMBARI_DESCRIPTOR;
    }

    public String getDescriptor() {
        return descriptor;
    }

    public void setDescriptor(String descriptor) {
        this.descriptor = descriptor;
    }

    public String getKrb5Conf() {
        return krb5Conf;
    }

    public void setKrb5Conf(String krb5Conf) {
        this.krb5Conf = krb5Conf;
    }

}
