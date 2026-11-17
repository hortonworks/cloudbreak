package com.sequenceiq.environment.environment.service.stack;

import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.apache.commons.collections4.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.dyngr.Polling;
import com.dyngr.core.AttemptMaker;
import com.dyngr.exception.PollerStoppedException;
import com.dyngr.exception.UserBreakException;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.StackType;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.StackV4Endpoint;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.StackViewV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.StackViewV4Responses;
import com.sequenceiq.cloudbreak.common.exception.CloudbreakServiceException;
import com.sequenceiq.environment.environment.flow.modify.tags.EnvTagsModificationSupport;
import com.sequenceiq.environment.environment.poller.StackPollerProvider;
import com.sequenceiq.environment.exception.DatahubOperationFailedException;
import com.sequenceiq.environment.exception.StackOperationFailedException;
import com.sequenceiq.flow.api.model.FlowIdentifier;

@Service
public class StackPollerService {

    private static final Logger LOGGER = LoggerFactory.getLogger(StackPollerService.class);

    private static final Set<Status> SKIPPED_STATES = EnumSet.of(
            Status.CREATE_FAILED,
            Status.STOPPED,
            Status.STOP_IN_PROGRESS,
            Status.STOP_REQUESTED,
            Status.STOP_FAILED,
            Status.DELETE_IN_PROGRESS,
            Status.DELETE_COMPLETED,
            Status.DELETED_ON_PROVIDER_SIDE,
            Status.DELETE_FAILED,
            Status.PRE_DELETE_IN_PROGRESS,
            Status.START_FAILED,
            Status.EXTERNAL_DATABASE_CREATION_FAILED,
            Status.EXTERNAL_DATABASE_DELETION_FINISHED,
            Status.EXTERNAL_DATABASE_DELETION_FAILED,
            Status.EXTERNAL_DATABASE_START_FAILED,
            Status.EXTERNAL_DATABASE_START_FINISHED,
            Status.EXTERNAL_DATABASE_START_IN_PROGRESS,
            Status.EXTERNAL_DATABASE_STOP_FAILED,
            Status.EXTERNAL_DATABASE_STOP_FINISHED,
            Status.EXTERNAL_DATABASE_STOP_IN_PROGRESS
    );

    private final StackV4Endpoint stackV4Endpoint;

    private final StackPollerProvider stackPollerProvider;

    @Value("${env.stack.config.update.polling.maximum.seconds:7200}")
    private Integer maxTime;

    @Value("${env.stack.config.update.sleep.time.seconds:60}")
    private Integer sleepTime;

    public StackPollerService(StackV4Endpoint stackV4Endpoint, StackPollerProvider stackPollerProvider) {
        this.stackV4Endpoint = stackV4Endpoint;
        this.stackPollerProvider = stackPollerProvider;
    }

    public List<FlowIdentifier> updateSaltOnStacks(Long envId, String envCrn) {
        List<String> stackNames = getUpdatableStacks(envCrn, StackViewV4Response::getName);
        LOGGER.info("Stacks names which will be updated: {}", stackNames);
        return startStackUpdatePolling(stackNames,
                stackPollerProvider.saltUpdateOnStacksPoller(stackNames, envId));
    }

    public List<FlowIdentifier> updateSaltOnDatahubStacks(Long envId, String envCrn) {
        List<String> stackNames = getUpdatableStacks(envCrn, StackViewV4Response::getName,
                stack -> StackType.WORKLOAD.name().equals(stack.getStackType()));
        LOGGER.info("Datahub stack names which will be salt-updated: {}", stackNames);
        return startStackUpdatePolling(stackNames,
                stackPollerProvider.saltUpdateOnStacksPoller(stackNames, envId));
    }

    private List<FlowIdentifier> startStackUpdatePolling(List<String> stackNames, AttemptMaker<List<FlowIdentifier>> attemptMaker) {
        if (CollectionUtils.isNotEmpty(stackNames)) {
            try {
                return Polling.stopAfterDelay(maxTime, TimeUnit.SECONDS)
                        .stopIfException(true)
                        .waitPeriodly(sleepTime, TimeUnit.SECONDS)
                        .run(attemptMaker);
            } catch (PollerStoppedException e) {
                LOGGER.warn("Stack updating timed out");
                throw new DatahubOperationFailedException("Stack updating timed out", e);
            } catch (UserBreakException e) {
                LOGGER.error("Stack updating aborted with error", e);
                throw new DatahubOperationFailedException("Stack updating aborted with error", e);
            }
        }
        return null;
    }

    public void updateStackConfigurations(Long envId, String envCrn, String flowId) {
        List<String> stackCrns = getUpdatableStacks(envCrn, StackViewV4Response::getCrn);
        LOGGER.info("Stacks CRNs which will be updated: {}", stackCrns);
        startStackConfigUpdatePolling(stackCrns,
                stackPollerProvider.stackUpdateConfigPoller(stackCrns, envId, flowId));
    }

