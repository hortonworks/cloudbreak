package com.sequenceiq.cloudbreak.service.diagnostics;

import java.time.Duration;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import jakarta.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status;
import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.cloudbreak.auth.crn.Crn;
import com.sequenceiq.cloudbreak.common.service.Clock;
import com.sequenceiq.cloudbreak.core.flow2.stack.CloudbreakFlowMessageService;
import com.sequenceiq.cloudbreak.event.ResourceEvent;
import com.sequenceiq.cloudbreak.service.ComponentConfigProviderService;
import com.sequenceiq.cloudbreak.service.stack.StackDtoService;
import com.sequenceiq.cloudbreak.view.StackView;
import com.sequenceiq.common.api.telemetry.model.Logging;
import com.sequenceiq.common.api.telemetry.model.Telemetry;
import com.sequenceiq.flow.domain.FlowLog;
import com.sequenceiq.flow.domain.FlowLogWithoutPayload;
import com.sequenceiq.flow.service.flowlog.FlowLogDBService;

/**
 * Fires a single user-facing hint event after a runtime upgrade or repair failure. The event carries a ready-made CDP CLI command the user may run to
 * collect Cloudera Manager diagnostics, scoped to the time window of the failed operation. Nothing is collected automatically; the command is only
 * suggested. The method never throws so the failure handler that calls it is unaffected.
 *
 * <p>The failed CM role names are not propagated to core yet (see CB-35612); callers pass an empty role collection for now and the {@code --roles} flag
 * is added only when roles are present.</p>
 *
 * <p>The start of the window is derived from the latest flow chain of the resource. For nested flow chains only the innermost (most recent) chain is
 * considered. The window is capped to {@link #MAX_LOOKBACK} so a long-running chain cannot produce an unbounded collection.</p>
 *
 * <p>The destination is chosen from the resource telemetry: when cloud storage logging (S3, ABFS or GCS) is configured the command targets
 * {@code CLOUD_STORAGE}, otherwise it falls back to {@code SUPPORT} with a {@code --ticket} placeholder for the support case number. No hint is fired
 * when telemetry is not configured.</p>
 */
@Service
public class CmDiagnosticsCommandHintService {

    private static final Logger LOGGER = LoggerFactory.getLogger(CmDiagnosticsCommandHintService.class);

    private static final String DATAHUB_COMMAND = "cdp datahub collect-cm-diagnostics";

    private static final String DATALAKE_COMMAND = "cdp datalake collect-cm-diagnostics";

    private static final String CLOUD_STORAGE_DESTINATION = "CLOUD_STORAGE";

    private static final String SUPPORT_DESTINATION = "SUPPORT";

    private static final String SUPPORT_CASE_PLACEHOLDER = "<support-case-number>";

    // margin subtracted from the detected operation start so diagnostics cover the moments leading up to the failure
    private static final Duration START_TIME_MARGIN = Duration.ofMinutes(15);

    // upper bound on the window: caps a long chain and is the fallback start when the operation start cannot be determined
    private static final Duration MAX_LOOKBACK = Duration.ofHours(6);

    @Inject
    private StackDtoService stackDtoService;

    @Inject
    private EntitlementService entitlementService;

    @Inject
    private FlowLogDBService flowLogDBService;

    @Inject
    private CloudbreakFlowMessageService flowMessageService;

    @Inject
    private ComponentConfigProviderService componentConfigProviderService;

    @Inject
    private Clock clock;

