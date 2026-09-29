package com.sequenceiq.it.cloudbreak.preflight;

public interface PreflightCheck {

    PreflightCheckResult check();

    String getName();
}
