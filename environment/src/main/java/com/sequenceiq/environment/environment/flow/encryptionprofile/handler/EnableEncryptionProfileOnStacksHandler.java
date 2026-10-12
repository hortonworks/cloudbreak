package com.sequenceiq.environment.environment.flow.encryptionprofile.handler;

import static com.sequenceiq.environment.environment.flow.encryptionprofile.event.EnableEncryptionProfileStateSelectors.ENABLE_ENCRYPTION_PROFILE_ON_STACKS_HANDLER_EVENT;
import static com.sequenceiq.environment.environment.flow.encryptionprofile.event.EnableEncryptionProfileStateSelectors.FINISH_ENABLE_ENCRYPTION_PROFILE_EVENT;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.collections4.ListUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.dyngr.Polling;
import com.dyngr.exception.PollerStoppedException;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.StackViewV4Response;
import com.sequenceiq.cloudbreak.cloud.scheduler.PollGroup;
import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.common.exception.CloudbreakServiceException;
import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.environment.environment.flow.DatalakeMultipleFlowsResultEvaluator;
import com.sequenceiq.environment.environment.flow.MultipleFlowsResultEvaluator;
import com.sequenceiq.environment.environment.flow.encryptionprofile.event.EnableEncryptionProfileEvent;
import com.sequenceiq.environment.environment.flow.encryptionprofile.event.EnableEncryptionProfileFailedEvent;
import com.sequenceiq.environment.environment.flow.encryptionprofile.validator.EncryptionProfileClusterResolver;
import com.sequenceiq.environment.environment.poller.DatahubPollerProvider;
import com.sequenceiq.environment.environment.poller.SdxPollerProvider;
import com.sequenceiq.environment.environment.service.datahub.DatahubService;
import com.sequenceiq.environment.environment.service.sdx.SdxService;
import com.sequenceiq.environment.environment.service.stack.StackService;
import com.sequenceiq.environment.store.EnvironmentInMemoryStateStore;
import com.sequenceiq.flow.api.model.FlowIdentifier;
import com.sequenceiq.flow.reactor.api.handler.ExceptionCatcherEventHandler;
import com.sequenceiq.flow.reactor.api.handler.HandlerEvent;

@Component
public class EnableEncryptionProfileOnStacksHandler extends ExceptionCatcherEventHandler<EnableEncryptionProfileEvent> {

    private static final Logger LOGGER = LoggerFactory.getLogger(EnableEncryptionProfileOnStacksHandler.class);

    private static final String DATALAKE = "Data Lake";

    private static final String DATAHUB = "Data Hub";

    @Value("${env.stack.config.update.polling.maximum.seconds:7200}")
    private Integer maxTime;

    @Value("${env.stack.config.update.sleep.time.seconds:60}")
    private Integer sleepTime;

    private final StackService stackService;

    private final SdxService sdxService;

    private final DatahubService datahubService;

    private final EncryptionProfileClusterResolver encryptionProfileClusterResolver;

    private final SdxPollerProvider sdxPollerProvider;

    private final DatalakeMultipleFlowsResultEvaluator datalakeMultipleFlowsResultEvaluator;

    private final DatahubPollerProvider datahubPollerProvider;

    private final MultipleFlowsResultEvaluator datahubMultipleFlowsResultEvaluator;

    protected EnableEncryptionProfileOnStacksHandler(StackService stackService, SdxService sdxService, DatahubService datahubService,
            EncryptionProfileClusterResolver encryptionProfileClusterResolver, SdxPollerProvider sdxPollerProvider,
            DatalakeMultipleFlowsResultEvaluator datalakeMultipleFlowsResultEvaluator, DatahubPollerProvider datahubPollerProvider,
            MultipleFlowsResultEvaluator datahubMultipleFlowsResultEvaluator) {
        this.stackService = stackService;
        this.sdxService = sdxService;
        this.datahubService = datahubService;
        this.encryptionProfileClusterResolver = encryptionProfileClusterResolver;
        this.sdxPollerProvider = sdxPollerProvider;
        this.datalakeMultipleFlowsResultEvaluator = datalakeMultipleFlowsResultEvaluator;
        this.datahubPollerProvider = datahubPollerProvider;
        this.datahubMultipleFlowsResultEvaluator = datahubMultipleFlowsResultEvaluator;
    }

    @Override
    public String selector() {
        return ENABLE_ENCRYPTION_PROFILE_ON_STACKS_HANDLER_EVENT.name();
    }

    @Override
    protected Selectable defaultFailureEvent(Long resourceId, Exception e, Event<EnableEncryptionProfileEvent> event) {
        return new EnableEncryptionProfileFailedEvent(event.getData().getResourceId(), event.getData().getResourceName(),
                event.getData().getResourceCrn(), e);
    }

