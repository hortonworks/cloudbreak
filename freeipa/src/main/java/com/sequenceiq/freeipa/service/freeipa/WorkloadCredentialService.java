package com.sequenceiq.freeipa.service.freeipa;

import static com.google.common.base.Preconditions.checkArgument;
import static java.util.Objects.requireNonNull;

import java.io.IOException;
import java.util.List;
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
import com.sequenceiq.freeipa.service.freeipa.user.model.UserSyncOptions;
import com.sequenceiq.freeipa.service.freeipa.user.model.WorkloadCredential;

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
            List<SetWlCredentialOperation> operations = Lists.newArrayList();
            updates.forEach(update -> {
                try {
                    operations.add(getOperation(update.getUsername(), update.getUserCrn(), update.getWorkloadCredential(),
                            options.credentialsUpdateOptimizationEnabled(), freeIpaClient));
                } catch (IOException e) {
                    recordWarning(update.getUsername(), e, warnings);
                }
            });

            if (options.fmsToFreeIpaBatchCallEnabled()) {
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

    public static class WorkloadCredentialUpdate {

        private final String username;

        private final String userCrn;

        private final WorkloadCredential workloadCredential;

        public WorkloadCredentialUpdate(String username, String userCrn, WorkloadCredential workloadCredential) {
            checkArgument(!StringUtils.isBlank(username), "username must not be blank");
            this.username = username;
            checkArgument(!StringUtils.isBlank(userCrn), "user CRN must not be blank");
            this.userCrn = userCrn;
            this.workloadCredential = requireNonNull(workloadCredential, "workload credential must not be null");
        }

        public String getUsername() {
            return username;
        }

        public String getUserCrn() {
            return userCrn;
        }

        public WorkloadCredential getWorkloadCredential() {
            return workloadCredential;
        }
    }
}