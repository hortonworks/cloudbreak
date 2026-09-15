package com.sequenceiq.cloudbreak.cloud.azure.validator;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import jakarta.inject.Inject;

import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.EnvironmentTagUpdatePermissionValidator;
import com.sequenceiq.cloudbreak.cloud.azure.client.AzureClient;
import com.sequenceiq.cloudbreak.cloud.azure.client.AzureClientService;
import com.sequenceiq.cloudbreak.cloud.exception.TagUpdatePermissionMissingException;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;

/**
 * Verifies that the role definitions assigned to the credential's service principal cover every action the
 * tag-update flow needs across Compute, Network, Resources and PostgreSQL. The required set lives in
 * {@code definitions/azure-tag-update-minimal-role-def.json} and is derived from the strategies under
 * {@code com.sequenceiq.cloudbreak.cloud.azure.tag} — keep it in sync when a new tag strategy is added.
 *
 * <p>Every strategy reads the resource's current tags before writing the merged map back, so the read action is
 * required alongside the write one: missing only the read would let this check pass and then fail
 * mid-propagation. Keep each read next to its matching write in the role definition.</p>
 */
@Component
public class AzureEnvironmentTagUpdatePermissionValidator implements EnvironmentTagUpdatePermissionValidator {

    private static final Logger LOGGER = LoggerFactory.getLogger(AzureEnvironmentTagUpdatePermissionValidator.class);

    @Inject
    private AzureClientService azureClientService;

    @Inject
    private AzurePermissionValidator azurePermissionValidator;

    @Override
    public String supportedPlatform() {
        return CloudPlatform.AZURE.name();
    }

    @Override
    public void validate(CloudCredential cloudCredential) throws TagUpdatePermissionMissingException {
        Pair<Set<String>, Set<String>> incorrectPermissions;
        try {
            AzureClient azureClient = azureClientService.getClient(cloudCredential);
            incorrectPermissions = azurePermissionValidator.findMissingTagUpdatePermissions(azureClient);
        } catch (Exception e) {
            // Listing the role assignments needs permissions of its own; without them the SDK failure would escape
            // as HTTP 500 and bypass the structured permission payload the caller reads.
            LOGGER.warn("Azure tag-update permission check could not run for credential '{}': {}", cloudCredential.getName(), e.getMessage(), e);
            throw new TagUpdatePermissionMissingException(
                    String.format("Failed to verify the tag-update permissions of CDP Credential '%s': %s", cloudCredential.getName(), e.getMessage()), e);
        }
        Set<String> missingActions = incorrectPermissions.getLeft();
        Set<String> deniedActions = incorrectPermissions.getRight();
        if (!missingActions.isEmpty() || !deniedActions.isEmpty()) {
            List<String> failedActions = new ArrayList<>(missingActions);
            deniedActions.stream()
                    .filter(action -> !missingActions.contains(action))
                    .forEach(failedActions::add);
            String message = buildMessage(cloudCredential, missingActions, deniedActions);
            LOGGER.info("Azure tag-update permission check failed for credential '{}': {}", cloudCredential.getName(), message);
            throw new TagUpdatePermissionMissingException(message, failedActions, null);
        }
    }

    private String buildMessage(CloudCredential cloudCredential, Set<String> missingActions, Set<String> deniedActions) {
        StringBuilder message = new StringBuilder(String.format("CDP Credential '%s' cannot update the tags of the existing Azure resources.",
                cloudCredential.getName()));
        if (!deniedActions.isEmpty()) {
            message.append(" The following required action(s) are explicitly denied in your role definition (in 'notActions' section): ")
                    .append(String.join(", ", sorted(deniedActions)))
                    .append('.');
        }
        if (!missingActions.isEmpty()) {
            message.append(" The following required action(s) are missing from your role definition: ")
                    .append(String.join(", ", sorted(missingActions)))
                    .append('.');
        }
        return message.toString();
    }

    private List<String> sorted(Set<String> actions) {
        return actions.stream().sorted().toList();
    }
}
