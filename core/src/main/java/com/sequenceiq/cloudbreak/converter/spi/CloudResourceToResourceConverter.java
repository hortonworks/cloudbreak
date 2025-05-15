package com.sequenceiq.cloudbreak.converter.spi;

import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.common.json.Json;
import com.sequenceiq.cloudbreak.domain.Resource;

@Component
public class CloudResourceToResourceConverter {

    private static final Logger LOGGER = LoggerFactory.getLogger(CloudResourceToResourceConverter.class);

    public Resource convert(CloudResource source) {
        Resource domainResource = new Resource();
        domainResource.setResourceType(source.getType());
        domainResource.setResourceName(source.getName());
        domainResource.setResourceReference(source.getReference());
        domainResource.setResourceStatus(source.getStatus());
        domainResource.setInstanceGroup(source.getGroup());
        domainResource.setInstanceId(source.getInstanceId());
        domainResource.setAvailabilityZone(source.getAvailabilityZone());
        domainResource.setPrivateId(source.getPrivateId());
        Optional.ofNullable(source.getParameters().get(CloudResource.ATTRIBUTES)).ifPresent(attributes -> {
            try {
                Json attributesJson = new Json(attributes);
                domainResource.setAttributes(attributesJson);
            } catch (IllegalArgumentException e) {
                LOGGER.info("Failed to parse resource attributes. Attributes: [{}]", source.getStringParameter(CloudResource.ATTRIBUTES), e);
                throw new IllegalStateException("Cannot parse stored resource attributes");
            }
        });
        return domainResource;
    }
}
