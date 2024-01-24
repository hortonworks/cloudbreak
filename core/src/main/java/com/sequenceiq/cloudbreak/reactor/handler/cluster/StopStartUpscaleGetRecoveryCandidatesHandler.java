package com.sequenceiq.cloudbreak.reactor.handler.cluster;

import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status.UPDATE_IN_PROGRESS;
import static com.sequenceiq.cloudbreak.event.ResourceEvent.CLUSTER_SCALING_STOPSTART_UPSCALE_IDENTIFYINGRECOVERYCANDIDATES;

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
import com.sequenceiq.cloudbreak.cloud.event.instance.StopStartUpscaleGetRecoveryCandidatesRequest;
import com.sequenceiq.cloudbreak.cloud.event.instance.StopStartUpscaleGetRecoveryCandidatesResult;
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
public class StopStartUpscaleGetRecoveryCandidatesHandler implements CloudPlatformEventHandler<StopStartUpscaleGetRecoveryCandidatesRequest> {

    private static final Logger LOGGER = LoggerFactory.getLogger(StopStartUpscaleGetRecoveryCandidatesHandler.class);

    @Inject
    private CloudbreakFlowMessageService flowMessageService;

    @Inject
    private CloudPlatformConnectors cloudPlatformConnectors;

    @Inject
    private StackDtoService stackDtoService;

    @Inject
    private RecoveryCandidateCollectionService recoveryCandidateCollectionService;

    @Inject
    private InstanceMetaDataToCloudInstanceConverter instanceMetaDataToCloudInstanceConverter;

    @Inject
    private EventBus eventBus;

    @Override
    public Class<StopStartUpscaleGetRecoveryCandidatesRequest> type() {
        return StopStartUpscaleGetRecoveryCandidatesRequest.class;
    }

    @Override
    public void accept(Event<StopStartUpscaleGetRecoveryCandidatesRequest> event) {
        StopStartUpscaleGetRecoveryCandidatesRequest request = event.getData();
        LOGGER.info("StopStartUpscaleGetRecoveryCandidatesHandler: {}", event.getData().getResourceId());

        CloudContext cloudContext = request.getCloudContext();
        try {
            CloudConnector connector = cloudPlatformConnectors.get(cloudContext.getPlatformVariant());
            AuthenticatedContext ac = getAuthenticatedContext(request, cloudContext, connector);

            StackDto stack = stackDtoService.getById(request.getResourceId());

            Set<String> startedInstanceIdsOnCloudProvider =
                    recoveryCandidateCollectionService.collectStartedInstancesFromCloudProvider(connector, ac, request.getAllInstancesInHostGroup());

            flowMessageService.fireEventAndLog(stack.getId(), UPDATE_IN_PROGRESS.name(), CLUSTER_SCALING_STOPSTART_UPSCALE_IDENTIFYINGRECOVERYCANDIDATES,
                    request.getHostGroupName());

            List<InstanceMetadataView> instanceMetadataWithServicesNotRunning =
                    recoveryCandidateCollectionService.getStartedInstancesWithServicesNotRunning(stack, request.getHostGroupName(),
                            startedInstanceIdsOnCloudProvider, request.isFailureRecoveryEnabled());

            List<CloudInstance> instancesWithServicesNotRunning;

            if (instanceMetadataWithServicesNotRunning.isEmpty()) {
                instancesWithServicesNotRunning = Collections.emptyList();
            } else {
                instancesWithServicesNotRunning = instanceMetaDataToCloudInstanceConverter.convert(instanceMetadataWithServicesNotRunning, stack.getStack());
                LOGGER.info("Collected {} upscale recovery candidates for hostGroup {}", instancesWithServicesNotRunning.size(),
                        request.getHostGroupName());
            }

            StopStartUpscaleGetRecoveryCandidatesResult result =
                    new StopStartUpscaleGetRecoveryCandidatesResult(request.getResourceId(), request, instancesWithServicesNotRunning,
                            request.getAdjustment(), request.getHostGroupName());
            notify(result, event);
        } catch (Exception e) {
            String errorMessage = String.format("Failed to collect upscale recovery candidates for stack: %s, proceeding with subsequent flow.",
                    request.getResourceId());
            LOGGER.warn(errorMessage, e);
            StopStartUpscaleGetRecoveryCandidatesResult result = new StopStartUpscaleGetRecoveryCandidatesResult(request.getResourceId(), request,
                    Collections.emptyList(), request.getAdjustment(), request.getHostGroupName());
            notify(result, event);
        }
    }

    private AuthenticatedContext getAuthenticatedContext(StopStartUpscaleGetRecoveryCandidatesRequest request, CloudContext cloudContext,
            CloudConnector connector) {
        return connector.authentication().authenticate(cloudContext, request.getCloudCredential());
    }

    protected void notify(StopStartUpscaleGetRecoveryCandidatesResult result, Event<StopStartUpscaleGetRecoveryCandidatesRequest> event) {
        eventBus.notify(result.selector(), new Event<>(event.getHeaders(), result));
    }
}

