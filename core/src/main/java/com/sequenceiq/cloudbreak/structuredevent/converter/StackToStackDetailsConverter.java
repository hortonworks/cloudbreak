package com.sequenceiq.cloudbreak.structuredevent.converter;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.util.ConverterUtil;
import com.sequenceiq.cloudbreak.cloud.model.Image;
import com.sequenceiq.cloudbreak.converter.AbstractConversionServiceAwareConverter;
import com.sequenceiq.cloudbreak.core.CloudbreakImageNotFoundException;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.service.ComponentConfigProviderService;
import com.sequenceiq.cloudbreak.structuredevent.event.ImageDetails;
import com.sequenceiq.cloudbreak.structuredevent.event.InstanceGroupDetails;
import com.sequenceiq.cloudbreak.structuredevent.event.StackDetails;

@Component
public class StackToStackDetailsConverter extends AbstractConversionServiceAwareConverter<Stack, StackDetails> {
    private static final Logger LOGGER = LoggerFactory.getLogger(StackToStackDetailsConverter.class);

    @Inject
    private ComponentConfigProviderService componentConfigProviderService;

    @Inject
    private ConverterUtil converterUtil;

    @Override
    public StackDetails convert(Stack source) {
        StackDetails stackDetails = new StackDetails();
        stackDetails.setId(source.getId());
        stackDetails.setName(source.getName());
        stackDetails.setTunnel(source.getTunnel().name());
        stackDetails.setType(source.getType().name());
        stackDetails.setDatalakeResourceId(source.getDatalakeResourceId());
        stackDetails.setRegion(source.getRegion());
        stackDetails.setAvailabilityZone(source.getAvailabilityZone());
        stackDetails.setDescription(source.getDescription());
        stackDetails.setCloudPlatform(source.cloudPlatform());
        stackDetails.setStatus(source.getStatus().name());
        if (source.getStackStatus() != null && source.getStackStatus().getDetailedStackStatus() != null) {
            stackDetails.setDetailedStatus(source.getStackStatus().getDetailedStackStatus().name());
        }
        stackDetails.setStatusReason(source.getStatusReason());
        stackDetails.setInstanceGroups(converterUtil.convertAll(source.getInstanceGroups(), InstanceGroupDetails.class));
        stackDetails.setTags(source.getTags());
        convertComponents(stackDetails, source);
        return stackDetails;
    }

    private void convertComponents(StackDetails stackDetails, Stack stack) {
        Long stackId = stack.getId();
        try {
            Image image = componentConfigProviderService.getImage(stackId);
            stackDetails.setImage(converterUtil.convert(image, ImageDetails.class));
        } catch (CloudbreakImageNotFoundException e) {
            LOGGER.warn("Image not found! {}", e.getMessage());
        }
    }

}
