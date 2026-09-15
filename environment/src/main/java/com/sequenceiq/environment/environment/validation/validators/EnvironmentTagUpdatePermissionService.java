package com.sequenceiq.environment.environment.validation.validators;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.EnvironmentTagUpdatePermissionValidator;
import com.sequenceiq.cloudbreak.cloud.exception.TagUpdatePermissionMissingException;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.environment.credential.v1.converter.CredentialToCloudCredentialConverter;
import com.sequenceiq.environment.environment.domain.Environment;
import com.sequenceiq.environment.exception.EnvironmentTagUpdatePermissionMissingException;

/**
 * Runs the per-cloud tag-update permission check and serves both of its callers:
 * <ul>
 *     <li>{@link #validate(Environment)} — the pre-flight check before the environment tag-modification flow is
 *     triggered. Translates a failure into an {@link EnvironmentTagUpdatePermissionMissingException} so the REST call
 *     fails fast with HTTP 400 rather than starting a flow that would partially succeed.</li>
 *     <li>{@link #findMissingPermissions(Environment)} — the same check as data, for the endpoint the UI calls before
 *     the customer enables tag propagation.</li>
 * </ul>
 * Both go through {@code findMissingPermissions}, so the probe cannot report "granted" for an edit that would then be
 * rejected.
 *
 * <p>Delegates to a per-cloud {@link EnvironmentTagUpdatePermissionValidator} registered by
 * {@link EnvironmentTagUpdatePermissionValidator#supportedPlatform()}; platforms without a registered validator
 * (e.g. Mock) report {@link TagUpdatePermissionResult#granted()} without touching the credential.</p>
 */
@Component
public class EnvironmentTagUpdatePermissionService {

    private static final Logger LOGGER = LoggerFactory.getLogger(EnvironmentTagUpdatePermissionService.class);

    private final Map<String, EnvironmentTagUpdatePermissionValidator> validatorsByPlatform;

    private final CredentialToCloudCredentialConverter credentialToCloudCredentialConverter;

    public EnvironmentTagUpdatePermissionService(List<EnvironmentTagUpdatePermissionValidator> validators,
            CredentialToCloudCredentialConverter credentialToCloudCredentialConverter) {
        this.validatorsByPlatform = validators.stream()
                .collect(Collectors.toMap(EnvironmentTagUpdatePermissionValidator::supportedPlatform, v -> v));
        this.credentialToCloudCredentialConverter = credentialToCloudCredentialConverter;
    }

    public void validate(Environment environment) {
        TagUpdatePermissionResult result = findMissingPermissions(environment);
        if (result.hasError()) {
            LOGGER.error("Tag-update permission check failed for environment '{}': {}", environment.getName(), result.message());
            throw new EnvironmentTagUpdatePermissionMissingException(result.message(), result.failedActions(), result.cause());
        }
    }

    public TagUpdatePermissionResult findMissingPermissions(Environment environment) {
        String cloudPlatform = environment.getCloudPlatform();
        EnvironmentTagUpdatePermissionValidator validator = validatorsByPlatform.get(cloudPlatform);
        if (validator == null) {
            LOGGER.debug("No tag-update permission validator registered for cloud platform '{}', skipping check.", cloudPlatform);
            return TagUpdatePermissionResult.granted();
        }
        CloudCredential cloudCredential = credentialToCloudCredentialConverter.convert(environment.getCredential());
        try {
            validator.validate(cloudCredential);
            return TagUpdatePermissionResult.granted();
        } catch (TagUpdatePermissionMissingException e) {
            return TagUpdatePermissionResult.error(e.getMessage(), e.getFailedActions(), e);
        }
    }
}
