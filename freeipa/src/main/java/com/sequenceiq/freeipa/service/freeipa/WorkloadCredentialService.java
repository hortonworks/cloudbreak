package com.sequenceiq.freeipa.service.freeipa;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;

import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.cloudera.thunderhead.service.usermanagement.UserManagementProto;
import com.google.common.collect.Lists;
import com.sequenceiq.cloudbreak.util.CheckedSupplier;
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
            BiConsumer<String, String> warnings) throws FreeIpaClientException, IOException {
        List<Object> operations = Lists.newArrayList();
        for (Map.Entry<String, WorkloadCredential> entry : workloadCredentials.entrySet()) {
            String username = entry.getKey();
            WorkloadCredential workloadCredential = entry.getValue();
            String ansEncodedKrbPrincipalKey = KrbKeySetEncoder.getASNEncodedKrbPrincipalKey(workloadCredential.getKeys());
            CheckedSupplier<Pair<List<Object>, Map<String, Object>>, FreeIpaClientException> flagsAndParams = () ->
                    freeIpaClient.getSetWlCredentialsFlagsAndParams(username,
                            workloadCredential.getHashedPassword(), ansEncodedKrbPrincipalKey, workloadCredential.getExpirationDate(),
                            workloadCredential.getSshPublicKeys().stream().map(UserManagementProto.SshPublicKey::getPublicKey).collect(Collectors.toList()));
            FreeIpaClient.fillInOperations(operations, FreeIpaClient.METHOD_NAME_USER_MOD, flagsAndParams);
        }
        freeIpaClient.callBatch(warnings, operations);
    }
}