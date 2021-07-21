package com.sequenceiq.cloudbreak.service.upgrade.validation.service;

public interface ServiceUpgradeValidator {

    void validate(ServiceUpgradeValidationRequest validationRequest);
}
