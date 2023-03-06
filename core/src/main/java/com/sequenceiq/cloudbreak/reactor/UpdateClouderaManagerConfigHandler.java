package com.sequenceiq.cloudbreak.reactor;

import javax.inject.Inject;

import org.apache.commons.collections4.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.event.CloudPlatformResult;
import com.sequenceiq.cloudbreak.cloud.handler.CloudPlatformEventHandler;
import com.sequenceiq.cloudbreak.cloud.model.ResourceStatus;
import com.sequenceiq.cloudbreak.cluster.api.ClusterApi;
import com.sequenceiq.cloudbreak.dto.StackDto;
import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.cloudbreak.eventbus.EventBus;
import com.sequenceiq.cloudbreak.reactor.api.event.resource.CoreVerticalScaleResult;
import com.sequenceiq.cloudbreak.reactor.api.event.resource.UpdateClouderaManagerConfigRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.resource.UpdateClouderaManagerConfigResult;
import com.sequenceiq.cloudbreak.service.cluster.ClusterApiConnectors;
import com.sequenceiq.cloudbreak.service.stack.StackDtoService;

@Component
public class UpdateClouderaManagerConfigHandler implements CloudPlatformEventHandler<UpdateClouderaManagerConfigRequest> {

    private static final Logger LOGGER = LoggerFactory.getLogger(UpdateClouderaManagerConfigHandler.class);

    private static final String YARN_LOCAL_DIR = "yarn_nodemanager_local_dirs";

    private static final String YARN_LOG_DIR = "yarn_nodemanager_log_dirs";

    private static final String IMPALA_SCRATCH_DIR = "scratch_dirs";

    private static final String IMPALA_DATACACHE_DIR = "datacache_dirs";

    private static final String YARN_SERVICE = "YARN";

    private static final String IMPALA_SERVICE = "IMPALA";

    @Inject
    private ClusterApiConnectors clusterApiConnectors;

    @Inject
    private StackDtoService stackDtoService;

    @Inject
    private EventBus eventBus;

    @Override
    public void accept(Event<UpdateClouderaManagerConfigRequest> updateCMConfigRequestEvent) {
        LOGGER.debug("Received event: {}", updateCMConfigRequestEvent);
        UpdateClouderaManagerConfigRequest<UpdateClouderaManagerConfigResult> request = updateCMConfigRequestEvent.getData();
        try {
            StackDto stackDto = stackDtoService.getById(request.getStackVerticalScaleV4Request().getStackId());
            ClusterApi clusterApi = clusterApiConnectors.getConnector(stackDto);
            if (!CollectionUtils.isEmpty(request.getYarnMountPaths())) {
                LOGGER.debug("Updating service configs for Yarn services to paths: {}", request.getYarnMountPaths());
                clusterApi.clusterModificationService().updateServiceConfigAndRestartService(YARN_SERVICE,
                        YARN_LOG_DIR, String.join(",", request.getYarnMountPaths()));
                clusterApi.clusterModificationService().updateServiceConfigAndRestartService(YARN_SERVICE,
                        YARN_LOCAL_DIR, String.join(",", request.getYarnMountPaths()));
                LOGGER.debug("Updated service configs for Yarn.");
            }
            if (!CollectionUtils.isEmpty(request.getImpalaMountPaths())) {
                LOGGER.debug("Updating service configs for Impala services to paths: {}", request.getImpalaMountPaths());
                clusterApi.clusterModificationService().updateServiceConfigAndRestartService(IMPALA_SERVICE,
                        IMPALA_SCRATCH_DIR, String.join(",", request.getImpalaMountPaths()));
                clusterApi.clusterModificationService().updateServiceConfigAndRestartService(YARN_SERVICE,
                        IMPALA_DATACACHE_DIR, String.join(",", request.getImpalaMountPaths()));
                LOGGER.debug("Updated service configs for Impala.");
            }
            UpdateClouderaManagerConfigResult result = new UpdateClouderaManagerConfigResult(
                    request.getStackVerticalScaleV4Request().getStackId(),
                    request.getStackVerticalScaleV4Request());
            request.getResult().onNext(result);
            eventBus.notify(result.selector(), new Event<>(updateCMConfigRequestEvent.getHeaders(), result));
        } catch(Exception ex) {
            LOGGER.warn("Error updating YARN and IMPALA service configuration.", ex);
            UpdateClouderaManagerConfigResult result = new UpdateClouderaManagerConfigResult(
                    ex.getMessage(),
                    ex,
                    request.getStackVerticalScaleV4Request().getStackId(),
                    request.getStackVerticalScaleV4Request());
            request.getResult().onNext(result);
            eventBus.notify(CloudPlatformResult.failureSelector(UpdateClouderaManagerConfigResult.class),
                    new Event<>(updateCMConfigRequestEvent.getHeaders(), result));
        }
    }

    @Override
    public Class<UpdateClouderaManagerConfigRequest> type() {
        return UpdateClouderaManagerConfigRequest.class;
    }
}
