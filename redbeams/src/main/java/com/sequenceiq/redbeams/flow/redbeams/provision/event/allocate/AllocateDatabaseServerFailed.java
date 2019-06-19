package com.sequenceiq.redbeams.flow.redbeams.provision.event.allocate;

/**
 * The event that occurs when a database server allocation has failed.
 */
public class AllocateDatabaseServerFailed extends AllocateDatabaseServerResponse {

    private Exception errorDetails;

    public AllocateDatabaseServerFailed(Exception errorDetails, Long resourceId) {
        super(resourceId);

        this.errorDetails = errorDetails;
    }

    public Exception getErrorDetails() {
        return errorDetails;
    }

    @Override
    public String toString() {
        return "AllocateDatabaseServerFailed{"
                + "errorDetails=" + errorDetails
                + ", resourceId=" + getResourceId()
                + '}';
    }
}
