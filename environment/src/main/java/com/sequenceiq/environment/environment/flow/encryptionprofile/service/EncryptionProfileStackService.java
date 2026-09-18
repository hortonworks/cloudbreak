package com.sequenceiq.environment.environment.flow.encryptionprofile.service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.dyngr.Polling;
import com.dyngr.exception.PollerStoppedException;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.StackViewV4Response;
import com.sequenceiq.cloudbreak.cloud.scheduler.PollGroup;
import com.sequenceiq.environment.environment.flow.DatalakeMultipleFlowsResultEvaluator;
import com.sequenceiq.environment.environment.flow.MultipleFlowsResultEvaluator;
import com.sequenceiq.environment.environment.flow.encryptionprofile.validator.EncryptionProfileClusterResolver;
import com.sequenceiq.environment.environment.poller.DatahubPollerProvider;
import com.sequenceiq.environment.environment.poller.SdxPollerProvider;
import com.sequenceiq.environment.environment.service.datahub.DatahubService;
import com.sequenceiq.environment.environment.service.sdx.SdxService;
import com.sequenceiq.environment.environment.service.stack.StackService;
import com.sequenceiq.environment.store.EnvironmentInMemoryStateStore;
import com.sequenceiq.flow.api.model.FlowIdentifier;

@Component
public class EncryptionProfileStackService {

    private static final Logger LOGGER = LoggerFactory.getLogger(EncryptionProfileStackService.class);

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

