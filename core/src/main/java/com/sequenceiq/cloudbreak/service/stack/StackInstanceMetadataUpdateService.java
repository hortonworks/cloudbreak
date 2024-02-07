package com.sequenceiq.cloudbreak.service.stack;

import java.util.Map;

import jakarta.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.common.exception.BadRequestException;
import com.sequenceiq.cloudbreak.common.imdupdate.InstanceMetadataUpdateProperties;
import com.sequenceiq.cloudbreak.common.imdupdate.InstanceMetadataUpdateType;
import com.sequenceiq.cloudbreak.common.imdupdate.InstanceMetadataUpdateTypeProperty;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.cloudbreak.core.flow2.service.ReactorFlowManager;
import com.sequenceiq.cloudbreak.dto.StackDto;
import com.sequenceiq.flow.api.model.FlowIdentifier;

@Service
public class StackInstanceMetadataUpdateService {

    private static final Logger LOGGER = LoggerFactory.getLogger(StackInstanceMetadataUpdateService.class);

    @Inject
    private ReactorFlowManager flowManager;

    @Inject
    private StackDtoService stackService;

    @Inject
    private InstanceMetadataUpdateProperties instanceMetadataUpdateProperties;

    public FlowIdentifier updateInstanceMetadata(String stackCrn, InstanceMetadataUpdateType updateType) {
        StackDto stack = stackService.getByCrn(stackCrn);
        Map<InstanceMetadataUpdateType, InstanceMetadataUpdateTypeProperty> types = instanceMetadataUpdateProperties.getTypes();
        CloudPlatform cloudPlatform = CloudPlatform.valueOf(stack.getCloudPlatform());
        instanceMetadataUpdateProperties.validateUpdateType(updateType, cloudPlatform);
        if (StringUtils.equals(types.get(updateType).metadata().get(cloudPlatform).imdsVersion(), stack.getSupportedImdsVersion())) {
            throw new BadRequestException("The given update type is already executed for the stack, no need to do it again.");
        }
        return flowManager.triggerInstanceMetadataUpdate(stack, updateType);
    }
}
