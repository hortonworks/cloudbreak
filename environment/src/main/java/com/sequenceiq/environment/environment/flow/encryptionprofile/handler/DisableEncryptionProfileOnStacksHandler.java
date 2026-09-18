package com.sequenceiq.environment.environment.flow.encryptionprofile.handler;

import static com.sequenceiq.environment.environment.flow.encryptionprofile.event.DisableEncryptionProfileStateSelectors.DISABLE_ENCRYPTION_PROFILE_ON_STACKS_HANDLER_EVENT;
import static com.sequenceiq.environment.environment.flow.encryptionprofile.event.DisableEncryptionProfileStateSelectors.FINISH_DISABLE_ENCRYPTION_PROFILE_EVENT;
import static com.sequenceiq.environment.environment.flow.encryptionprofile.service.EncryptionProfileOperation.DISABLE;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.commons.collections4.ListUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.StackViewV4Response;
import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.common.exception.CloudbreakServiceException;
import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.environment.environment.flow.encryptionprofile.event.DisableEncryptionProfileEvent;
import com.sequenceiq.environment.environment.flow.encryptionprofile.event.DisableEncryptionProfileFailedEvent;
import com.sequenceiq.environment.environment.flow.encryptionprofile.service.EncryptionProfileStackService;
import com.sequenceiq.environment.environment.flow.encryptionprofile.service.TargetClusters;
import com.sequenceiq.flow.reactor.api.handler.ExceptionCatcherEventHandler;
import com.sequenceiq.flow.reactor.api.handler.HandlerEvent;

@Component
public class DisableEncryptionProfileOnStacksHandler extends ExceptionCatcherEventHandler<DisableEncryptionProfileEvent> {

    private static final Logger LOGGER = LoggerFactory.getLogger(DisableEncryptionProfileOnStacksHandler.class);

    private final EncryptionProfileStackService encryptionProfileStackService;

    protected DisableEncryptionProfileOnStacksHandler(EncryptionProfileStackService encryptionProfileStackService) {
        this.encryptionProfileStackService = encryptionProfileStackService;
    }

    @Override
    public String selector() {
        return DISABLE_ENCRYPTION_PROFILE_ON_STACKS_HANDLER_EVENT.name();
    }

    @Override
    protected Selectable defaultFailureEvent(Long resourceId, Exception e, Event<DisableEncryptionProfileEvent> event) {
        return DisableEncryptionProfileFailedEvent.from(event.getData(), e);
    }

    @Override
    protected Selectable doAccept(HandlerEvent<DisableEncryptionProfileEvent> event) {
        DisableEncryptionProfileEvent payload = event.getData();
        Long envId = payload.getResourceId();
        String envCrn = payload.getResourceCrn();
        String envName = payload.getResourceName();
        LOGGER.info("Disabling encryption profile on stacks for environment {}", envName);
        Map<Boolean, List<StackViewV4Response>> clustersByEligibility = encryptionProfileStackService.getClusters(envCrn).stream()
                .collect(Collectors.partitioningBy(this::isEligible));
        List<String> skippedClusters = clustersByEligibility.get(false).stream().map(StackViewV4Response::getName).toList();
        if (!skippedClusters.isEmpty()) {
            LOGGER.warn("Skipping cluster(s) {} because their stack or cluster is not in {} status, their configuration will not be updated",
                    skippedClusters, Status.getEncryptionProfileDisableAllowedStatuses());
        }
        TargetClusters targetClusters = encryptionProfileStackService.collectClustersInheritingEnvironmentProfile(clustersByEligibility.get(true));
        List<String> failures = ListUtils.union(
                encryptionProfileStackService.applyOnDatalakes(envId, targetClusters, DISABLE),
                encryptionProfileStackService.applyOnDatahubs(envId, targetClusters, DISABLE));
        if (!failures.isEmpty()) {
            String message = failureMessage(failures, skippedClusters);
            LOGGER.warn("Disable encryption profile on stacks for environment {} finished with failures: {}", envName, message);
            return DisableEncryptionProfileFailedEvent.from(payload, new CloudbreakServiceException(message));
        }
        LOGGER.info("Encryption profile disabled on all eligible stacks for environment {}", envName);
        return DisableEncryptionProfileEvent.builder()
                .withSelector(FINISH_DISABLE_ENCRYPTION_PROFILE_EVENT.selector())
                .withResourceId(envId)
                .withResourceName(envName)
                .withResourceCrn(envCrn)
                .withSkippedClusters(skippedClusters)
                .build();
    }

    private String failureMessage(List<String> failures, List<String> skippedClusters) {
        StringBuilder message = new StringBuilder(String.format("Failed to disable encryption profile on the following cluster(s): %s. ",
                String.join(", ", failures)));
        if (!skippedClusters.isEmpty()) {
            message.append(String.format("The following cluster(s) were skipped because they were not in AVAILABLE or UPDATE_FAILED state: %s. ",
                    String.join(", ", skippedClusters)));
        }
        return message.append("The encryption profile was removed from the environment; retry the operation.").toString();
    }

    private boolean isEligible(StackViewV4Response stack) {
        return isDisableAllowed(stack.getStatus())
                && stack.getCluster() != null
                && isDisableAllowed(stack.getCluster().getStatus());
    }

    private boolean isDisableAllowed(Status status) {
        return status != null && status.isEncryptionProfileDisableAllowed();
    }
}
