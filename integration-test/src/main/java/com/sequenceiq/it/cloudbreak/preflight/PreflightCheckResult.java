package com.sequenceiq.it.cloudbreak.preflight;

public class PreflightCheckResult {

    private final String checkName;

    private final boolean passed;

    private final boolean skipped;

    private final String message;

    private PreflightCheckResult(String checkName, boolean passed, boolean skipped, String message) {
        this.checkName = checkName;
        this.passed = passed;
        this.skipped = skipped;
        this.message = message;
    }

    public static PreflightCheckResult pass(String checkName, String message) {
        return new PreflightCheckResult(checkName, true, false, message);
    }

    public static PreflightCheckResult fail(String checkName, String message) {
        return new PreflightCheckResult(checkName, false, false, message);
    }

    public static PreflightCheckResult skip(String checkName, String reason) {
        return new PreflightCheckResult(checkName, false, true, reason);
    }

    public String getCheckName() {
        return checkName;
    }

    public boolean isPassed() {
        return passed;
    }

    public boolean isSkipped() {
        return skipped;
    }

    public String getMessage() {
        return message;
    }
}
