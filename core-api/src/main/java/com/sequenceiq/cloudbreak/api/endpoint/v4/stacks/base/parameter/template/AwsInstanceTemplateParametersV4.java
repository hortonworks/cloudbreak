package com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.parameter.template;

import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.mappable.CloudPlatform;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.EncryptionType;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.InstanceTemplateParameterV4Base;
import com.sequenceiq.cloudbreak.doc.ModelDescriptions.TemplateModelDescription;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(Include.NON_NULL)
public class AwsInstanceTemplateParametersV4 extends InstanceTemplateParameterV4Base {

    @ApiModelProperty(TemplateModelDescription.AWS_SPOT_PRICE)
    private Double spotPrice;

    @ApiModelProperty(TemplateModelDescription.ENCRYPTION)
    private AwsEncryptionParametersV4 encryption;

    public AwsEncryptionParametersV4 getEncryption() {
        return encryption;
    }

    public void setEncryption(AwsEncryptionParametersV4 encryption) {
        this.encryption = encryption;
    }

    public Double getSpotPrice() {
        return spotPrice;
    }

    public void setSpotPrice(Double spotPrice) {
        this.spotPrice = spotPrice;
    }

    @Override
    public Map<String, Object> asMap() {
        setPlatformType(CloudPlatform.AWS);
        Map<String, Object> map = super.asMap();
        map.put("spotPrice", spotPrice);
        if (encryption != null) {
            map.put("key", encryption.getKey());
            map.put("type", encryption.getType());
            map.put("encrypted", true);
        }
        return map;
    }

    @Override
    public void parse(Map<String, Object> parameters) {
        String spotPrice = getParameterOrNull(parameters, "spotPrice");
        if (spotPrice != null) {
            this.spotPrice = Double.parseDouble(spotPrice);
        }
        AwsEncryptionParametersV4 encription = new AwsEncryptionParametersV4();
        encription.setKey(getParameterOrNull(parameters, "key"));
        String type = getParameterOrNull(parameters, "type");
        if (type != null) {
            encription.setType(EncryptionType.valueOf(type));
        }
        setPlatformType(getPlatformType(parameters));
    }
}
