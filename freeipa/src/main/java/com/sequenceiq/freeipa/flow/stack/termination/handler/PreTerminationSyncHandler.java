package com.sequenceiq.freeipa.flow.stack.termination.handler;

import static com.sequenceiq.cloudbreak.util.Benchmark.checkedMeasure;

import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.auth.crn.RegionAwareInternalCrnGeneratorFactory;
import com.sequenceiq.cloudbreak.cloud.event.resource.TerminateStackResult;
import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.flow.event.EventSelectorUtil;
import com.sequenceiq.flow.reactor.api.handler.ExceptionCatcherEventHandler;
import com.sequenceiq.flow.reactor.api.handler.HandlerEvent;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.DetailedStackStatus;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.instance.InstanceStatus;
import com.sequenceiq.freeipa.entity.InstanceMetaData;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.flow.stack.StackFailureEvent;
import com.sequenceiq.freeipa.flow.stack.termination.StackTerminationEvent;
import com.sequenceiq.freeipa.flow.stack.termination.event.sync.PreTerminationSyncFinished;
import com.sequenceiq.freeipa.flow.stack.termination.event.sync.PreTerminationSyncRequest;
import com.sequenceiq.freeipa.service.stack.StackService;
import com.sequenceiq.freeipa.sync.FreeipaChecker;
import com.sequenceiq.freeipa.sync.FreeipaStatusInfoLogger;
import com.sequenceiq.freeipa.sync.ProviderChecker;
import com.sequenceiq.freeipa.sync.SyncResult;

@Component
public class PreTerminationSyncHandler extends ExceptionCatcherEventHandler<PreTerminationSyncRequest> {

    private static final Logger LOGGER = LoggerFactory.getLogger(PreTerminationSyncHandler.class);

    @Inject
    private RegionAwareInternalCrnGeneratorFactory regionAwareInternalCrnGeneratorFactory;

    @Inject
    private StackService stackService;

    @Inject
    private FreeipaChecker freeipaChecker;

    @Inject
    private ProviderChecker providerChecker;

    @Inject
    private FreeipaStatusInfoLogger freeipaStatusInfoLogger;

    @Override
    protected Selectable doAccept(HandlerEvent<PreTerminationSyncRequest> event) {
        PreTerminationSyncRequest request = event.getData();
        Long stackId = request.getResourceId();
        try {
            checkedMeasure(() -> {
                ThreadBasedUserCrnProvider.doAsInternalActor(
                        regionAwareInternalCrnGeneratorFactory.iam().getInternalCrnForServiceAsString(), () -> syncInstances(stackId));
                return null;
            }, LOGGER, "freeipa stack sync in {}ms");
        } catch (Exception e) {
            if (request.getForced()) {
                return new PreTerminationSyncFinished(stackId, request.getForced());
            }
            return new StackFailureEvent(EventSelectorUtil.failureSelector(TerminateStackResult.class), stackId, e);
        }
        return new PreTerminationSyncFinished(stackId, request.getForced());
    }

    private void syncInstances(Long stackId) {
        Stack stack = stackService.getByIdWithListsInTransaction(stackId);
        Set<InstanceMetaData> notTerminatedForStack = stack.getAllInstanceMetaDataList().stream()
                .filter(Predicate.not(InstanceMetaData::isTerminated))
                .collect(Collectors.toSet());
        Set<InstanceMetaData> checkableInstances = notTerminatedForStack.stream()
                .filter(Predicate.not(InstanceMetaData::isDeletedOnProvider))
                .collect(Collectors.toSet());

        int alreadyDeletedCount = notTerminatedForStack.size() - checkableInstances.size();
        if (alreadyDeletedCount > 0) {
            LOGGER.info("Count of instances already in DELETED_ON_PROVIDER_SIDE state: {}", alreadyDeletedCount);
        }
        if (!checkableInstances.isEmpty()) {
            SyncResult syncResult = freeipaChecker.getStatus(stack, checkableInstances);
            if (DetailedStackStatus.AVAILABLE == syncResult.getStatus()) {
                syncResult.getInstanceStatusMap().entrySet().forEach(entry ->
                        updateInstanceStatus(entry.getKey(), entry.getValue()));
            } else {
                providerChecker.updateAndGetStatuses(stack, checkableInstances, syncResult.getInstanceStatusMap(), false);
            }
        }
        freeipaStatusInfoLogger.logFreeipaStatus(stack.getId(), checkableInstances);
    }

    @Override
    protected Selectable defaultFailureEvent(Long resourceId, Exception e, Event<PreTerminationSyncRequest> event) {
        boolean forced = event.getData().getForced();
        if (!forced) {
            return new StackFailureEvent(StackTerminationEvent.TERMINATION_FAILED_EVENT.event(), resourceId, e);
        } else {
            return new PreTerminationSyncFinished(event.getData().getResourceId(), forced);
        }
    }

    @Override
    public String selector() {
        return EventSelectorUtil.selector(PreTerminationSyncRequest.class);
    }

    private void updateInstanceStatus(InstanceMetaData instanceMetaData, DetailedStackStatus detailedStackStatus) {
        switch (detailedStackStatus) {
            case AVAILABLE:
                setStatusIfNotTheSame(instanceMetaData, InstanceStatus.CREATED);
                break;
            case UNHEALTHY:
                setStatusIfNotTheSame(instanceMetaData, InstanceStatus.UNHEALTHY);
                break;
            case UNREACHABLE:
                setStatusIfNotTheSame(instanceMetaData, InstanceStatus.UNREACHABLE);
                break;
            default:
                LOGGER.info("the '{}' status is not converted", detailedStackStatus);
        }
    }

    private void setStatusIfNotTheSame(InstanceMetaData instanceMetaData, InstanceStatus newStatus) {
        InstanceStatus oldStatus = instanceMetaData.getInstanceStatus();
        if (oldStatus != newStatus) {
            instanceMetaData.setInstanceStatus(newStatus);
            LOGGER.info("The instance status has been updated from {} to {}", oldStatus, newStatus);
        }
    }
}
