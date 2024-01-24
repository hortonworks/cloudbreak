package com.sequenceiq.cloudbreak.reactor.handler.cluster;

import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status.UPDATE_IN_PROGRESS;
import static com.sequenceiq.cloudbreak.event.ResourceEvent.CLUSTER_SCALING_STOPSTART_DOWNSCALE_IDENTIFYRECOVERYCANDIDATES;

import java.util.Collections;
import java.util.List;
import java.util.Set;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.CloudConnector;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.event.instance.StopStartDownscaleGetRecoveryCandidatesRequest;
import com.sequenceiq.cloudbreak.cloud.event.instance.StopStartDownscaleGetRecoveryCandidatesResult;
import com.sequenceiq.cloudbreak.cloud.handler.CloudPlatformEventHandler;
import com.sequenceiq.cloudbreak.cloud.init.CloudPlatformConnectors;
import com.sequenceiq.cloudbreak.cloud.model.CloudInstance;
import com.sequenceiq.cloudbreak.converter.spi.InstanceMetaDataToCloudInstanceConverter;
import com.sequenceiq.cloudbreak.core.flow2.stack.CloudbreakFlowMessageService;
import com.sequenceiq.cloudbreak.dto.StackDto;
import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.cloudbreak.eventbus.EventBus;
import com.sequenceiq.cloudbreak.service.stack.StackDtoService;
import com.sequenceiq.cloudbreak.service.stopstart.RecoveryCandidateCollectionService;
import com.sequenceiq.cloudbreak.view.InstanceMetadataView;

@Component
public class StopStartDownscaleGetRecoveryCandidatesHandler implements CloudPlatformEventHandler<StopStartDownscaleGetRecoveryCandidatesRequest> {

    private static final Logger LOGGER = LoggerFactory.getLogger(StopStartDownscaleGetRecoveryCandidatesHandler.class);

    @Inject
    private CloudPlatformConnectors cloudPlatformConnectors;

    @Inject
    private CloudbreakFlowMessageService flowMessageService;

    @Inject
    private StackDtoService stackDtoService;

    @Inject
    private RecoveryCandidateCollectionService recoveryCandidateCollectionService;

    @Inject
    private InstanceMetaDataToCloudInstanceConverter instanceMetaDataToCloudInstanceConverter;

    @Inject
    private EventBus eventBus;

    @Override
    public Class<StopStartDownscaleGetRecoveryCandidatesRequest> type() {
        return StopStartDownscaleGetRecoveryCandidatesRequest.class;
    }

    @Override
    public void accept(Event<StopStartDownscaleGetRecoveryCandidatesRequest> event) {
        StopStartDownscaleGetRecoveryCandidatesRequest request = event.getData();
        LOGGER.info("StopStartDownscaleGetRunningInstancesHandler: {}, with request: {}", request.getResourceId(), request);

        CloudContext cloudContext = request.getCloudContext();

        try {
            CloudConnector connector  = cloudPlatformConnectors.get(cloudContext.getPlatformVariant());
            AuthenticatedContext ac = getAuthenticatedContext(request, cloudContext, connector);

            StackDto stack = stackDtoService.getById(request.getResourceId());

            Set<String> startedInstanceIdsOnCloudProvider =
                    recoveryCandidateCollectionService.collectStartedInstancesFromCloudProvider(connector, ac, request.getAllInstancesInHostGroup());

            flowMessageService.fireEventAndLog(stack.getId(), UPDATE_IN_PROGRESS.name(), CLUSTER_SCALING_STOPSTART_DOWNSCALE_IDENTIFYRECOVERYCANDIDATES,
                    request.getHostGroupName());

            List<InstanceMetadataView> startedInstanceMetadataWithServicesNotRunning =
                    recoveryCandidateCollectionService.getStartedInstancesWithServicesNotRunning(stack, request.getHostGroupName(),
                            startedInstanceIdsOnCloudProvider, request.isFailureRecoveryEnabled());

            List<CloudInstance> instancesWithServicesNotRunning;

            if (startedInstanceMetadataWithServicesNotRunning.isEmpty()) {
                instancesWithServicesNotRunning = Collections.emptyList();
            } else {
                instancesWithServicesNotRunning = instanceMetaDataToCloudInstanceConverter.convert(startedInstanceMetadataWithServicesNotRunning,
                        stack.getStack());
                LOGGER.info("Collected {} downscale recovery candidates for hostGroup: {}", instancesWithServicesNotRunning.size(), request.getHostGroupName());
            }

            StopStartDownscaleGetRecoveryCandidatesResult result = new StopStartDownscaleGetRecoveryCandidatesResult(stack.getId(),
                    instancesWithServicesNotRunning, request.getHostGroupName(), request.getHostIds());
            notify(result, event);
        } catch (Exception e) {
            String errorMessage = String.format("Failed to collect downscale recovery candidates for stack: %s. " +
                    "Proceeding with subsequent flow.", request.getResourceId());
            LOGGER.warn(errorMessage, e);
            StopStartDownscaleGetRecoveryCandidatesResult result = new StopStartDownscaleGetRecoveryCandidatesResult(request.getResourceId(),
                    Collections.emptyList(), request.getHostGroupName(), request.getHostIds());
            notify(result, event);
        }
    }

    private AuthenticatedContext getAuthenticatedContext(StopStartDownscaleGetRecoveryCandidatesRequest request, CloudContext cloudContext,
            CloudConnector connector) {
        return connector.authentication().authenticate(cloudContext, request.getCloudCredential());
    }

    protected void notify(StopStartDownscaleGetRecoveryCandidatesResult result, Event<StopStartDownscaleGetRecoveryCandidatesRequest> event) {
        eventBus.notify(result.selector(), new Event<>(event.getHeaders(), result));
    }
}
