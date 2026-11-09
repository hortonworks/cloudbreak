package com.sequenceiq.maintenance.client.schedule;

import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.auth.crn.Crn;
import com.sequenceiq.cloudbreak.common.exception.BadRequestException;
import com.sequenceiq.cloudbreak.common.exception.WebApplicationExceptionMessageExtractor;
import com.sequenceiq.maintenance.api.internal.schedule.endpoint.MaintenanceWindowScheduleInternalEndpoint;
import com.sequenceiq.maintenance.api.model.MaintenanceScopeType;
import com.sequenceiq.maintenance.api.util.MaintenanceNotFoundExceptions;
import com.sequenceiq.maintenance.api.v1.schedule.model.request.MaintenanceWindowScheduleFieldsRequest;

/**
 * Creates or rolls back a resource-scoped maintenance window schedule via the internal maintenance API. Internal (not
 * public) endpoints are used on purpose: the calling service has already authorized the user for the scoped resource
 * create/delete, so re-authorizing the (possibly flow-trigger or internal-actor) user here would reject legitimate
 * calls. The account is taken from the resource CRN, and calls run as the internal actor for that account.
 */
public class MaintenanceWindowScheduleProvisioner {

    private static final Logger LOGGER = LoggerFactory.getLogger(MaintenanceWindowScheduleProvisioner.class);

    private final MaintenanceWindowScheduleInternalEndpoint scheduleInternalEndpoint;

    private final MaintenanceScopeType scopeType;

    private final WebApplicationExceptionMessageExtractor exceptionMessageExtractor;

    public MaintenanceWindowScheduleProvisioner(
            MaintenanceWindowScheduleInternalEndpoint scheduleInternalEndpoint,
            MaintenanceScopeType scopeType,
            WebApplicationExceptionMessageExtractor exceptionMessageExtractor) {
        this.scheduleInternalEndpoint = scheduleInternalEndpoint;
        this.scopeType = scopeType;
        this.exceptionMessageExtractor = exceptionMessageExtractor;
    }

    public void createIfPresent(String scopeId, MaintenanceWindowScheduleFieldsRequest scheduleRequest) {
        if (scheduleRequest == null) {
            return;
        }
        String accountId = accountIdFromScopeId(scopeId);
        // Captured before switching to the internal actor so the schedule's createdBy reflects the real initiating user
        // (the one creating the owning resource), not the internal actor the call runs as.
        String initiatorUserCrn = ThreadBasedUserCrnProvider.getUserCrn();
        LOGGER.info("Creating {} maintenance window schedule for {}", scopeType, scopeId);
        try {
            ThreadBasedUserCrnProvider.doAsInternalActor(
                    () -> scheduleInternalEndpoint.create(
                            accountId, initiatorUserCrn, MaintenanceWindowScheduleCreateMapper.toCreateRequest(scheduleRequest, scopeType, scopeId)),
                    accountId);
        } catch (WebApplicationException e) {
            // The internal client reports a rejected schedule as a bare "HTTP 400 Bad Request", hiding the real reason
            // (e.g. "Schedule must have at least one occurrence within the next 90 days"). Read the maintenance service's
            // response body and re-raise it as a BadRequestException so the caller fails with a 400 carrying that message.
            throw asBadRequestIfClientError(scopeId, e);
        }
    }

    private RuntimeException asBadRequestIfClientError(String scopeId, WebApplicationException e) {
        Response response = e.getResponse();
        if (response != null && response.getStatusInfo().getFamily() == Response.Status.Family.CLIENT_ERROR) {
            String message = exceptionMessageExtractor.getErrorMessage(e);
            LOGGER.info("Maintenance service rejected the {} schedule for {}: {}", scopeType, scopeId, message);
            return new BadRequestException(message, e);
        }
        return e;
    }

    public void clearIfPresent(String scopeId) {
        String accountId = accountIdFromScopeId(scopeId);
        String initiatorUserCrn = ThreadBasedUserCrnProvider.getUserCrn();
        try {
            LOGGER.info("Deleting {} maintenance window schedule for {}", scopeType, scopeId);
            ThreadBasedUserCrnProvider.doAsInternalActor(
                    () -> scheduleInternalEndpoint.delete(accountId, initiatorUserCrn, scopeType.name(), scopeId),
                    accountId);
        } catch (RuntimeException e) {
            if (!MaintenanceNotFoundExceptions.isNotFound(e)) {
                throw e;
            }
        }
    }

    /**
     * Deletes the schedule, swallowing any failure (logged as WARN) so a maintenance-service problem never breaks an
     * otherwise-complete delete/rollback. Callers that must not fail on maintenance errors should use this instead of
     * {@link #clearIfPresent(String)}.
     */
    public void clearQuietly(String scopeId) {
        try {
            clearIfPresent(scopeId);
        } catch (RuntimeException e) {
            LOGGER.warn("Failed to clear {} maintenance window schedule for {}", scopeType, scopeId, e);
        }
    }

    private static String accountIdFromScopeId(String scopeId) {
        return Crn.safeFromString(scopeId).getAccountId();
    }
}
