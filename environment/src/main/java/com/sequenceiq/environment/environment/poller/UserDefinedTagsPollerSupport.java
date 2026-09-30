package com.sequenceiq.environment.environment.poller;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

import jakarta.ws.rs.BadRequestException;

import org.springframework.stereotype.Component;

import com.dyngr.core.AttemptMaker;
import com.dyngr.core.AttemptResult;
import com.dyngr.core.AttemptResults;
import com.dyngr.core.AttemptState;
import com.sequenceiq.flow.api.model.FlowIdentifier;

/**
 * Shared dyngr {@link AttemptMaker} for triggering user-defined tag update or delete on many child resource CRNs
 * (Cloudbreak stacks or Redbeams database servers) for one environment.
 * <p>
 * {@link StackPollerProvider} and {@link com.sequenceiq.environment.environment.service.database.RedbeamsPollerProvider}
 * call {@link #updatePoller} or {@link #deletePoller} with a per-CRN trigger lambda (typically
 * {@code StackService} / {@code RedBeamsService}). This component only <em>starts</em> downstream flows; poller
 * services poll until those flows complete ({@code StackPollerProvider#updateUserDefinedTags},
 * {@code RedbeamsPollerProvider#userDefinedTagsFlowsCompletionPoller}, etc.).
 * <p>
 * Each invocation of the returned {@code AttemptMaker} processes the current CRN list: call trigger per CRN, then
 * merge per-CRN results via {@link FlowResultPollerEvaluator#attemptResultFinisher}. CRNs that must be retried stay
 * in a mutable list for the next attempt.
 * <p>
 * Update and delete differ in conflict handling and in how flow identifiers are returned:
 * <table>
 *   <caption>Trigger conflict handling</caption>
 *   <tr><th>Operation</th><th>{@link BadRequestException}</th><th>Other {@link Exception}</th><th>Flow IDs across attempts</th></tr>
 *   <tr><td>UPDATE</td><td>Treat as flow-running conflict: retry CRN ({@link AttemptResults#justContinue()})</td>
 *       <td>{@link AttemptResults#breakFor(Throwable)} unless finisher says otherwise</td>
 *       <td>Not accumulated; final batch comes from the finishing attempt only</td></tr>
 *   <tr><td>DELETE</td><td>{@link AttemptResults#breakFor(Throwable)} (not a delete retry path)</td>
 *       <td>Retry CRN if {@link FlowRunningConflictDetector#isFlowRunningConflict(Throwable)} (HTTP 409 in cause chain)</td>
 *       <td>Successful IDs accumulated until all CRNs complete</td></tr>
 * </table>
 * On delete, retriable flow-running conflicts are recognized with {@link FlowRunningConflictDetector} when child
 * services wrap HTTP {@code 409 CONFLICT} in domain exceptions.
 * <p>
 */
@Component
public class UserDefinedTagsPollerSupport {

    private final FlowResultPollerEvaluator flowResultPollerEvaluator;

    public UserDefinedTagsPollerSupport(FlowResultPollerEvaluator flowResultPollerEvaluator) {
        this.flowResultPollerEvaluator = flowResultPollerEvaluator;
    }

    public AttemptMaker<List<FlowIdentifier>> updatePoller(
            List<String> resourceCrns,
            Long envId,
            String resourceTypeLabel,
            String remoteServiceName,
            String resourceLabel,
            Function<String, FlowIdentifier> trigger) {
        return poller(resourceCrns, envId, resourceTypeLabel, UserDefinedTagsModificationType.UPDATE,
                remoteServiceName, resourceLabel, trigger);
    }

    public AttemptMaker<List<FlowIdentifier>> deletePoller(
            List<String> resourceCrns,
            Long envId,
            String resourceTypeLabel,
            String remoteServiceName,
            String resourceLabel,
            Function<String, FlowIdentifier> trigger) {
        return poller(resourceCrns, envId, resourceTypeLabel, UserDefinedTagsModificationType.DELETE,
                remoteServiceName, resourceLabel, trigger);
    }

    private AttemptMaker<List<FlowIdentifier>> poller(
            List<String> resourceCrns,
            Long envId,
            String resourceTypeLabel,
            UserDefinedTagsModificationType kind,
            String remoteServiceName,
            String resourceLabel,
            Function<String, FlowIdentifier> trigger) {
        List<String> mutableCrnsList = new ArrayList<>(resourceCrns);
        List<FlowIdentifier> accumulatedFlowIdentifiers = kind == UserDefinedTagsModificationType.DELETE ? new ArrayList<>() : null;
        return () -> {
            kind.logAttempt(mutableCrnsList.size(), resourceTypeLabel, envId);
            List<String> remaining = new ArrayList<>();
            List<AttemptResult<FlowIdentifier>> results = mutableCrnsList.stream()
                    .map(resourceCrn -> fetchResults(remaining, resourceCrn, kind, remoteServiceName, resourceLabel, trigger))
                    .collect(Collectors.toList());
            AttemptResult<List<FlowIdentifier>> result = flowResultPollerEvaluator.attemptResultFinisher(results);
            if (kind == UserDefinedTagsModificationType.UPDATE) {
                mutableCrnsList.retainAll(remaining);
                return result;
            }
            if (result.getState() == AttemptState.BREAK) {
                return result;
            }
            accumulatedFlowIdentifiers.addAll(results.stream()
                    .filter(attemptResult -> attemptResult.getState() == AttemptState.FINISH)
                    .map(AttemptResult::getResult)
                    .toList());
            mutableCrnsList.retainAll(remaining);
            return mutableCrnsList.isEmpty()
                    ? AttemptResults.finishWith(accumulatedFlowIdentifiers)
                    : AttemptResults.justContinue();
        };
    }

    /**
     * Triggers tag modification for one CRN. Conflict/retry rules are documented on {@link UserDefinedTagsPollerSupport}.
     */
    private AttemptResult<FlowIdentifier> fetchResults(
            List<String> remainingResources,
            String resourceCrn,
            UserDefinedTagsModificationType kind,
            String remoteServiceName,
            String resourceLabel,
            Function<String, FlowIdentifier> trigger) {
        try {
            kind.logTrigger(remoteServiceName, resourceLabel, resourceCrn);
            return AttemptResults.finishWith(trigger.apply(resourceCrn));
        } catch (BadRequestException e) {
            if (kind != UserDefinedTagsModificationType.UPDATE) {
                kind.logUnexpectedFailure(remoteServiceName, e.getMessage());
                return AttemptResults.breakFor(e);
            }
            kind.logFlowConflictRetry(resourceCrn, kind.resourceLabelForConflictMessage(resourceLabel));
            remainingResources.add(resourceCrn);
            return AttemptResults.justContinue();
        } catch (Exception e) {
            if (kind == UserDefinedTagsModificationType.DELETE && FlowRunningConflictDetector.isFlowRunningConflict(e)) {
                kind.logFlowConflictRetry(resourceCrn, kind.resourceLabelForConflictMessage(resourceLabel));
                remainingResources.add(resourceCrn);
                return AttemptResults.justContinue();
            }
            kind.logUnexpectedFailure(remoteServiceName, e.getMessage());
            return AttemptResults.breakFor(e);
        }
    }
}
