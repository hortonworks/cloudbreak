package com.sequenceiq.freeipa.flow.freeipa.binduser.create.event;

public class CreateBindUserFailureEvent extends CreateBindUserEvent {

    private final String failureMessage;

    private final Exception exception;

    public CreateBindUserFailureEvent(String selector, Long stackId, String accountId, String operationId, String suffix, String environmentCrn,
            String failureMessage, Exception exception) {
        super(selector, stackId, accountId, operationId, suffix, environmentCrn);
        this.failureMessage = failureMessage;
        this.exception = exception;
    }

    public CreateBindUserFailureEvent(String selector, CreateBindUserEvent event, String failureMessage, Exception exception) {
        super(selector, event.getResourceId(), event.getAccountId(), event.getOperationId(), event.getSuffix(), event.getEnvironmentCrn());
        this.failureMessage = failureMessage;
        this.exception = exception;
    }

    public String getFailureMessage() {
        return failureMessage;
    }

    public Exception getException() {
        return exception;
    }

    @Override
    public String toString() {
        return super.toString() + ' ' +
                "CreateBindUserFailureEvent{" +
                "failureMessage='" + failureMessage + '\'' +
                ", exception=" + exception +
                '}';
    }
}