    @Override
    protected Selectable doAccept(HandlerEvent<EnableEncryptionProfileEvent> event) {
        EnableEncryptionProfileEvent payload = event.getData();
        Long envId = payload.getResourceId();
        String envCrn = payload.getResourceCrn();
        String envName = payload.getResourceName();
        LOGGER.info("Enabling encryption profile on stacks for environment {} (crn={})", envName, envCrn);
        TargetClusters targetClusters = collectClustersInheritingEnvironmentProfile(envCrn);
        List<String> datalakeFailures = enableOnDatalakes(envId, targetClusters);
        List<String> datahubFailures = datalakeFailures.isEmpty()
                ? enableOnDatahubs(envId, targetClusters)
                : skipDatahubs(targetClusters);
        List<String> failures = ListUtils.union(datalakeFailures, datahubFailures);
        if (!failures.isEmpty()) {
            String message = String.format("Failed to enable encryption profile on the following cluster(s): %s. "
                    + "The environment encryption profile was set; retry the operation or run disable encryption profile.", String.join(", ", failures));
            LOGGER.warn("Enable encryption profile on stacks for environment {} finished with failures: {}", envName, message);
            return new EnableEncryptionProfileFailedEvent(envId, envName, envCrn, new CloudbreakServiceException(message));
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

    private TargetClusters collectClustersInheritingEnvironmentProfile(String envCrn) {
        List<StackViewV4Response> datalakes = new ArrayList<>();
        List<StackViewV4Response> datahubs = new ArrayList<>();
        List<String> datalakeFailures = new ArrayList<>();
        List<String> datahubFailures = new ArrayList<>();
        for (StackViewV4Response stack : stackService.getAllNotDeletedClustersByEnvironmentCrn(envCrn)) {
            boolean datalake = encryptionProfileClusterResolver.isDatalake(stack);
            try {
                if (encryptionProfileClusterResolver.hasOwnEncryptionProfile(stack)) {
                    LOGGER.info("Skipping cluster {}, it has its own encryption profile", stack.getName());
                } else if (datalake) {
                    datalakes.add(stack);
                } else {
                    datahubs.add(stack);
                }
            } catch (Exception e) {
                LOGGER.warn("Failed to inspect encryption profile of cluster {}", stack.getName(), e);
                if (datalake) {
                    datalakeFailures.add(failure(DATALAKE, stack.getName(), e.getMessage()));
                } else {
                    datahubFailures.add(failure(DATAHUB, stack.getName(), e.getMessage()));
                }
            }
        }
        return new TargetClusters(datalakes, datahubs, datalakeFailures, datahubFailures);
    }

    private List<String> enableOnDatalakes(Long envId, TargetClusters targetClusters) {
        List<String> failures = new ArrayList<>(targetClusters.datalakeFailures());
        targetClusters.datalakes().forEach(datalake -> enableOnDatalake(envId, datalake, failures));
        return failures;
    }

    private List<String> skipDatahubs(TargetClusters targetClusters) {
        List<String> failures = new ArrayList<>(targetClusters.datahubFailures());
        if (!targetClusters.datahubs().isEmpty()) {
            List<String> datahubNames = targetClusters.datahubs().stream().map(StackViewV4Response::getName).toList();
            LOGGER.warn("Skipping enable encryption profile on datahubs {} because it failed on the Data Lake", datahubNames);
            datahubNames.forEach(datahubName -> failures.add(failure(DATAHUB, datahubName, "skipped because the Data Lake update failed")));
        }
        return failures;
    }

    private void enableOnDatalake(Long envId, StackViewV4Response datalake, List<String> failures) {
        try {
            LOGGER.info("Triggering enable encryption profile on Data Lake {} (env-level profile inherited, no cluster-level CRN forwarded)",
                    datalake.getName());
            FlowIdentifier flowIdentifier = sdxService.enableEncryptionProfile(datalake.getCrn(), null);
            List<FlowIdentifier> flowIdentifiers = List.of(flowIdentifier);
            Polling.stopAfterDelay(maxTime, TimeUnit.SECONDS)
                    .stopIfException(true)
                    .waitPeriodly(sleepTime, TimeUnit.SECONDS)
                    .run(sdxPollerProvider.flowListPoller(envId, flowIdentifiers));
            if (datalakeMultipleFlowsResultEvaluator.anyFailed(flowIdentifiers)) {
                LOGGER.warn("Enable encryption profile flow failed on Data Lake {}, flow: {}", datalake.getName(), flowIdentifier);
                failures.add(failure(DATALAKE, datalake.getName(), getDatalakeStatusReason(datalake)));
            }
        } catch (Exception e) {
            rethrowIfCancelled(envId, e);
            LOGGER.warn("Enable encryption profile on Data Lake {} failed or timed out", datalake.getName(), e);
            failures.add(failure(DATALAKE, datalake.getName(), pollingErrorMessage(e)));
        }
    }

    private List<String> enableOnDatahubs(Long envId, TargetClusters targetClusters) {
        List<String> failures = new ArrayList<>(targetClusters.datahubFailures());
        Map<FlowIdentifier, StackViewV4Response> triggeredFlows = new LinkedHashMap<>();
        for (StackViewV4Response datahub : targetClusters.datahubs()) {
            try {
                LOGGER.info("Triggering enable encryption profile on datahub {} (env-level profile inherited, no cluster-level CRN forwarded)",
                        datahub.getName());
                triggeredFlows.put(datahubService.enableEncryptionProfile(datahub.getCrn(), null), datahub);
            } catch (Exception e) {
                LOGGER.warn("Failed to trigger enable encryption profile on datahub {}", datahub.getName(), e);
                failures.add(failure(DATAHUB, datahub.getName(), e.getMessage()));
            }
        }
        if (triggeredFlows.isEmpty()) {
            LOGGER.info("No datahub enable encryption profile flow to wait for");
            return failures;
        }
        List<FlowIdentifier> flowIdentifiers = new ArrayList<>(triggeredFlows.keySet());
        String pollingError = null;
        try {
            Polling.stopAfterDelay(maxTime, TimeUnit.SECONDS)
                    .stopIfException(true)
                    .waitPeriodly(sleepTime, TimeUnit.SECONDS)
                    .run(datahubPollerProvider.multipleFlowsPoller(envId, flowIdentifiers));
        } catch (Exception e) {
            rethrowIfCancelled(envId, e);
            LOGGER.warn("Enable encryption profile on datahubs timed out or error happened", e);
            pollingError = pollingErrorMessage(e);
        }
        List<FlowIdentifier> failedFlows;
        try {
            failedFlows = datahubMultipleFlowsResultEvaluator.collectFailed(flowIdentifiers);
        } catch (Exception e) {
            LOGGER.warn("Failed to collect the result of datahub enable encryption profile flows", e);
            failedFlows = flowIdentifiers;
            pollingError = pollingError != null ? pollingError : e.getMessage();
        }
        for (FlowIdentifier failedFlow : failedFlows) {
            StackViewV4Response datahub = triggeredFlows.get(failedFlow);
            if (datahub != null) {
                LOGGER.warn("Enable encryption profile flow failed on datahub {}, flow: {}", datahub.getName(), failedFlow);
                String reason = Stream.of(getDatahubStatusReason(datahub), pollingError)
                        .filter(StringUtils::isNotBlank)
                        .collect(Collectors.joining("; "));
                failures.add(failure(DATAHUB, datahub.getName(), reason));
            }
        }
        return failures;
    }

    private String getDatalakeStatusReason(StackViewV4Response datalake) {
        try {
            return sdxService.getDetailByCrn(datalake.getCrn()).getStatusReason();
        } catch (Exception e) {
            LOGGER.warn("Failed to get status reason of Data Lake {}", datalake.getName(), e);
            return null;
        }
    }

    private String getDatahubStatusReason(StackViewV4Response datahub) {
        try {
            return datahubService.getByCrn(datahub.getCrn(), Set.of()).getStatusReason();
        } catch (Exception e) {
            LOGGER.warn("Failed to get status reason of datahub {}", datahub.getName(), e);
            return null;
        }
    }

    private void rethrowIfCancelled(Long envId, Exception e) {
        if (e instanceof PollerStoppedException pollerStoppedException && PollGroup.CANCELLED.equals(EnvironmentInMemoryStateStore.get(envId))) {
            LOGGER.info("Enable encryption profile on stacks was cancelled for environment {}", envId);
            throw pollerStoppedException;
        }
    }

    private String pollingErrorMessage(Exception e) {
        return e instanceof PollerStoppedException && e.getCause() == null
                ? String.format("polling timed out after %d seconds", maxTime)
                : e.getMessage();
    }

    private String failure(String clusterType, String clusterName, String reason) {
        return StringUtils.isBlank(reason)
                ? String.format("%s '%s'", clusterType, clusterName)
                : String.format("%s '%s' (%s)", clusterType, clusterName, reason);
    }

    private record TargetClusters(List<StackViewV4Response> datalakes, List<StackViewV4Response> datahubs, List<String> datalakeFailures,
            List<String> datahubFailures) {
    }
}
