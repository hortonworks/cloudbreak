package com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.parameter.template;

import static com.sequenceiq.cloudbreak.util.NullUtil.doIfNotNull;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import javax.validation.Valid;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.InstanceTemplateV4ParameterBase;
import com.sequenceiq.cloudbreak.cloud.model.InstanceTemplate;
import com.sequenceiq.cloudbreak.cloud.model.instance.AwsInstanceTemplate;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.cloudbreak.doc.ModelDescriptions.TemplateModelDescription;
import com.sequenceiq.common.api.placement.AwsPlacementGroupStrategy;
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

    @Valid
    @ApiModelProperty(TemplateModelDescription.AWS_PLACEMENT_GROUP)
    private AwsPlacementGroupV4Parameters placementGroup;

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

    public AwsPlacementGroupV4Parameters getPlacementGroup() {
        return placementGroup;
    }

    public void setPlacementGroup(AwsPlacementGroupV4Parameters placementGroup) {
        this.placementGroup = placementGroup;
    }

    @Override
    public Map<String, Object> asMap() {
        Map<String, Object> map = super.asMap();
        doIfNotNull(spot, sp -> {
            putIfValueNotNull(map, AwsInstanceTemplate.EC2_SPOT_PERCENTAGE, sp.getPercentage());
            putIfValueNotNull(map, AwsInstanceTemplate.EC2_SPOT_MAX_PRICE, sp.getMaxPrice());
        });
        if (encryption != null) {
            EncryptionType encryptionType = encryption.getType();
            putIfValueNotNull(map, InstanceTemplate.VOLUME_ENCRYPTION_KEY_TYPE, encryptionType);
            putIfValueNotNull(map, AwsInstanceTemplate.EBS_ENCRYPTION_ENABLED, encryptionType != null && encryptionType != EncryptionType.NONE);
        }

        putIfValueNotNull(map, AwsInstanceTemplate.PLACEMENT_GROUP_STRATEGY, Optional.ofNullable(placementGroup)
                .map(AwsPlacementGroupV4Parameters::getStrategy)
                .orElse(AwsPlacementGroupStrategy.NONE).name());
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
            putIfValueNotNull(secretMap, InstanceTemplate.VOLUME_ENCRYPTION_KEY_ID, encryption.getKey());
        }
        return secretMap;
    }

    @Override
    public void parse(Map<String, Object> parameters) {
        Integer spotPercentage = getInt(parameters, AwsInstanceTemplate.EC2_SPOT_PERCENTAGE);
        doIfNotNull(spotPercentage, sp -> {
            spot = new AwsInstanceTemplateV4SpotParameters();
            spot.setPercentage(sp);
        });
        Double spotMaxPrice = getDouble(parameters, AwsInstanceTemplate.EC2_SPOT_MAX_PRICE);
        doIfNotNull(spotMaxPrice, maxPrice -> {
            if (Objects.isNull(spot)) {
                spot = new AwsInstanceTemplateV4SpotParameters();
            }
            spot.setMaxPrice(maxPrice);
        });
        encryption = new AwsEncryptionV4Parameters();
        encryption.setKey(getParameterOrNull(parameters, InstanceTemplate.VOLUME_ENCRYPTION_KEY_ID));
        String type = getParameterOrNull(parameters, InstanceTemplate.VOLUME_ENCRYPTION_KEY_TYPE);
        if (type != null) {
            encryption.setType(EncryptionType.valueOf(type));
        }

        String placementGroupStrategy = getParameterOrNull(parameters, AwsInstanceTemplate.PLACEMENT_GROUP_STRATEGY);
        placementGroup = new AwsPlacementGroupV4Parameters();
        if (placementGroupStrategy != null) {
            placementGroup.setStrategy(AwsPlacementGroupStrategy.valueOf(placementGroupStrategy));
        } else {
            placementGroup.setStrategy(AwsPlacementGroupStrategy.NONE);
        }
    }

}
