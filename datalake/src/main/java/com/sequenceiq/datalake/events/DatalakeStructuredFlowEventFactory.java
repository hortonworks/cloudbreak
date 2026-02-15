package com.sequenceiq.datalake.events;

import static com.sequenceiq.cloudbreak.structuredevent.event.StructuredEventType.FLOW;

import java.util.Optional;
import java.util.UUID;

import jakarta.inject.Inject;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.StackV4Request;
import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.common.json.Json;
import com.sequenceiq.cloudbreak.common.json.JsonUtil;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.cloudbreak.common.service.Clock;
import com.sequenceiq.cloudbreak.ha.NodeConfig;
import com.sequenceiq.cloudbreak.structuredevent.event.CloudbreakEventService;
import com.sequenceiq.cloudbreak.structuredevent.event.DatabaseDetails;
import com.sequenceiq.cloudbreak.structuredevent.event.FlowDetails;
import com.sequenceiq.cloudbreak.structuredevent.event.cdp.CDPOperationDetails;
import com.sequenceiq.cloudbreak.structuredevent.event.cdp.CDPStructuredFlowEvent;
import com.sequenceiq.cloudbreak.structuredevent.event.cdp.datalake.CDPDatalakeStructuredFlowEvent;
import com.sequenceiq.cloudbreak.structuredevent.event.cdp.datalake.DatalakeDetails;
import com.sequenceiq.cloudbreak.structuredevent.service.CDPStructuredFlowEventFactory;
import com.sequenceiq.datalake.entity.SdxCluster;
import com.sequenceiq.datalake.entity.SdxDatabase;
import com.sequenceiq.datalake.entity.SdxStatusEntity;
import com.sequenceiq.datalake.repository.SdxStatusRepository;
import com.sequenceiq.datalake.service.sdx.SdxService;

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

    private static final Logger LOGGER = LoggerFactory.getLogger(DatalakeStructuredFlowEventFactory.class);

    @Inject
    private Clock clock;

    @Inject
    private NodeConfig nodeConfig;

    @Inject
    private SdxService sdxService;

    @Inject
    private SdxStatusRepository sdxStatusRepository;

    @Value("${info.app.version:}")
    private String serviceVersion;

    @Override
    public CDPStructuredFlowEvent createStructuredFlowEvent(Long resourceId, FlowDetails flowDetails) {
        return createStructuredFlowEvent(resourceId, flowDetails, null);
    }

    @Override
    public CDPStructuredFlowEvent createStructuredFlowEvent(Long resourceId, FlowDetails flowDetails, Exception exception) {
        SdxCluster sdxCluster = sdxService.getById(resourceId);
        CDPOperationDetails operationDetails = makeCdpOperationDetails(resourceId, sdxCluster);
        SdxStatusEntity sdxStatus = sdxStatusRepository.findFirstByDatalakeIsOrderByIdDesc(sdxCluster);
        String status = sdxStatus.getStatus().name();
        String statusReason = sdxStatus.getStatusReason();
        DatalakeDetails datalakeDetails = createDatalakeDetails(sdxCluster, sdxStatus);
        CDPDatalakeStructuredFlowEvent event = new CDPDatalakeStructuredFlowEvent(operationDetails, flowDetails, datalakeDetails, status, statusReason);
        if (exception != null) {
            event.setException(ExceptionUtils.getStackTrace(exception));
        }
        return event;
    }

    private DatalakeDetails createDatalakeDetails(SdxCluster sdxCluster, SdxStatusEntity sdxStatus) {
        DatalakeDetails datalakeDetails = new DatalakeDetails();
        SdxDatabase sdxDatabase = sdxCluster.getSdxDatabase();
        datalakeDetails.setDatabaseDetails(createDatabaseDetails(sdxDatabase));
        datalakeDetails.setRazEnabled(sdxCluster.isRangerRazEnabled());
        datalakeDetails.setMultiAzEnabled(sdxCluster.isEnableMultiAz());
        datalakeDetails.setStatus(sdxStatus.getStatus().name());
        datalakeDetails.setStatusReason(sdxStatus.getStatusReason());
        datalakeDetails.setSeLinux(sdxCluster.getSeLinux());
        datalakeDetails.setNotificationState(sdxCluster.getNotificationState());
        Optional<StackV4Request> stackV4Request = getStackV4Request(sdxCluster);
        stackV4Request.ifPresent(request -> {
            datalakeDetails.setCloudPlatform(getCloudPlatform(request));
        });
        return datalakeDetails;
    }

    private Optional<StackV4Request> getStackV4Request(SdxCluster sdxCluster) {
        try {
            if (sdxCluster.getStackRequest() != null) {
                return Optional.ofNullable(JsonUtil.readValue(sdxCluster.getStackRequest(), StackV4Request.class));
            }
        } catch (Exception e) {
            LOGGER.warn("Cannot read stack request for sdx cluster", e);
        }
        return Optional.empty();
    }

    private String getCloudPlatform(StackV4Request stackV4Request) {
        if (stackV4Request != null) {
            if (stackV4Request.getAws() != null) {
                return CloudPlatform.AWS.name();
            } else if (stackV4Request.getAzure() != null) {
                return CloudPlatform.AZURE.name();
            } else if (stackV4Request.getGcp() != null) {
                return CloudPlatform.GCP.name();
            } else if (stackV4Request.getYarn() != null) {
                return CloudPlatform.YARN.name();
            } else if (stackV4Request.getMock() != null) {
                return CloudPlatform.MOCK.name();
            }
        }
        return null;
    }

    private DatabaseDetails createDatabaseDetails(SdxDatabase sdxDatabase) {
        DatabaseDetails databaseDetails = new DatabaseDetails();
        databaseDetails.setAvailabilityType(sdxDatabase.getDatabaseAvailabilityType().name());
        databaseDetails.setAttributes(Optional.ofNullable(sdxDatabase.getAttributes()).map(Json::getValue).orElse(""));
        databaseDetails.setEngineVersion(sdxDatabase.getDatabaseEngineVersion());
        return databaseDetails;
    }

    private CDPOperationDetails makeCdpOperationDetails(Long resourceId, SdxCluster sdxCluster) {
        // turning the string constants in CloudbreakEventService into a enums would be nice.
        String resourceType = CloudbreakEventService.DATALAKE_RESOURCE_TYPE;

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
        operationDetails.setCreatorClient(sdxCluster.getCreatorClient());
        return operationDetails;
    }
}
