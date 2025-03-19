package com.sequenceiq.cloudbreak.service;

import static com.sequenceiq.cloudbreak.core.flow2.cluster.rds.upgrade.UpgradeRdsEvent.UPGRADE_RDS_UPGRADE_DATABASE_SERVER_FINISHED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.rds.upgrade.validation.ValidateRdsUpgradeEvent.VALIDATE_RDS_UPGRADE_CLEANUP_FINISHED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.rds.upgrade.validation.ValidateRdsUpgradeEvent.VALIDATE_RDS_UPGRADE_ON_CLOUDPROVIDER_FINISHED_EVENT;
import static com.sequenceiq.cloudbreak.event.ResourceEvent.STACK_RETRY_FLOW_START;

import java.util.List;

import jakarta.inject.Inject;
import jakarta.ws.rs.BadRequestException;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status;
import com.sequenceiq.cloudbreak.api.endpoint.v4.dto.NameOrCrn;
import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.service.stack.StackDtoService;
import com.sequenceiq.cloudbreak.service.stack.StackService;
import com.sequenceiq.cloudbreak.structuredevent.event.CloudbreakEventService;
import com.sequenceiq.cloudbreak.view.ClusterView;
import com.sequenceiq.flow.api.model.FlowIdentifier;
import com.sequenceiq.flow.core.FlowEvent;
import com.sequenceiq.flow.domain.FlowLog;
import com.sequenceiq.flow.domain.RetryResponse;
import com.sequenceiq.flow.domain.RetryableFlow;
import com.sequenceiq.flow.service.FlowRetryService;
import com.sequenceiq.redbeams.api.endpoint.v4.databaseserver.DatabaseServerV4Endpoint;

@Service
public class CloudbreakFlowRetryService {

    private static final Logger LOGGER = LoggerFactory.getLogger(CloudbreakFlowRetryService.class);

    private static final List<FlowEvent> STACK_RETRY_EVENTS = List.of(
            UPGRADE_RDS_UPGRADE_DATABASE_SERVER_FINISHED_EVENT,
            VALIDATE_RDS_UPGRADE_ON_CLOUDPROVIDER_FINISHED_EVENT,
            VALIDATE_RDS_UPGRADE_CLEANUP_FINISHED_EVENT
            );

    @Inject
    private StackService stackService;

    @Inject
    private StackDtoService stackDtoService;

    @Inject
    private FlowRetryService flowRetryService;

    @Inject
    private CloudbreakEventService eventService;

    @Inject
    private DatabaseServerV4Endpoint databaseServerV4Endpoint;

    public FlowIdentifier retryInWorkspace(NameOrCrn nameOrCrn, Long workspaceId) {
        Long stackId = stackService.getIdByNameOrCrnInWorkspace(nameOrCrn, workspaceId);
        RetryResponse retry = flowRetryService.retry(stackId, lastSuccessfulStateLog -> retryRedbeamsIfNecessary(stackId, lastSuccessfulStateLog));
        eventService.fireCloudbreakEvent(stackId, Status.UPDATE_IN_PROGRESS.name(), STACK_RETRY_FLOW_START, List.of(retry.getName()));
        return retry.getFlowIdentifier();
    }

    private void retryRedbeamsIfNecessary(Long stackId, FlowLog lastSuccessfulStateLog) {
        if (isRedbeamsRetryNecessary(lastSuccessfulStateLog.getNextEvent())) {
            LOGGER.info("Last successful state was {}, so try a retry on stack", lastSuccessfulStateLog.getNextEvent());
            ClusterView clusterView = stackDtoService.getClusterViewByStackId(stackId);
            if (StringUtils.isNotEmpty(clusterView.getDatabaseServerCrn())) {
                try {
                    ThreadBasedUserCrnProvider.doAsInternalActor(
                            () -> databaseServerV4Endpoint.retry(clusterView.getDatabaseServerCrn()));
                } catch (BadRequestException e) {
                    LOGGER.info("Cloudbreak retry failed on redbeams side, but try to restart the flow. Related exception: ", e);
                }
            } else {
                LOGGER.warn("RedBeams retry is not possible on {} as databasecrn is empty.", clusterView.getName());
            }
        }
    }

    private boolean isRedbeamsRetryNecessary(String retriedEvent) {
        if (StringUtils.isNotEmpty(retriedEvent)) {
            return STACK_RETRY_EVENTS.stream().anyMatch(flowEvent -> retriedEvent.equals(flowEvent.name()) || retriedEvent.equals(flowEvent.event()));
        } else {
            return false;
        }
    }

    public List<RetryableFlow> getRetryableFlows(String name, Long workspaceId) {
        Long stackId = stackService.getIdByNameInWorkspace(name, workspaceId);
        return flowRetryService.getRetryableFlows(stackId);
    }
}