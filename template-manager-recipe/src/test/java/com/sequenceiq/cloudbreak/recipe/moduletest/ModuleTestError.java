package com.sequenceiq.cloudbreak.recipe.moduletest;

public class ModuleTestError extends AssertionError {
    public ModuleTestError(String message, Throwable cause) {
        super(message, cause);
    }
}
