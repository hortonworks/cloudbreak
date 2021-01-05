package com.sequenceiq.freeipa.service.freeipa;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.cloudera.thunderhead.service.usermanagement.UserManagementProto;
import com.google.common.collect.Lists;
import com.sequenceiq.freeipa.configuration.BatchPartitionSizeProperties;
import com.sequenceiq.freeipa.client.FreeIpaClient;
import com.sequenceiq.freeipa.client.FreeIpaClientException;
import com.sequenceiq.freeipa.client.FreeIpaClientExceptionUtil;
import com.sequenceiq.freeipa.client.operation.SetWlCredentialOperation;
import com.sequenceiq.freeipa.service.freeipa.user.kerberos.KrbKeySetEncoder;
import com.sequenceiq.freeipa.service.freeipa.user.model.WorkloadCredential;

@Service
public class WorkloadCredentialService {
    private static final Logger LOGGER = LoggerFactory.getLogger(WorkloadCredentialService.class);

    @Inject
    private BatchPartitionSizeProperties batchPartitionSizeProperties;

    public void setWorkloadCredential(FreeIpaClient freeIpaClient, String username, WorkloadCredential workloadCredential)
            throws IOException, FreeIpaClientException {
        LOGGER.debug("Setting workload credentials for user '{}'", username);
        try {
            getOperation(username, workloadCredential, freeIpaClient).invoke(freeIpaClient);
        } catch (FreeIpaClientException e) {
            if (FreeIpaClientExceptionUtil.isEmptyModlistException(e)) {
                LOGGER.debug("Workload credentials for user '{}' already set.", username);
            } else {
                throw e;
            }
        }
    }

    public void setWorkloadCredentials(boolean fmsToFreeipaBatchCallEnabled, FreeIpaClient freeIpaClient, Map<String, WorkloadCredential> workloadCredentials,
            BiConsumer<String, String> warnings) throws FreeIpaClientException, IOException {
        List<SetWlCredentialOperation> operations = Lists.newArrayList();
        for (Map.Entry<String, WorkloadCredential> entry : workloadCredentials.entrySet()) {
            String username = entry.getKey();
            WorkloadCredential workloadCredential = entry.getValue();
            operations.add(getOperation(username, workloadCredential, freeIpaClient));
        }
        if (fmsToFreeipaBatchCallEnabled) {
            List<Object> batchCallOperations = operations.stream().map(operation -> operation.getOperationParamsForBatchCall()).collect(Collectors.toList());
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

    private SetWlCredentialOperation getOperation(String user, WorkloadCredential workloadCredential, FreeIpaClient freeIpaClient) throws IOException {
        String expiration = freeIpaClient.formatDate(workloadCredential.getExpirationDate());
        String asnEncodedKrbPrincipalKey = KrbKeySetEncoder.getASNEncodedKrbPrincipalKey(workloadCredential.getKeys());
        List<String> sshPublicKeys = workloadCredential.getSshPublicKeys().stream()
                .map(UserManagementProto.SshPublicKey::getPublicKey).collect(Collectors.toList());
        return SetWlCredentialOperation.create(user, workloadCredential.getHashedPassword(), asnEncodedKrbPrincipalKey, sshPublicKeys, expiration);
    }

    private void recordWarning(String username, Exception e, BiConsumer<String, String> warnings) {
        LOGGER.warn("Failed to set workload credentials for user '{}'", username, e);
        warnings.accept(username, "Failed to set workload credentials:" + e.getMessage());
    }
}