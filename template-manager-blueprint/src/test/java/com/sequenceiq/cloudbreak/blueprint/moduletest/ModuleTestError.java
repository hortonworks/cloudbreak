package com.sequenceiq.cloudbreak.blueprint.moduletest;

public class ModuleTestError extends AssertionError {
    public ModuleTestError(String message, Throwable cause) {
        super(message, cause);
    }
}
