package com.sequenceiq.cloudbreak.common.imdupdate;

import java.util.Map;

import org.apache.commons.collections4.MapUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import com.sequenceiq.cloudbreak.common.exception.CloudbreakServiceException;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;

@Configuration
@ConfigurationProperties(prefix = "cb.imd.update")
public class InstanceMetadataUpdateProperties {

    private static final Logger LOGGER = LoggerFactory.getLogger(InstanceMetadataUpdateProperties.class);

    private Map<InstanceMetadataUpdateType, InstanceMetadataUpdateTypeProperty> types;

    public Map<InstanceMetadataUpdateType, InstanceMetadataUpdateTypeProperty> getTypes() {
        return MapUtils.emptyIfNull(types);
    }

    public void setTypes(Map<InstanceMetadataUpdateType, InstanceMetadataUpdateTypeProperty> types) {
        this.types = types;
    }

    public void validateUpdateType(InstanceMetadataUpdateType updateType, CloudPlatform cloudPlatform) {
        if (!types.containsKey(updateType) || !types.get(updateType).getSupportedPlatformsSet().contains(cloudPlatform)) {
            throw new CloudbreakServiceException(String.format("Instance metadata update type %s is not applicable for cloud platform %s.",
                    updateType, cloudPlatform));
        }
        Map<CloudPlatform, InstanceMetadataUpdateTypeMetadata> metadata = types.get(updateType).metadata();
        if (!metadata.containsKey(cloudPlatform)) {
            LOGGER.error("There is no information about update type {} in application config.", updateType);
            throw new CloudbreakServiceException(String.format("Instance metadata update type %s is not applicable for cloud platform %s.",
                    updateType, cloudPlatform));
        }
    }
}
