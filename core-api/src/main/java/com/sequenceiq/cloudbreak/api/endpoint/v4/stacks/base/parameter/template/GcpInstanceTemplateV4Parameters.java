package com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.parameter.template;

import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.mappable.CloudPlatform;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.EncryptionType;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.InstanceTemplateV4ParameterBase;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.KeyEncryptionMethod;
import com.sequenceiq.cloudbreak.doc.ModelDescriptions.TemplateModelDescription;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(Include.NON_NULL)
public class GcpInstanceTemplateV4Parameters extends InstanceTemplateV4ParameterBase {

    @ApiModelProperty(TemplateModelDescription.ENCRYPTION)
    private GcpEncryptionParametersV4 encryption;

    @ApiModelProperty
    private Boolean preemptible;

    public GcpEncryptionParametersV4 getEncryption() {
        return encryption;
    }

    public void setEncryption(GcpEncryptionParametersV4 encryption) {
        this.encryption = encryption;
    }

    public Boolean getPreemptible() {
        return preemptible;
    }

    public void setPreemptible(Boolean preemptible) {
        this.preemptible = preemptible;
    }

    @Override
    public Map<String, Object> asMap() {
        setPlatformType(CloudPlatform.GCP);
        Map<String, Object> map = super.asMap();
        map.put("key", encryption.getKey());
        map.put("keyEncryptionMethod", encryption.getKeyEncryptionMethod());
        map.put("type", encryption.getType());
        map.put("preemptible", preemptible);
        return map;
    }

    @Override
    public void parse(Map<String, Object> parameters) {
        setPlatformType(getPlatformType(parameters));
        GcpEncryptionParametersV4 encryption = new GcpEncryptionParametersV4();
        encryption.setKey(getParameterOrNull(parameters, "key"));
        String keyEncryptionMethod = getParameterOrNull(parameters, "keyEncryptionMethod");
        if (keyEncryptionMethod != null) {
            encryption.setKeyEncryptionMethod(KeyEncryptionMethod.valueOf(keyEncryptionMethod));
        }
        String type = getParameterOrNull(parameters, "type");
        if (type != null) {
            encryption.setType(EncryptionType.valueOf(type));
            this.encryption = encryption;
        }
        String preemptible = getParameterOrNull(parameters, "preemptible");
        if (preemptible != null) {
            this.preemptible = Boolean.valueOf(preemptible);
        }
    }
}
