package com.sequenceiq.freeipa.service.freeipa;

import java.io.IOException;
import java.util.List;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.cloudera.thunderhead.service.usermanagement.UserManagementProto;
import com.sequenceiq.freeipa.client.FreeIpaClient;
import com.sequenceiq.freeipa.client.FreeIpaClientException;
import com.sequenceiq.freeipa.client.operation.AbstractFreeipaOperation;
import com.sequenceiq.freeipa.client.operation.SetWlCredentialOperation;
import com.sequenceiq.freeipa.client.operation.UserModOperation;
import com.sequenceiq.freeipa.configuration.BatchPartitionSizeProperties;
import com.sequenceiq.freeipa.service.freeipa.user.conversion.UserMetadataConverter;
import com.sequenceiq.freeipa.service.freeipa.user.kerberos.KrbKeySetEncoder;
import com.sequenceiq.freeipa.service.freeipa.user.model.UserSyncOptions;
import com.sequenceiq.freeipa.service.freeipa.user.model.WorkloadCredential;
import com.sequenceiq.freeipa.service.freeipa.user.model.WorkloadCredentialUpdate;

@Service
public class WorkloadCredentialService {
    private static final Logger LOGGER = LoggerFactory.getLogger(WorkloadCredentialService.class);

    @Inject
    private BatchPartitionSizeProperties batchPartitionSizeProperties;

    @Inject
    private UserMetadataConverter userMetadataConverter;

    public void setWorkloadCredential(boolean credentialsUpdateOptimizationEnabled, FreeIpaClient freeIpaClient, WorkloadCredentialUpdate update)
            throws IOException, FreeIpaClientException {
        LOGGER.debug("Setting workload credentials for user '{}'", update.getUsername());
        getOperation(update.getUsername(), update.getUserCrn(), update.getWorkloadCredential(), credentialsUpdateOptimizationEnabled, freeIpaClient)
                .invoke(freeIpaClient);
    }

    public void setWorkloadCredentials(UserSyncOptions options, FreeIpaClient freeIpaClient, Set<WorkloadCredentialUpdate> updates,
            BiConsumer<String, String> warnings) throws FreeIpaClientException {
        if (!updates.isEmpty()) {
            List<SetWlCredentialOperation> operations = updates.stream()
                    .flatMap(update -> createUpdateOperation(options, freeIpaClient, warnings, update))
                    .collect(Collectors.toList());
            if (options.isFmsToFreeIpaBatchCallEnabled()) {
                updateInBatch(freeIpaClient, warnings, operations);
            } else {
                updateOneByOne(freeIpaClient, warnings, operations);
            }
        } else {
            LOGGER.debug("No workload credentials to update");
        }
    }

    private void updateOneByOne(FreeIpaClient freeIpaClient,  BiConsumer<String, String> warnings, List<SetWlCredentialOperation> operations)
            throws FreeIpaClientException {
        LOGGER.debug("Updating workload credentials one by one");
        for (SetWlCredentialOperation operation : operations) {
            try {
                operation.invoke(freeIpaClient);
            } catch (FreeIpaClientException e) {
                recordWarning(operation.getUser(), e, warnings);
                freeIpaClient.checkIfClientStillUsable(e);
            }
        }
    }

    private void updateInBatch(FreeIpaClient freeIpaClient,  BiConsumer<String, String> warnings, List<SetWlCredentialOperation> operations)
            throws FreeIpaClientException {
        LOGGER.debug("Updating workload credentials in batches");
        List<Object> batchCallOperations = operations.stream()
                .map(AbstractFreeipaOperation::getOperationParamsForBatchCall)
                .collect(Collectors.toList());
        String operationName = operations.stream()
                .map(UserModOperation::getOperationName)
                .findFirst().orElse("unknown");
        freeIpaClient.callBatch(warnings, batchCallOperations, batchPartitionSizeProperties.getByOperation(operationName), Set.of());
    }

    private Stream<SetWlCredentialOperation> createUpdateOperation(UserSyncOptions options, FreeIpaClient freeIpaClient, BiConsumer<String, String> warnings,
            WorkloadCredentialUpdate update) {
        try {
            return Stream.of(getOperation(update.getUsername(), update.getUserCrn(), update.getWorkloadCredential(),
                    options.isCredentialsUpdateOptimizationEnabled(), freeIpaClient));
        } catch (IOException e) {
            recordWarning(update.getUsername(), e, warnings);
            return Stream.empty();
        }
    }

    private SetWlCredentialOperation getOperation(String user, String crn, WorkloadCredential workloadCredential, boolean credentialsUpdateOptimizationEnabled,
            FreeIpaClient freeIpaClient) throws IOException {
        String expiration = freeIpaClient.formatDate(workloadCredential.getExpirationDate());
        String asnEncodedKrbPrincipalKey = KrbKeySetEncoder.getASNEncodedKrbPrincipalKey(workloadCredential.getKeys());
        List<String> sshPublicKeys = workloadCredential.getSshPublicKeys().stream()
                .map(UserManagementProto.SshPublicKey::getPublicKey).collect(Collectors.toList());

        if (credentialsUpdateOptimizationEnabled) {
            String userMetadataJson = userMetadataConverter.toUserMetadataJson(crn, workloadCredential.getVersion());
            return SetWlCredentialOperation.create(user, workloadCredential.getHashedPassword(), asnEncodedKrbPrincipalKey,
                    sshPublicKeys, expiration, userMetadataJson);
        } else {
            return SetWlCredentialOperation.create(user, workloadCredential.getHashedPassword(), asnEncodedKrbPrincipalKey,
                    sshPublicKeys, expiration);
        }
    }

    private void recordWarning(String username, Exception e, BiConsumer<String, String> warnings) {
        LOGGER.warn("Failed to set workload credentials for user '{}'", username, e);
        warnings.accept(username, "Failed to set workload credentials:" + e.getMessage());
    }
}