package com.sequenceiq.environment.environment.poller;

import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;

public final class FlowRunningConflictDetector {

    private FlowRunningConflictDetector() {
    }

    /**
     * A child resource that already has a flow running answers the trigger request with HTTP 409 (CONFLICT). The service layer wraps that
     * {@link WebApplicationException} into a domain specific runtime exception, so the conflict has to be recognized from the exception chain.
     *
     * @return {@code true} if the exception (or its cause) is a {@link WebApplicationException} carrying a CONFLICT status.
     */
    public static boolean isFlowRunningConflict(Throwable e) {
        return e.getCause() instanceof WebApplicationException wae
                && wae.getResponse() != null
                && wae.getResponse().getStatus() == Response.Status.CONFLICT.getStatusCode();
    }
}
