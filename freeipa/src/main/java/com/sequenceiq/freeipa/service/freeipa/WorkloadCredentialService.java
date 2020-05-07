package com.sequenceiq.freeipa.service.freeipa;

import java.io.IOException;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.cloudera.thunderhead.service.usermanagement.UserManagementProto;
import com.sequenceiq.freeipa.client.FreeIpaClient;
import com.sequenceiq.freeipa.client.FreeIpaClientException;
import com.sequenceiq.freeipa.client.FreeIpaClientExceptionUtil;
import com.sequenceiq.freeipa.service.freeipa.user.kerberos.KrbKeySetEncoder;
import com.sequenceiq.freeipa.service.freeipa.user.model.WorkloadCredential;

@Service
public class WorkloadCredentialService {
    private static final Logger LOGGER = LoggerFactory.getLogger(WorkloadCredentialService.class);

    public void setWorkloadCredential(FreeIpaClient freeIpaClient, String username, WorkloadCredential workloadCredential)
            throws IOException, FreeIpaClientException {
        LOGGER.debug("Setting workload credentials for user '{}'", username);

        try {
            String ansEncodedKrbPrincipalKey = KrbKeySetEncoder.getASNEncodedKrbPrincipalKey(workloadCredential.getKeys());
            freeIpaClient.userSetWorkloadCredentials(username,
                    workloadCredential.getHashedPassword(), ansEncodedKrbPrincipalKey, workloadCredential.getExpirationDate(),
                    workloadCredential.getSshPublicKeys().stream().map(UserManagementProto.SshPublicKey::getPublicKey).collect(Collectors.toList()));
        } catch (FreeIpaClientException e) {
            if (FreeIpaClientExceptionUtil.isEmptyModlistException(e)) {
                LOGGER.debug("Workload credentials for user '{}' already set.", username);
            } else {
                throw e;
            }
        }
    }

    public void setWorkloadCredentials(FreeIpaClient freeIpaClient, Map<String, WorkloadCredential> workloadCredentials,
            BiConsumer<String, String> warnings) throws FreeIpaClientException {
        for (Map.Entry<String, WorkloadCredential> entry : workloadCredentials.entrySet()) {
            try {
                setWorkloadCredential(freeIpaClient, entry.getKey(), entry.getValue());
            } catch (IOException e) {
                recordWarning(entry.getKey(), e, warnings);
            } catch (FreeIpaClientException e) {
                recordWarning(entry.getKey(), e, warnings);
                if (e.isClientUnusable()) {
                    LOGGER.warn("Client is not usable for further usage");
                    throw e;
                }
            }
        }
    }

    private void recordWarning(String username, Exception e, BiConsumer<String, String> warnings) {
        LOGGER.warn("Failed to set workload credentials for user '{}'", username, e);
        warnings.accept(username, "Failed to set workload credentials:" + e.getMessage());
    }
}
