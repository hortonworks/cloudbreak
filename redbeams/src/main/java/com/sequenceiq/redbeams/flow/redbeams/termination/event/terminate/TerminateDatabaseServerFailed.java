package com.sequenceiq.redbeams.flow.redbeams.termination.event.terminate;

/**
 * The event that occurs when a database server termination has failed.
 */
public class TerminateDatabaseServerFailed extends TerminateDatabaseServerResponse {

    private Exception errorDetails;

    public TerminateDatabaseServerFailed(Exception errorDetails, Long resourceId) {
        super(resourceId);

        this.errorDetails = errorDetails;
    }

    public Exception getErrorDetails() {
        return errorDetails;
    }

    @Override
    public String toString() {
        return "TerminateDatabaseServerFailed{"
                + "errorDetails=" + errorDetails
                + ", resourceId=" + getResourceId()
                + '}';
    }
}
