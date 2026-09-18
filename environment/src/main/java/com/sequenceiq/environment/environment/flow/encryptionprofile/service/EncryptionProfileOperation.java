package com.sequenceiq.environment.environment.flow.encryptionprofile.service;

public enum EncryptionProfileOperation {
    ENABLE("enable"),
    DISABLE("disable");

    private final String verb;

    EncryptionProfileOperation(String verb) {
        this.verb = verb;
    }

    public String verb() {
        return verb;
    }

}
