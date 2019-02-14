package com.sequenceiq.cloudbreak.clusterdefinition.moduletest;

public class ModuleTestError extends AssertionError {
    public ModuleTestError(String message, Throwable cause) {
        super(message, cause);
    }
}
