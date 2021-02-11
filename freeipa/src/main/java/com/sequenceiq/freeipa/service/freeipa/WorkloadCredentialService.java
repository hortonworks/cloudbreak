package com.sequenceiq.freeipa.service.freeipa;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.cloudera.thunderhead.service.usermanagement.UserManagementProto;
import com.google.common.collect.Lists;
import com.sequenceiq.freeipa.client.FreeIpaClient;
import com.sequenceiq.freeipa.client.FreeIpaClientException;
import com.sequenceiq.freeipa.client.operation.SetWlCredentialOperation;
import com.sequenceiq.freeipa.configuration.BatchPartitionSizeProperties;
import com.sequenceiq.freeipa.service.freeipa.user.conversion.UserMetadataConverter;
import com.sequenceiq.freeipa.service.freeipa.user.kerberos.KrbKeySetEncoder;
import com.sequenceiq.freeipa.service.freeipa.user.model.UserMetadata;
import com.sequenceiq.freeipa.service.freeipa.user.model.WorkloadCredential;

@Service
public class WorkloadCredentialService {
    private static final Logger LOGGER = LoggerFactory.getLogger(WorkloadCredentialService.class);

    @Inject
    private BatchPartitionSizeProperties batchPartitionSizeProperties;

    @Inject
    private UserMetadataConverter userMetadataConverter;

    public void setWorkloadCredential(boolean credentialsUpdateOptimizationEnabled, FreeIpaClient freeIpaClient, String username, String userCrn,
            WorkloadCredential workloadCredential) throws IOException, FreeIpaClientException {
        boolean updateCredentials = true;
        if (credentialsUpdateOptimizationEnabled) {
            Optional<com.sequenceiq.freeipa.client.model.User> ipaUser = freeIpaClient.userFind(username);
            if (ipaUser.isPresent()) {
                Optional<UserMetadata> userMetadata = userMetadataConverter.toUserMetadata(ipaUser.get());
                if (userMetadata.isPresent() && userMetadata.get().getWorkloadCredentialsVersion() >= workloadCredential.getVersion()) {
                    updateCredentials = false;
                }
            }
        }

        if (updateCredentials) {
            LOGGER.debug("Setting workload credentials for user '{}'", username);
            getOperation(username, userCrn, workloadCredential, credentialsUpdateOptimizationEnabled, freeIpaClient).invoke(freeIpaClient);
        } else {
            LOGGER.debug("Not setting workload credentials for user '{}' because credentials are already up to date", username);
        }
    }

    public void setWorkloadCredentials(boolean fmsToFreeipaBatchCallEnabled, boolean credentialsUpdateOptimizationEnabled, FreeIpaClient freeIpaClient,
            Map<String, WorkloadCredential> workloadCredentials, Set<String> usersWithCredentialsToUpdate, Map<String, String> userToCrnMap,
            BiConsumer<String, String> warnings) throws FreeIpaClientException {
        List<SetWlCredentialOperation> operations = Lists.newArrayList();
        usersWithCredentialsToUpdate.forEach(username -> {
            try {
                operations.add(getOperation(username, userToCrnMap.get(username), workloadCredentials.get(username), credentialsUpdateOptimizationEnabled,
                        freeIpaClient));
            } catch (IOException e) {
                recordWarning(username, e, warnings);
            }
        });

        if (!operations.isEmpty()) {
            if (fmsToFreeipaBatchCallEnabled) {
                List<Object> batchCallOperations = operations.stream().map(operation ->
                        operation.getOperationParamsForBatchCall()).collect(Collectors.toList());
                String operationName = operations.stream().map(op -> op.getOperationName()).findFirst().orElse("unknown");
                freeIpaClient.callBatch(warnings, batchCallOperations, batchPartitionSizeProperties.getByOperation(operationName), Set.of());
            } else {
                for (SetWlCredentialOperation operation : operations) {
                    try {
                        operation.invoke(freeIpaClient);
                    } catch (FreeIpaClientException e) {
                        recordWarning(operation.getUser(), e, warnings);
                        freeIpaClient.checkIfClientStillUsable(e);
                    }
                }
            }
        }
    }

    private SetWlCredentialOperation getOperation(String user, String crn, WorkloadCredential workloadCredential, boolean credentialsUpdateOptimizationEnabled,
            FreeIpaClient freeIpaClient) throws IOException {
        String expiration = freeIpaClient.formatDate(workloadCredential.getExpirationDate());
        String asnEncodedKrbPrincipalKey = KrbKeySetEncoder.getASNEncodedKrbPrincipalKey(workloadCredential.getKeys());
        List<String> sshPublicKeys = workloadCredential.getSshPublicKeys().stream()
                .map(UserManagementProto.SshPublicKey::getPublicKey).collect(Collectors.toList());

        if (credentialsUpdateOptimizationEnabled) {
            if (StringUtils.isBlank(crn)) {
                // The user's CRN is gotten from the UMS, not the IPA, and so should always be present.
                throw new IllegalStateException(String.format("Missing CRN for user %s ", user));
            }
            String userMetadataJson = userMetadataConverter.toUserMetadataJson(crn, workloadCredential.getVersion());
            return SetWlCredentialOperation.create(user, workloadCredential.getHashedPassword(), asnEncodedKrbPrincipalKey,
                    sshPublicKeys, expiration, userMetadataJson);
        } else {
            // Do not set the title attribute if the credentials update optimization is not enabled.
            return SetWlCredentialOperation.create(user, workloadCredential.getHashedPassword(), asnEncodedKrbPrincipalKey,
                    sshPublicKeys, expiration);
        }
    }

    private void recordWarning(String username, Exception e, BiConsumer<String, String> warnings) {
        LOGGER.warn("Failed to set workload credentials for user '{}'", username, e);
        warnings.accept(username, "Failed to set workload credentials:" + e.getMessage());
    }
}