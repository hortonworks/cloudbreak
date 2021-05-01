package com.sequenceiq.cloudbreak.cloud.gcp.validator;

import java.io.IOException;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.google.api.services.iam.v1.Iam;
import com.google.api.services.iam.v1.model.ListServiceAccountsResponse;
import com.sequenceiq.cloudbreak.cloud.gcp.client.GcpIamFactory;
import com.sequenceiq.cloudbreak.cloud.gcp.util.GcpStackUtil;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.SpiFileSystem;
import com.sequenceiq.cloudbreak.cloud.model.filesystem.CloudFileSystemView;
import com.sequenceiq.cloudbreak.cloud.model.filesystem.CloudGcsView;
import com.sequenceiq.cloudbreak.validation.ValidationResult.ValidationResultBuilder;

@Component
public class GcpServiceAccountObjectStorageValidator {

    private static final Logger LOGGER = LoggerFactory.getLogger(GcpServiceAccountObjectStorageValidator.class);

    private static final int DEFAULT_PAGE_SIZE = 50;

    @Inject
    private GcpIamFactory gcpIamFactory;

    @Inject
    private GcpStackUtil gcpStackUtil;

    public ValidationResultBuilder validateObjectStorage(CloudCredential cloudCredential,
            SpiFileSystem spiFileSystem,
            ValidationResultBuilder resultBuilder) throws IOException {
        LOGGER.info("Validating Gcp identities...");
        Iam iam = gcpIamFactory.buildIam(cloudCredential);
        List<CloudFileSystemView> cloudFileSystems = spiFileSystem.getCloudFileSystems();
        if (Objects.nonNull(cloudFileSystems) && cloudFileSystems.size() > 0) {
            String projectId = gcpStackUtil.getProjectId(cloudCredential);
            Set<String> serviceAccountEmailsToFind = cloudFileSystems
                    .stream()
                    .map(cloudFileSystemView -> ((CloudGcsView) cloudFileSystemView).getServiceAccountEmail())
                    .collect(Collectors.toSet());

            Iam.Projects.ServiceAccounts.List listServiceAccountEmailsRequest = iam
                    .projects()
                    .serviceAccounts()
                    .list("projects/" + projectId)
                    .setPageSize(DEFAULT_PAGE_SIZE);

            ListServiceAccountsResponse response;
            do {
                response = listServiceAccountEmailsRequest.execute();
                response.getAccounts()
                        .forEach(serviceAccount -> serviceAccountEmailsToFind.remove(serviceAccount.getEmail()));
                listServiceAccountEmailsRequest.setPageToken(response.getNextPageToken());
            } while (response.getNextPageToken() != null && !serviceAccountEmailsToFind.isEmpty());

            if (!serviceAccountEmailsToFind.isEmpty()) {
                addError(resultBuilder, String.format("Service Account with email(s) '%s' could not be found in the configured Google Cloud project '%s'.",
                        String.join(", ", serviceAccountEmailsToFind), projectId));
            }
        }
        return resultBuilder;
    }

    private void addError(ValidationResultBuilder resultBuilder, String msg) {
        LOGGER.info(msg);
        resultBuilder.error(msg);
    }
}
