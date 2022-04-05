package com.sequenceiq.environment.environment.service.stack;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import javax.ws.rs.WebApplicationException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.StackV4Endpoint;
import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.auth.crn.RegionAwareInternalCrnGeneratorFactory;
import com.sequenceiq.cloudbreak.cloud.scheduler.PollGroup;
import com.sequenceiq.cloudbreak.common.exception.WebApplicationExceptionMessageExtractor;
import com.sequenceiq.environment.environment.domain.EnvironmentView;
import com.sequenceiq.environment.environment.flow.config.update.config.EnvStackConfigUpdatesFlowConfig;
import com.sequenceiq.environment.store.EnvironmentInMemoryStateStore;
import com.sequenceiq.flow.api.model.FlowIdentifier;
import com.sequenceiq.flow.domain.FlowLog;
import com.sequenceiq.flow.service.FlowCancelService;
import com.sequenceiq.flow.service.flowlog.FlowLogDBService;

@Service
public class StackService {

    private static final Logger LOGGER = LoggerFactory.getLogger(StackService.class);

    private final StackV4Endpoint stackV4Endpoint;

    private final FlowCancelService flowCancelService;

    private final FlowLogDBService flowLogDBService;

    private final WebApplicationExceptionMessageExtractor messageExtractor;

    private final RegionAwareInternalCrnGeneratorFactory regionAwareInternalCrnGeneratorFactory;

    public StackService(
        StackV4Endpoint stackV4Endpoint,
        FlowCancelService flowCancelService,
        FlowLogDBService flowLogDBService,
        WebApplicationExceptionMessageExtractor messageExtractor,
        RegionAwareInternalCrnGeneratorFactory regionAwareInternalCrnGeneratorFactory) {
        this.stackV4Endpoint = stackV4Endpoint;
        this.flowCancelService = flowCancelService;
        this.flowLogDBService = flowLogDBService;
        this.messageExtractor = messageExtractor;
        this.regionAwareInternalCrnGeneratorFactory = regionAwareInternalCrnGeneratorFactory;
    }

    public void triggerConfigUpdateForStack(String stackCrn) {
        try {
            stackV4Endpoint.updatePillarConfigurationByCrn(0L, stackCrn);
        } catch (WebApplicationException wae) {
            LOGGER.info(String
                .format("Unable to start config update for stack %s.  Message is %s", stackCrn,
                    messageExtractor.getErrorMessage(wae)));
            throw wae;
        }
    }

    public void cancelRunningStackConfigUpdates(EnvironmentView environment) {
        List<FlowLog> flowLogs = flowLogDBService.findAllByResourceIdAndFinalizedIsFalseOrderByCreatedDesc(environment.getId());
        if (!flowLogs.isEmpty() && flowLogs.get(0).isFlowType(EnvStackConfigUpdatesFlowConfig.class)) {
            LOGGER.info("Canceling running Stack config update flow for environment {}", environment.getResourceCrn());
            EnvironmentInMemoryStateStore.put(environment.getId(), PollGroup.CANCELLED);
            flowCancelService.cancelFlowSilently(flowLogs.get(0));
        } else {
            LOGGER.debug("No running Stack config update flow to cancel");
        }
    }

    public List<FlowIdentifier> updateLoadBalancer(Set<String> names) {
        String initiatorUserCrn = ThreadBasedUserCrnProvider.getUserCrn();
        List<FlowIdentifier> flowIdentifiers = new ArrayList<>();
        for (String name : names) {
            try {
                ThreadBasedUserCrnProvider.doAsInternalActor(
                        regionAwareInternalCrnGeneratorFactory.iam().getInternalCrnForServiceAsString(),
                        () -> {
                    FlowIdentifier flowidentifier = stackV4Endpoint.updateLoadBalancersInternal(0L, name, initiatorUserCrn);
                    flowIdentifiers.add(flowidentifier);
                });
            } catch (WebApplicationException e) {
                String errorMessage = messageExtractor.getErrorMessage(e);
                LOGGER.error(String.format("Failed update load balancer for stack %s due to: '%s'.", name, errorMessage), e);
                throw e;
            }
        }
        return flowIdentifiers;
    }
}
