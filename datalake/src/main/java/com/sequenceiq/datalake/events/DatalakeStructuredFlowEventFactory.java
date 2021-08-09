package com.sequenceiq.datalake.events;

import static com.sequenceiq.cloudbreak.structuredevent.event.StructuredEventType.FLOW;

import java.util.UUID;

import javax.inject.Inject;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.common.service.Clock;
import com.sequenceiq.cloudbreak.structuredevent.event.CloudbreakEventService;
import com.sequenceiq.cloudbreak.structuredevent.event.FlowDetails;
import com.sequenceiq.cloudbreak.structuredevent.event.cdp.CDPOperationDetails;
import com.sequenceiq.cloudbreak.structuredevent.event.cdp.CDPStructuredFlowEvent;
import com.sequenceiq.cloudbreak.structuredevent.service.CDPStructuredFlowEventFactory;
import com.sequenceiq.datalake.entity.SdxCluster;
import com.sequenceiq.datalake.entity.SdxStatusEntity;
import com.sequenceiq.datalake.repository.SdxStatusRepository;
import com.sequenceiq.datalake.service.sdx.SdxService;
import com.sequenceiq.flow.ha.NodeConfig;

/**
 * This class lets the Datalake module handle Flow Structured Events.
 * <p>
 * This class primarily gathers data about an SDX cluster and the state of the CB flow to construct a Structured Flow Event in the
 * {@code  createStructuredFlowEvent} method.
 * <p>
 * This class shows up as a dependency at runtime, rather than during compilation.
 * We're including the {@literal :structuredevent-service-cdp} module as a dependency, it brings along a {@code CDPFlowStructuredEventHandler}.
 * The Event Handler has a Spring injected dependency on {@code CDPStructuredFlowEventFactor}, which is the interface of this class.
 */
@Component
public class DatalakeStructuredFlowEventFactory implements CDPStructuredFlowEventFactory {

    @Inject
    private Clock clock;

    @Inject
    private NodeConfig nodeConfig;

    @Inject
    private SdxService sdxService;

    @Inject
    private SdxClusterDtoConverter sdxClusterDtoConverter;

    @Inject
    private SdxStatusRepository sdxStatusRepository;

    @Value("${info.app.version:}")
    private String serviceVersion;

    @Override
    public CDPStructuredFlowEvent<SdxClusterDto> createStructuredFlowEvent(Long resourceId, FlowDetails flowDetails, Boolean detailed) {
        return createStructuredFlowEvent(resourceId, flowDetails, detailed, null);
    }

    @Override
    public CDPStructuredFlowEvent<SdxClusterDto> createStructuredFlowEvent(Long resourceId, FlowDetails flowDetails, Boolean detailed, Exception exception) {
        SdxCluster sdxCluster = sdxService.getById(resourceId);
        CDPOperationDetails operationDetails = makeCdpOperationDetails(resourceId, sdxCluster);
        SdxClusterDto sdxClusterDto = sdxClusterDtoConverter.sdxClusterToDto(sdxCluster);
        SdxStatusEntity sdxStatus = sdxStatusRepository.findFirstByDatalakeIsOrderByIdDesc(sdxCluster);
        String status = sdxStatus.getStatus().name();
        String statusReason = sdxStatus.getStatusReason();
        CDPStructuredFlowEvent<SdxClusterDto> event = new CDPStructuredFlowEvent<>(operationDetails, flowDetails, sdxClusterDto, status, statusReason);
        if (exception != null) {
            event.setException(ExceptionUtils.getStackTrace(exception));
        }
        return event;
    }

    private CDPOperationDetails makeCdpOperationDetails(Long resourceId, SdxCluster sdxCluster) {
        // turning the string constants in CloudbreakEventService into a enums would be nice.
        String resourceType = CloudbreakEventService.DATALAKE_RESOURCE_TYPE;

        // todo: make a CDPOperationDetails Builder
        CDPOperationDetails operationDetails = new CDPOperationDetails();
        operationDetails.setTimestamp(clock.getCurrentTimeMillis());
        operationDetails.setEventType(FLOW);
        operationDetails.setResourceId(resourceId);
        operationDetails.setResourceName(sdxCluster.getName());
        operationDetails.setResourceType(resourceType);
        operationDetails.setCloudbreakId(nodeConfig.getId());
        operationDetails.setCloudbreakVersion(serviceVersion);
        operationDetails.setResourceCrn(sdxCluster.getResourceCrn());
        operationDetails.setUserCrn(ThreadBasedUserCrnProvider.getUserCrn());
        operationDetails.setAccountId(sdxCluster.getAccountId());
        operationDetails.setEnvironmentCrn(sdxCluster.getEnvCrn());
        operationDetails.setUuid(UUID.randomUUID().toString());

        return operationDetails;
    }
}
