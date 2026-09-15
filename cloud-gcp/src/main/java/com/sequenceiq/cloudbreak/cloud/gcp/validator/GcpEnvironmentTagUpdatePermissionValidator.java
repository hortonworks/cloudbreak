package com.sequenceiq.cloudbreak.cloud.gcp.validator;

import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.google.api.services.cloudresourcemanager.v3.CloudResourceManager;
import com.google.api.services.cloudresourcemanager.v3.model.TestIamPermissionsRequest;
import com.google.api.services.cloudresourcemanager.v3.model.TestIamPermissionsResponse;
import com.sequenceiq.cloudbreak.cloud.EnvironmentTagUpdatePermissionValidator;
import com.sequenceiq.cloudbreak.cloud.exception.TagUpdatePermissionMissingException;
import com.sequenceiq.cloudbreak.cloud.gcp.client.GcpCloudResourceManagerFactory;
import com.sequenceiq.cloudbreak.cloud.gcp.util.GcpStackUtil;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;

/**
 * Uses the GCP project-scoped {@code testIamPermissions} primitive on Cloud Resource Manager v3 to verify
 * that the credential can invoke every permission the tag-update flow needs across Compute Engine + Cloud SQL.
 * The set is derived from the strategies under {@code com.sequenceiq.cloudbreak.cloud.gcp.tag} — keep it in
 * sync when a new tag strategy is added.
 *
 * <p>Every strategy reads the resource before mutating it (to pick up the existing labels and the label
 * fingerprint), so the read permission is required alongside the write one: missing only the read would let
 * this check pass and then fail mid-propagation. Keep each read next to its matching write below.</p>
 */
@Component
public class GcpEnvironmentTagUpdatePermissionValidator implements EnvironmentTagUpdatePermissionValidator {

    private static final Logger LOGGER = LoggerFactory.getLogger(GcpEnvironmentTagUpdatePermissionValidator.class);

    private static final List<String> REQUIRED_PERMISSIONS = List.of(
            "compute.instances.get",
            "compute.instances.setLabels",
            "compute.disks.get",
            "compute.disks.setLabels",
            "compute.addresses.get",
            "compute.addresses.setLabels",
            "compute.forwardingRules.get",
            "compute.forwardingRules.setLabels",
            "cloudsql.instances.get",
            "cloudsql.instances.update");

    @Inject
    private GcpCloudResourceManagerFactory cloudResourceManagerFactory;

    @Inject
    private GcpStackUtil gcpStackUtil;

    @Override
    public String supportedPlatform() {
        return CloudPlatform.GCP.name();
    }

    @Override
    public void validate(CloudCredential cloudCredential) throws TagUpdatePermissionMissingException {
        String projectId = gcpStackUtil.getProjectId(cloudCredential);
        CloudResourceManager cloudResourceManager = cloudResourceManagerFactory.buildCloudResourceManager(cloudCredential);
        TestIamPermissionsRequest request = new TestIamPermissionsRequest().setPermissions(REQUIRED_PERMISSIONS);
        try {
            TestIamPermissionsResponse response = cloudResourceManager.projects()
                    .testIamPermissions("projects/" + projectId, request)
                    .execute();
            Set<String> granted = response.getPermissions() == null ? Set.of() : new HashSet<>(response.getPermissions());
            List<String> missing = REQUIRED_PERMISSIONS.stream()
                    .filter(p -> !granted.contains(p))
                    .toList();
            if (!missing.isEmpty()) {
                String message = "One or more tag-update permissions are missing on project '" + projectId + "': " + String.join(", ", missing);
                LOGGER.info("GCP tag-update permission check failed for credential '{}': {}", cloudCredential.getName(), message);
                throw new TagUpdatePermissionMissingException(message, missing, null);
            }
        } catch (IOException e) {
            LOGGER.warn("GCP testIamPermissions call failed for credential '{}': {}", cloudCredential.getName(), e.getMessage());
            throw new TagUpdatePermissionMissingException(
                    "Failed to verify tag-update permissions on GCP project '" + projectId + "': " + e.getMessage(), e);
        }
    }
}
