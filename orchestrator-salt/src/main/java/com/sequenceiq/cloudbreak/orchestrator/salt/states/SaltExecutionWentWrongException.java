package com.sequenceiq.cloudbreak.orchestrator.salt.states;

public class SaltExecutionWentWrongException extends RuntimeException {

    public SaltExecutionWentWrongException(String message) {
        super(message);
    }

}
