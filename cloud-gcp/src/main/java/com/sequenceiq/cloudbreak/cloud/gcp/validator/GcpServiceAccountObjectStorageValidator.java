package com.sequenceiq.cloudbreak.cloud.gcp.validator;

import java.io.IOException;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.google.api.services.iam.v1.Iam;
import com.google.api.services.iam.v1.model.ListServiceAccountsResponse;
import com.google.api.services.iam.v1.model.ServiceAccount;
import com.sequenceiq.cloudbreak.cloud.gcp.util.GcpStackUtil;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.SpiFileSystem;
import com.sequenceiq.cloudbreak.cloud.model.filesystem.CloudFileSystemView;
import com.sequenceiq.cloudbreak.cloud.model.filesystem.CloudGcsView;
import com.sequenceiq.cloudbreak.validation.ValidationResult.ValidationResultBuilder;

@Component
public class GcpServiceAccountObjectStorageValidator {

    private static final Logger LOGGER = LoggerFactory.getLogger(GcpServiceAccountObjectStorageValidator.class);

    public ValidationResultBuilder validateObjectStorage(CloudCredential cloudCredential,
        SpiFileSystem spiFileSystem,
        ValidationResultBuilder resultBuilder) throws IOException {
        LOGGER.info("Validating Gcp identities...");
        Iam iam = GcpStackUtil.buildIam(cloudCredential);
        List<CloudFileSystemView> cloudFileSystems = spiFileSystem.getCloudFileSystems();
        if (Objects.nonNull(cloudFileSystems) && cloudFileSystems.size() > 0) {
            String projectId = GcpStackUtil.getProjectId(cloudCredential);

            ListServiceAccountsResponse listServiceAccountsResponse = iam
                    .projects()
                    .serviceAccounts()
                    .list("projects/" + projectId)
                    .execute();

            for (CloudFileSystemView cloudFileSystemView : cloudFileSystems) {
                CloudGcsView cloudFileSystem = (CloudGcsView) cloudFileSystemView;
                Optional<ServiceAccount> serviceAccount = listServiceAccountsResponse.getAccounts()
                        .stream()
                        .filter(e -> e.getEmail().equals(cloudFileSystem.getServiceAccountEmail()))
                        .findFirst();

                if (serviceAccount.isEmpty()) {
                    addError(resultBuilder, String.format("Service Account with email %s does not exist in the given Google Cloud project.",
                            cloudFileSystem.getServiceAccountEmail()));
                }
            }

        }
        return resultBuilder;
    }

    private void addError(ValidationResultBuilder resultBuilder, String msg) {
        LOGGER.info(msg);
        resultBuilder.error(msg);
    }
}
