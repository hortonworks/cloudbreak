package com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.upgrade.validation.handler;

import static com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.upgrade.validation.event.ClusterUpgradeValidationHandlerSelectors.VALIDATE_CREDENTIAL_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.upgrade.validation.event.ClusterUpgradeValidationHandlerSelectors.VALIDATE_DISK_SPACE_EVENT;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.upgrade.validation.event.ClusterUpgradeCredentialValidationFailed;
import com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.upgrade.validation.event.ClusterUpgradeCredentialValidationRequest;
import com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.upgrade.validation.event.ClusterUpgradeCredentialValidationSuccess;
import com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.upgrade.validation.event.ClusterUpgradeValidationEvent;
import com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.upgrade.validation.event.ClusterUpgradeValidationFinishedEvent;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.service.image.ImageCatalogService;
import com.sequenceiq.cloudbreak.service.stack.StackService;
import com.sequenceiq.cloudbreak.service.upgrade.validation.DiskSpaceValidationService;
import com.sequenceiq.environment.api.v1.credential.endpoint.CredentialEndpoint;
import com.sequenceiq.environment.api.v1.credential.model.response.CredentialResponse;
import com.sequenceiq.flow.reactor.api.handler.ExceptionCatcherEventHandler;
import com.sequenceiq.flow.reactor.api.handler.HandlerEvent;

import reactor.bus.Event;

@Component
public class ClusterUpgradeCredentialValidationHandler extends ExceptionCatcherEventHandler<ClusterUpgradeCredentialValidationRequest> {

    private static final Logger LOGGER = LoggerFactory.getLogger(ClusterUpgradeCredentialValidationHandler.class);

    @Inject
    private DiskSpaceValidationService diskSpaceValidationService;

    @Inject
    private ImageCatalogService imageCatalogService;

    @Inject
    private StackService stackService;

    @Inject
    private CredentialEndpoint credentialEndpoint;

    @Override
    protected Selectable doAccept(HandlerEvent<ClusterUpgradeCredentialValidationRequest> event) {
        LOGGER.debug("Accepting Cluster upgrade validation event.");
        ClusterUpgradeCredentialValidationRequest request = event.getData();
        Long stackId = request.getResourceId();
        Selectable result;
        try {
            credentialEndpoint.verifyByName(event.getData().getCloudCredential().getName());
            result = new ClusterUpgradeCredentialValidationSuccess(stackId, request.getCloudStack(), request.getCloudCredential(), request.getCloudContext());
        } catch (Exception ex) {
            LOGGER.warn("Cluster upgrade credential validation was unsuccessful due to an internal error", ex);
            result = new ClusterUpgradeCredentialValidationFailed(stackId, request.getCloudStack(), request.getCloudCredential(), request.getCloudContext(), ex);
        }
        return result;
    }

    @Override
    public String selector() {
        return VALIDATE_CREDENTIAL_EVENT.selector();
    }

    @Override
    protected Selectable defaultFailureEvent(Long resourceId, Exception e, Event<ClusterUpgradeCredentialValidationRequest> event) {
        return new ClusterUpgradeValidationFinishedEvent(resourceId, e);
    }
}
