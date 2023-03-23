package com.sequenceiq.cloudbreak.orchestrator.salt.states;

public class SaltEmptyResponseException extends SaltExecutionWentWrongException {
    public SaltEmptyResponseException(String message) {
        super(message);
    }
}
