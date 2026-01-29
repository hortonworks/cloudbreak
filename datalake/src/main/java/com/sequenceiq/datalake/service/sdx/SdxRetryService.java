package com.sequenceiq.datalake.service.sdx;

import static java.util.function.Function.identity;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.annotation.PostConstruct;
import jakarta.inject.Inject;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.InternalServerErrorException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.google.common.collect.Sets;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.StackV4Endpoint;
import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.datalake.entity.SdxCluster;
import com.sequenceiq.datalake.flow.RetryableDatalakeFlowConfiguration;
import com.sequenceiq.datalake.flow.dr.backup.DatalakeBackupFlowConfig;
import com.sequenceiq.datalake.flow.dr.restore.DatalakeRestoreFlowConfig;
import com.sequenceiq.flow.api.model.FlowIdentifier;
import com.sequenceiq.flow.core.Flow2Handler;
import com.sequenceiq.flow.core.FlowEvent;
import com.sequenceiq.flow.core.config.FlowConfiguration;
import com.sequenceiq.flow.core.metrics.FlowEnumUtil;
import com.sequenceiq.flow.domain.ClassValue;
import com.sequenceiq.flow.domain.FlowLog;

@Service
public class SdxRetryService {

    private static final Logger LOGGER = LoggerFactory.getLogger(SdxRetryService.class);

    private static final Set<Class<? extends FlowConfiguration<?>>> FULLY_RESTARTABLE_FLOWS = Sets.newHashSet(
            DatalakeRestoreFlowConfig.class,
            DatalakeBackupFlowConfig.class);

    @Inject
    private Flow2Handler flow2Handler;

    @Inject
    private StackV4Endpoint stackV4Endpoint;

    @Inject
    private List<? extends RetryableDatalakeFlowConfiguration<? extends FlowEvent>> retryableDatalakeFlowConfigurations;

    private Map<? extends Class<? extends RetryableDatalakeFlowConfiguration>,
            ? extends RetryableDatalakeFlowConfiguration<? extends FlowEvent>> retryableDatalakeFlowConfigurationMap;

    @PostConstruct
    void init() {
        retryableDatalakeFlowConfigurationMap = retryableDatalakeFlowConfigurations.stream()
                .collect(Collectors.toMap(flowConfiguration -> flowConfiguration.getClass(), identity()));
    }

    public FlowIdentifier retrySdx(SdxCluster sdxCluster) {
        FlowLog flowLog = flow2Handler.getFirstRetryableStateLogfromLatestFlow(sdxCluster.getId());
        if (!flowLog.getFlowType().isOnClassPath()) {
            throw new InternalServerErrorException(String.format("Flow type %s is not on classpath", flowLog.getFlowType().getName()));
        }
        if (FULLY_RESTARTABLE_FLOWS.contains(flowLog.getFlowType().getClassValue())) {
            return flow2Handler.retryLastFailedFlowFromStart(sdxCluster.getId());
        } else {
            return flow2Handler.retryLastFailedFlow(sdxCluster.getId(), lastSuccessfulStateLog -> retryCloudbreakIfNecessary(sdxCluster,
                    lastSuccessfulStateLog));
        }
    }

    private void retryCloudbreakIfNecessary(SdxCluster sdxCluster, FlowLog lastSuccessfulStateLog) {
        if (isCloudbreakRetryNecessary(lastSuccessfulStateLog.getNextEvent(), lastSuccessfulStateLog.getFlowType())) {
            LOGGER.info("Last successful state was " + lastSuccessfulStateLog.getNextEvent() + ", so try a retry on stack");
            try {
                ThreadBasedUserCrnProvider.doAsInternalActor(
                        () -> stackV4Endpoint.retry(0L, sdxCluster.getClusterName(), sdxCluster.getAccountId()));
            } catch (BadRequestException e) {
                LOGGER.warn("Sdx retry failed on cloudbreak side, but try to restart the flow. Related exception: ", e);
            }
        }
    }

    private boolean isCloudbreakRetryNecessary(String retriedEvent, ClassValue flowType) {
        if (!retryableDatalakeFlowConfigurationMap.containsKey(flowType.getClassValue())) {
            LOGGER.info("Missing retryable datalake flow configuration for type: {}", flowType);
            return false;
        }
        RetryableDatalakeFlowConfiguration<? extends FlowEvent> flowConfiguration = retryableDatalakeFlowConfigurationMap.get(flowType.getClassValue());
        Enum<? extends FlowEvent> flowEventEnum = FlowEnumUtil.getFlowEventEnum(flowConfiguration.getEventType(), retriedEvent);
        return flowEventEnum != null && flowConfiguration.getStackRetryEvents().contains(flowEventEnum);
    }
}
