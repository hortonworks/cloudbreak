package com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.upgrade.validation.handler;

import static com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.upgrade.validation.event.ClusterUpgradeValidationHandlerSelectors.VALIDATE_CLOUDPROVIDER_UPDATE;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.CloudConnector;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.init.CloudPlatformConnectors;
import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.upgrade.validation.event.ClusterUpgradeUpdateCheckFailed;
import com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.upgrade.validation.event.ClusterUpgradeUpdateCheckFinishedEvent;
import com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.upgrade.validation.event.ClusterUpgradeUpdateCheckRequest;
import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.flow.reactor.api.handler.ExceptionCatcherEventHandler;
import com.sequenceiq.flow.reactor.api.handler.HandlerEvent;

@Component
public class CloudProviderUpdateCheckHandler extends ExceptionCatcherEventHandler<ClusterUpgradeUpdateCheckRequest> {
    private static final Logger LOGGER = LoggerFactory.getLogger(CloudProviderUpdateCheckHandler.class);

    @Inject
    private CloudPlatformConnectors cloudPlatformConnectors;

    @Override
    protected Selectable doAccept(HandlerEvent<ClusterUpgradeUpdateCheckRequest> event) {
        LOGGER.debug("Received event: {}", event);
        ClusterUpgradeUpdateCheckRequest request = event.getData();
        CloudContext cloudContext = request.getCloudContext();
        CloudConnector connector = cloudPlatformConnectors.get(cloudContext.getPlatform(), cloudContext.getVariant());
        try {
            AuthenticatedContext ac = connector.authentication().authenticate(cloudContext, request.getCloudCredential());
            connector.resources().checkUpdate(ac, request.getCloudStack(), request.getCloudResources());
            LOGGER.debug("Cloud provider update check has finished");
            return new ClusterUpgradeUpdateCheckFinishedEvent(request.getResourceId(), request.getCloudStack(), request.getCloudCredential(),
                    cloudContext);
        } catch (Exception e) {
            LOGGER.error("Cloud provider update check has failed", e);
            return new ClusterUpgradeUpdateCheckFailed(request.getResourceId(), request.getCloudStack(), request.getCloudCredential(),
                    cloudContext, e);
        }
    }

    @Override
    public String selector() {
        return VALIDATE_CLOUDPROVIDER_UPDATE.selector();
    }

    @Override
    protected Selectable defaultFailureEvent(Long resourceId, Exception e, Event<ClusterUpgradeUpdateCheckRequest> event) {
        ClusterUpgradeUpdateCheckRequest request = event.getData();
        return new ClusterUpgradeUpdateCheckFailed(resourceId, request.getCloudStack(), request.getCloudCredential(), request.getCloudContext(), e);
    }
}
