package com.sequenceiq.cloudbreak.cloud;

import com.sequenceiq.cloudbreak.cloud.exception.TagUpdatePermissionMissingException;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;

/**
 * Verifies that the given cloud credential has the IAM permissions required to update
 * user-defined tags on already-provisioned cloud resources belonging to an environment.
 * One implementation per supported cloud platform; the environment side picks the right
 * one by {@link #supportedPlatform()}. Platforms without an implementation skip the
 * pre-flight check.
 */
public interface EnvironmentTagUpdatePermissionValidator {

    String supportedPlatform();

    void validate(CloudCredential cloudCredential) throws TagUpdatePermissionMissingException;
}