    private void startStackConfigUpdatePolling(List<String> stackCrns, AttemptMaker<Void> attemptMaker) {
        if (CollectionUtils.isNotEmpty(stackCrns)) {
            try {
                Polling.stopAfterDelay(maxTime, TimeUnit.SECONDS)
                        .stopIfException(true)
                        .waitPeriodly(sleepTime, TimeUnit.SECONDS)
                        .run(attemptMaker);
            } catch (PollerStoppedException e) {
                LOGGER.warn("Stack config updating timed out");
                throw new DatahubOperationFailedException("Stack config updating timed out", e);
            } catch (UserBreakException e) {
                LOGGER.error("Stack config updating aborted with error", e);
                throw new DatahubOperationFailedException("Stack config updating aborted with error", e);
            }
        }
    }

    public List<String> getUpdatableStacks(String envCrn, Function<StackViewV4Response, String> requestedAttributeMapper) {
        return getUpdatableStacks(envCrn, requestedAttributeMapper, stack -> true);
    }

    public List<String> getUpdatableStacks(String envCrn, Function<StackViewV4Response, String> requestedAttributeMapper,
            Predicate<StackViewV4Response> additionalFilter) {
        StackViewV4Responses stackViewV4Responses = stackV4Endpoint.list(0L, envCrn, false);
        Collection<StackViewV4Response> responses = Optional.ofNullable(stackViewV4Responses.getResponses()).orElse(List.of());
        List<String> responseToLog = responses.stream()
                .map(response -> String.format("[Name: %s; Crn: %s; Status: %s, ClusterStatus: %s]",
                        response.getName(), response.getCrn(), response.getStatus(), response.getCluster().getStatus()))
                .collect(Collectors.toList());
        LOGGER.info("Stacks returned for update: {}", responseToLog);
        return responses.stream()
                .filter(stack -> !SKIPPED_STATES.contains(stack.getCluster().getStatus()))
                .filter(additionalFilter)
                .map(requestedAttributeMapper)
                .collect(Collectors.toList());
    }

    /**
     * Applies user-defined tag updates or deletions on stacks of the given type.
     * Non-empty {@code tagsToRemove} selects deletion; otherwise tags are updated.
     */
    public void modifyUserDefinedTagsOnStacks(
            Long envId,
            String envCrn,
            StackType stackType,
            Map<String, String> tags,
            Set<String> tagsToRemove) {
        if (EnvTagsModificationSupport.hasTagsToRemove(tagsToRemove)) {
            doModifyUserDefinedTagsOnStacks(envId, envCrn, stackType, null, tagsToRemove);
        } else {
            doModifyUserDefinedTagsOnStacks(envId, envCrn, stackType, tags, null);
        }
    }

    private void doModifyUserDefinedTagsOnStacks(
            Long envId,
            String envCrn,
            StackType stackType,
            Map<String, String> tags,
            Set<String> tagKeys) {
        List<String> stackCrns = getUpdatableStacks(envCrn, StackViewV4Response::getCrn,
                stack -> stackType.name().equals(stack.getStackType()) && !SKIPPED_STATES.contains(stack.getStatus()));
        String operation = tags != null ? "update" : "deletion";
        LOGGER.info("User defined tags {} will be applied on stacks: {}", operation, stackCrns);

        AttemptMaker<List<FlowIdentifier>> attemptMaker = tags != null
                ? stackPollerProvider.userDefinedTagsUpdatePoller(stackCrns, envId, tags)
                : stackPollerProvider.userDefinedTagsDeletePoller(stackCrns, envId, tagKeys);
        List<FlowIdentifier> flowIdentifiers = triggerUserDefinedTagsModificationOnStacks(stackCrns, attemptMaker, operation);
        awaitUserDefinedTagsModificationCompletion(flowIdentifiers, envId, operation);
    }

    private List<FlowIdentifier> triggerUserDefinedTagsModificationOnStacks(List<String> stackCrns, AttemptMaker<List<FlowIdentifier>> attemptMaker,
            String operation) {
        if (CollectionUtils.isNotEmpty(stackCrns)) {
            try {
                return Polling.stopAfterDelay(maxTime, TimeUnit.SECONDS)
                        .stopIfException(true)
                        .waitPeriodly(sleepTime, TimeUnit.SECONDS)
                        .run(attemptMaker);
            } catch (PollerStoppedException e) {
                String message = "Starting stack user defined tags " + operation + " timed out";
                LOGGER.warn(message, e);
                throw new StackOperationFailedException(message, e);
            } catch (UserBreakException e) {
                String message = "Starting stack user defined tags " + operation + " aborted with error";
                LOGGER.error(message, e);
                throw new StackOperationFailedException(message, e);
            }
        }
        return Collections.emptyList();
    }

    private void awaitUserDefinedTagsModificationCompletion(List<FlowIdentifier> flowIdentifiers, Long envId, String operation) {
        if (CollectionUtils.isNotEmpty(flowIdentifiers)) {
            try {
                Polling.stopAfterDelay(maxTime, TimeUnit.SECONDS)
                        .stopIfException(true)
                        .waitPeriodly(sleepTime, TimeUnit.SECONDS)
                        .run(stackPollerProvider.updateUserDefinedTags(envId, flowIdentifiers));
            } catch (PollerStoppedException e) {
                String message = "Stack user defined tags " + operation + " timed out or error happened: " + e.getMessage();
                LOGGER.warn(message, e);
                throw new CloudbreakServiceException(message);
            } catch (UserBreakException e) {
                String message = "Stack user defined tags " + operation + " aborted with error: " + e.getMessage();
                LOGGER.error(message, e);
                throw new CloudbreakServiceException(message);
            }
        }
    }
}