    public void fireCmDiagnosticsHint(Long stackId, Collection<String> roles) {
        try {
            StackView stack = stackDtoService.getStackViewById(stackId);
            String accountId = Crn.safeFromString(stack.getResourceCrn()).getAccountId();
            if (!entitlementService.isDiagnosticsEnabled(accountId)) {
                LOGGER.debug("Skipping CM diagnostics hint for stack {} because diagnostics is not entitled for account {}", stackId, accountId);
                return;
            }
            String baseCommand = resolveBaseCommand(stack);
            if (baseCommand == null) {
                LOGGER.debug("Skipping CM diagnostics hint for stack {} because its type {} is not supported", stackId, stack.getType());
                return;
            }
            Telemetry telemetry = componentConfigProviderService.getTelemetry(stackId);
            if (telemetry == null) {
                LOGGER.debug("Skipping CM diagnostics hint for stack {} because telemetry is not configured", stackId);
                return;
            }
            String command = buildCommand(baseCommand, stack.getResourceCrn(), stackId, roles, telemetry);
            flowMessageService.fireEventAndLog(stackId, Status.UPDATE_FAILED.name(), ResourceEvent.CLUSTER_CM_DIAGNOSTICS_COLLECTION_HINT, command);
        } catch (RuntimeException e) {
            LOGGER.warn("Could not fire CM diagnostics hint for stack {}", stackId, e);
        }
    }

    private String resolveBaseCommand(StackView stack) {
        if (stack.isDatahub()) {
            return DATAHUB_COMMAND;
        } else if (stack.isDatalake()) {
            return DATALAKE_COMMAND;
        }
        return null;
    }

    private String buildCommand(String baseCommand, String stackCrn, Long stackId, Collection<String> roles, Telemetry telemetry) {
        Instant endTime = clock.getCurrentInstant().truncatedTo(ChronoUnit.SECONDS);
        Instant startTime = resolveStartTime(stackId, endTime);
        StringBuilder command = new StringBuilder(baseCommand)
                .append(" --stack-crn ").append(stackCrn);
        if (hasCloudStorageLogging(telemetry)) {
            command.append(" --destination ").append(CLOUD_STORAGE_DESTINATION);
        } else {
            command.append(" --destination ").append(SUPPORT_DESTINATION)
                    .append(" --ticket ").append(SUPPORT_CASE_PLACEHOLDER);
        }
        command.append(" --start-time ").append(DateTimeFormatter.ISO_INSTANT.format(startTime))
                .append(" --end-time ").append(DateTimeFormatter.ISO_INSTANT.format(endTime));
        if (roles != null && !roles.isEmpty()) {
            command.append(" --roles ").append(String.join(" ", roles));
        }
        return command.toString();
    }

    private boolean hasCloudStorageLogging(Telemetry telemetry) {
        Logging logging = telemetry.getLogging();
        return logging != null && (logging.getS3() != null || logging.getAdlsGen2() != null || logging.getGcs() != null);
    }

    private Instant resolveStartTime(Long stackId, Instant endTime) {
        Instant maxLookbackStart = endTime.minus(MAX_LOOKBACK);
        return findChainStartMillis(stackId)
                .map(created -> Instant.ofEpochMilli(created).minus(START_TIME_MARGIN).truncatedTo(ChronoUnit.SECONDS))
                // cap the window: never reach further back than now minus MAX_LOOKBACK
                .map(chainStart -> chainStart.isAfter(maxLookbackStart) ? chainStart : maxLookbackStart)
                .orElse(maxLookbackStart);
    }

    private Optional<Long> findChainStartMillis(Long stackId) {
        Optional<FlowLog> lastFlowLog = flowLogDBService.getLastFlowLog(stackId);
        if (lastFlowLog.isEmpty()) {
            return Optional.empty();
        }
        String flowChainId = lastFlowLog.get().getFlowChainId();
        List<FlowLogWithoutPayload> chainLogs = StringUtils.isNotBlank(flowChainId)
                ? flowLogDBService.getFlowLogsWithoutPayloadByFlowChainIdsCreatedDesc(Set.of(flowChainId))
                : flowLogDBService.findAllWithoutPayloadByFlowIdOrderByCreatedDesc(lastFlowLog.get().getFlowId());
        return chainLogs.stream()
                .map(FlowLogWithoutPayload::getCreated)
                .min(Long::compareTo);
    }
}
