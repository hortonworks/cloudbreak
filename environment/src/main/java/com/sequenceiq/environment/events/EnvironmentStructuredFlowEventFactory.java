package com.sequenceiq.environment.events;

import static com.sequenceiq.cloudbreak.structuredevent.event.StructuredEventType.FLOW;

import jakarta.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.common.service.Clock;
import com.sequenceiq.cloudbreak.ha.NodeConfig;
import com.sequenceiq.cloudbreak.structuredevent.event.CloudbreakEventService;
import com.sequenceiq.cloudbreak.structuredevent.event.FlowDetails;
import com.sequenceiq.cloudbreak.structuredevent.event.cdp.CDPOperationDetails;
import com.sequenceiq.cloudbreak.structuredevent.event.cdp.CDPStructuredFlowEvent;
import com.sequenceiq.cloudbreak.structuredevent.event.cdp.environment.CDPEnvironmentStructuredFlowEvent;
import com.sequenceiq.cloudbreak.structuredevent.event.cdp.environment.EnvironmentDetails;
import com.sequenceiq.cloudbreak.structuredevent.service.CDPStructuredFlowEventFactory;
import com.sequenceiq.environment.environment.EnvironmentStatus;
import com.sequenceiq.environment.environment.domain.Environment;
import com.sequenceiq.environment.environment.dto.EnvironmentDtoConverter;
import com.sequenceiq.environment.environment.service.EnvironmentService;

@Component
public class EnvironmentStructuredFlowEventFactory implements CDPStructuredFlowEventFactory {

    @Inject
    private Clock clock;

    @Inject
    private EnvironmentService environmentService;

    @Inject
    private EnvironmentDtoConverter environmentDtoConverter;

    @Inject
    private NodeConfig nodeConfig;

    @Value("${info.app.version:}")
    private String serviceVersion;

    @Override
    public CDPStructuredFlowEvent createStructuredFlowEvent(Long resourceId, FlowDetails flowDetails) {
        return createStructuredFlowEvent(resourceId, flowDetails, null);
    }

    @Override
    public CDPStructuredFlowEvent createStructuredFlowEvent(Long resourceId, FlowDetails flowDetails, Exception exception) {
        Environment environment = environmentService.findEnvironmentByIdOrThrow(resourceId);
        String resourceType = CloudbreakEventService.ENVIRONMENT_RESOURCE_TYPE;
        CDPOperationDetails operationDetails = getOperationDetails(resourceType, environment);

        EnvironmentDetails environmentDetails = environmentDtoConverter.environmentToDto(environment);

        CDPEnvironmentStructuredFlowEvent event = new CDPEnvironmentStructuredFlowEvent(operationDetails, flowDetails, environmentDetails,
                environment.getStatus().name(), getReason(environment));
        if (exception != null) {
            event.setException(ExceptionUtils.getStackTrace(exception));
        }
        return event;
    }

    private CDPOperationDetails getOperationDetails(String resourceType, Environment environment) {
        return new CDPOperationDetails(
                clock.getCurrentTimeMillis(),
                FLOW,
                resourceType,
                environment.getId(),
                environment.getName(),
                nodeConfig.getId(),
                serviceVersion,
                environment.getAccountId(),
                environment.getResourceCrn(),
                ThreadBasedUserCrnProvider.getUserCrn(),
                environment.getResourceCrn(),
                null);
    }

    private String getReason(Environment environment) {
        EnvironmentStatus status = environment.getStatus();
        String reason = environment.getStatusReason();
        if (StringUtils.isEmpty(reason)) {
            reason = status.getResponseStatus().getDescription();
        }
        return reason;
    }
}