    public EncryptionProfileStackService(StackService stackService, SdxService sdxService, DatahubService datahubService,
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

    public List<StackViewV4Response> getClusters(String envCrn) {
        return stackService.getAllNotDeletedClustersByEnvironmentCrn(envCrn);
    }

    public TargetClusters collectClustersInheritingEnvironmentProfile(String envCrn) {
        return collectClustersInheritingEnvironmentProfile(getClusters(envCrn));
    }

    public TargetClusters collectClustersInheritingEnvironmentProfile(List<StackViewV4Response> clusters) {
        List<StackViewV4Response> datalakes = new ArrayList<>();
        List<StackViewV4Response> datahubs = new ArrayList<>();
        List<String> datalakeFailures = new ArrayList<>();
        List<String> datahubFailures = new ArrayList<>();
        for (StackViewV4Response stack : clusters) {
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

    public List<String> applyOnDatalakes(Long envId, TargetClusters targetClusters, EncryptionProfileOperation operation) {
        List<String> failures = new ArrayList<>(targetClusters.datalakeFailures());
        targetClusters.datalakes().forEach(datalake -> applyOnDatalake(envId, datalake, operation, failures));
        return failures;
    }

    public List<String> applyOnDatahubs(Long envId, TargetClusters targetClusters, EncryptionProfileOperation operation) {
        List<String> failures = new ArrayList<>(targetClusters.datahubFailures());
        Map<FlowIdentifier, StackViewV4Response> triggeredFlows = new LinkedHashMap<>();
        for (StackViewV4Response datahub : targetClusters.datahubs()) {
            try {
                LOGGER.info("Triggering {} encryption profile on datahub {}", operation.verb(), datahub.getName());
                triggeredFlows.put(triggerOnDatahub(datahub, operation), datahub);
            } catch (Exception e) {
                LOGGER.warn("Failed to trigger {} encryption profile on datahub {}", operation.verb(), datahub.getName(), e);
                failures.add(failure(DATAHUB, datahub.getName(), e.getMessage()));
            }
        }
        if (triggeredFlows.isEmpty()) {
            LOGGER.info("No datahub {} encryption profile flow to wait for", operation.verb());
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
            rethrowIfCancelled(envId, e, operation);
            LOGGER.warn("The {} encryption profile on datahubs timed out or error happened", operation.verb(), e);
            pollingError = pollingErrorMessage(e);
        }
        List<FlowIdentifier> failedFlows;
        try {
            failedFlows = datahubMultipleFlowsResultEvaluator.collectFailed(flowIdentifiers);
        } catch (Exception e) {
            LOGGER.warn("Failed to collect the result of datahub {} encryption profile flows", operation.verb(), e);
            failedFlows = flowIdentifiers;
            pollingError = pollingError != null ? pollingError : e.getMessage();
        }
        for (FlowIdentifier failedFlow : failedFlows) {
            StackViewV4Response datahub = triggeredFlows.get(failedFlow);
            if (datahub != null) {
                LOGGER.warn("The {} encryption profile flow failed on datahub {}, flow: {}", operation.verb(), datahub.getName(), failedFlow);
                String reason = Stream.of(getDatahubStatusReason(datahub), pollingError)
                        .filter(StringUtils::isNotBlank)
                        .collect(Collectors.joining("; "));
                failures.add(failure(DATAHUB, datahub.getName(), reason));
            }
        }
        return failures;
    }

    public List<String> skipDatahubs(TargetClusters targetClusters, EncryptionProfileOperation operation) {
        List<String> failures = new ArrayList<>(targetClusters.datahubFailures());
        if (!targetClusters.datahubs().isEmpty()) {
            List<String> datahubNames = targetClusters.datahubs().stream().map(StackViewV4Response::getName).toList();
            LOGGER.warn("Skipping {} encryption profile on datahubs {} because it failed on the Data Lake", operation.verb(), datahubNames);
            datahubNames.forEach(datahubName -> failures.add(failure(DATAHUB, datahubName, "skipped because the Data Lake update failed")));
        }
        return failures;
    }

    private void applyOnDatalake(Long envId, StackViewV4Response datalake, EncryptionProfileOperation operation, List<String> failures) {
        try {
            LOGGER.info("Triggering {} encryption profile on Data Lake {}", operation.verb(), datalake.getName());
            FlowIdentifier flowIdentifier = triggerOnDatalake(datalake, operation);
            List<FlowIdentifier> flowIdentifiers = List.of(flowIdentifier);
            Polling.stopAfterDelay(maxTime, TimeUnit.SECONDS)
                    .stopIfException(true)
                    .waitPeriodly(sleepTime, TimeUnit.SECONDS)
                    .run(sdxPollerProvider.flowListPoller(envId, flowIdentifiers));
            if (datalakeMultipleFlowsResultEvaluator.anyFailed(flowIdentifiers)) {
                LOGGER.warn("The {} encryption profile flow failed on Data Lake {}, flow: {}", operation.verb(), datalake.getName(), flowIdentifier);
                failures.add(failure(DATALAKE, datalake.getName(), getDatalakeStatusReason(datalake)));
            }
        } catch (Exception e) {
            rethrowIfCancelled(envId, e, operation);
            LOGGER.warn("The {} encryption profile on Data Lake {} failed or timed out", operation.verb(), datalake.getName(), e);
            failures.add(failure(DATALAKE, datalake.getName(), pollingErrorMessage(e)));
        }
    }

    private FlowIdentifier triggerOnDatalake(StackViewV4Response datalake, EncryptionProfileOperation operation) {
        return switch (operation) {
            case ENABLE -> {
                LOGGER.info("Environment-level encryption profile is inherited, no cluster-level CRN is forwarded to {}", datalake.getName());
                yield sdxService.enableEncryptionProfile(datalake.getCrn(), null);
            }
            case DISABLE -> sdxService.disableEncryptionProfile(datalake.getCrn());
        };
    }

    private FlowIdentifier triggerOnDatahub(StackViewV4Response datahub, EncryptionProfileOperation operation) {
        return switch (operation) {
            case ENABLE -> {
                LOGGER.info("Environment-level encryption profile is inherited, no cluster-level CRN is forwarded to {}", datahub.getName());
                yield datahubService.enableEncryptionProfile(datahub.getCrn(), null);
            }
            case DISABLE -> datahubService.disableEncryptionProfile(datahub.getCrn());
        };
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

    private void rethrowIfCancelled(Long envId, Exception e, EncryptionProfileOperation operation) {
        if (e instanceof PollerStoppedException pollerStoppedException && PollGroup.CANCELLED.equals(EnvironmentInMemoryStateStore.get(envId))) {
            LOGGER.info("The {} encryption profile on stacks was cancelled for environment {}", operation.verb(), envId);
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
}
