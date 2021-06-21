package com.sequenceiq.cloudbreak.service.upgrade.image;

public final class BlueprintValidationResult {

    private final boolean valid;

    private final String reason;

    public BlueprintValidationResult(boolean valid) {
        this.valid = valid;
        this.reason = null;
    }

    public BlueprintValidationResult(boolean valid, String reason) {
        this.valid = valid;
        this.reason = reason;
    }

    public boolean isValid() {
        return valid;
    }

    public String getReason() {
        return reason;
    }
}
