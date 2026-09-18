package com.sequenceiq.environment.environment.flow.encryptionprofile.handler;

import static com.sequenceiq.environment.environment.flow.encryptionprofile.event.EnableEncryptionProfileStateSelectors.ENABLE_ENCRYPTION_PROFILE_ON_STACKS_HANDLER_EVENT;
import static com.sequenceiq.environment.environment.flow.encryptionprofile.event.EnableEncryptionProfileStateSelectors.FINISH_ENABLE_ENCRYPTION_PROFILE_EVENT;
import static com.sequenceiq.environment.environment.flow.encryptionprofile.service.EncryptionProfileOperation.ENABLE;

import java.util.List;

import org.apache.commons.collections4.ListUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.common.exception.CloudbreakServiceException;
import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.environment.environment.flow.encryptionprofile.event.EnableEncryptionProfileEvent;
import com.sequenceiq.environment.environment.flow.encryptionprofile.event.EnableEncryptionProfileFailedEvent;
import com.sequenceiq.environment.environment.flow.encryptionprofile.service.EncryptionProfileStackService;
import com.sequenceiq.environment.environment.flow.encryptionprofile.service.TargetClusters;
import com.sequenceiq.flow.reactor.api.handler.ExceptionCatcherEventHandler;
import com.sequenceiq.flow.reactor.api.handler.HandlerEvent;

@Component
public class EnableEncryptionProfileOnStacksHandler extends ExceptionCatcherEventHandler<EnableEncryptionProfileEvent> {

    private static final Logger LOGGER = LoggerFactory.getLogger(EnableEncryptionProfileOnStacksHandler.class);

    private final EncryptionProfileStackService encryptionProfileStackService;

    protected EnableEncryptionProfileOnStacksHandler(EncryptionProfileStackService encryptionProfileStackService) {
        this.encryptionProfileStackService = encryptionProfileStackService;
    }

    @Override
    public String selector() {
        return ENABLE_ENCRYPTION_PROFILE_ON_STACKS_HANDLER_EVENT.name();
    }

    @Override
    protected Selectable defaultFailureEvent(Long resourceId, Exception e, Event<EnableEncryptionProfileEvent> event) {
        return EnableEncryptionProfileFailedEvent.from(event.getData(), e);
    }

    @Override
    protected Selectable doAccept(HandlerEvent<EnableEncryptionProfileEvent> event) {
        EnableEncryptionProfileEvent payload = event.getData();
        Long envId = payload.getResourceId();
        String envCrn = payload.getResourceCrn();
        String envName = payload.getResourceName();
        LOGGER.info("Enabling encryption profile on stacks for environment {}", envName);
        TargetClusters targetClusters = encryptionProfileStackService.collectClustersInheritingEnvironmentProfile(envCrn);
        List<String> datalakeFailures = encryptionProfileStackService.applyOnDatalakes(envId, targetClusters, ENABLE);
        List<String> datahubFailures = datalakeFailures.isEmpty()
                ? encryptionProfileStackService.applyOnDatahubs(envId, targetClusters, ENABLE)
                : encryptionProfileStackService.skipDatahubs(targetClusters, ENABLE);
        List<String> failures = ListUtils.union(datalakeFailures, datahubFailures);
        if (!failures.isEmpty()) {
            String message = String.format("Failed to enable encryption profile on the following cluster(s): %s. "
                    + "The environment encryption profile was set; retry the operation or run disable encryption profile.", String.join(", ", failures));
            LOGGER.warn("Enable encryption profile on stacks for environment {} finished with failures: {}", envName, message);
            return EnableEncryptionProfileFailedEvent.from(payload, new CloudbreakServiceException(message));
        }
        LOGGER.info("Encryption profile enabled on all stacks for environment {}", envName);
        return EnableEncryptionProfileEvent.builder()
                .withSelector(FINISH_ENABLE_ENCRYPTION_PROFILE_EVENT.selector())
                .withResourceId(envId)
                .withResourceName(envName)
                .withResourceCrn(envCrn)
                .withEncryptionProfileCrn(payload.getEncryptionProfileCrn())
                .build();
    }
}
