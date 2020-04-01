package com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.parameter.template;

import static com.sequenceiq.cloudbreak.util.NullUtil.doIfNotNull;

import java.util.Map;

import javax.validation.Valid;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.InstanceTemplateV4ParameterBase;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.cloudbreak.doc.ModelDescriptions.TemplateModelDescription;
import com.sequenceiq.common.api.type.EncryptionType;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(Include.NON_NULL)
public class AwsInstanceTemplateV4Parameters extends InstanceTemplateV4ParameterBase {

    @Valid
    @ApiModelProperty(TemplateModelDescription.AWS_SPOT_PARAMETERS)
    private AwsInstanceTemplateV4SpotParameters spot;

    @ApiModelProperty(TemplateModelDescription.ENCRYPTION)
    private AwsEncryptionV4Parameters encryption;

    public AwsInstanceTemplateV4SpotParameters getSpot() {
        return spot;
    }

    public void setSpot(AwsInstanceTemplateV4SpotParameters spot) {
        this.spot = spot;
    }

    public AwsEncryptionV4Parameters getEncryption() {
        return encryption;
    }

    public void setEncryption(AwsEncryptionV4Parameters encryption) {
        this.encryption = encryption;
    }

    @Override
    public Map<String, Object> asMap() {
        Map<String, Object> map = super.asMap();
        doIfNotNull(spot, sp -> putIfValueNotNull(map, "spotPercentage", sp.getPercentage()));
        if (encryption != null) {
            putIfValueNotNull(map, "type", encryption.getType());
            putIfValueNotNull(map, "encrypted", encryption.getType() != EncryptionType.NONE);
        }
        return map;
    }

    @Override
    @JsonIgnore
    @ApiModelProperty(hidden = true)
    public CloudPlatform getCloudPlatform() {
        return CloudPlatform.AWS;
    }

    @Override
    public Map<String, Object> asSecretMap() {
        Map<String, Object> secretMap = super.asSecretMap();
        if (encryption != null) {
            secretMap.put("key", encryption.getKey());
        }
        return secretMap;
    }

    @Override
    public void parse(Map<String, Object> parameters) {
        Integer spotPercentage = getInt(parameters, "spotPercentage");
        doIfNotNull(spotPercentage, sp -> {
            spot = new AwsInstanceTemplateV4SpotParameters();
            spot.setPercentage(sp);
        });
        AwsEncryptionV4Parameters encryption = new AwsEncryptionV4Parameters();
        encryption.setKey(getParameterOrNull(parameters, "key"));
        String type = getParameterOrNull(parameters, "type");
        if (type != null) {
            encryption.setType(EncryptionType.valueOf(type));
        }
    }
}
