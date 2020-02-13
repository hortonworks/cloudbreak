package com.sequenceiq.cloudbreak.service.freeipa;

import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceMetaData;
import com.sequenceiq.cloudbreak.dto.KerberosConfig;
import com.sequenceiq.cloudbreak.kerberos.KerberosConfigService;
import com.sequenceiq.cloudbreak.polling.PollingResult;
import com.sequenceiq.cloudbreak.polling.PollingService;
import com.sequenceiq.cloudbreak.service.environment.EnvironmentConfigProvider;
import com.sequenceiq.cloudbreak.template.kerberos.KerberosDetailService;
import com.sequenceiq.freeipa.api.v1.freeipa.cleanup.CleanupRequest;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.FreeIpaV1Endpoint;
import com.sequenceiq.freeipa.api.v1.operation.OperationV1Endpoint;
import com.sequenceiq.freeipa.api.v1.operation.model.OperationStatus;

@Service
public class FreeIpaCleanupService {

    private static final Logger LOGGER = LoggerFactory.getLogger(FreeIpaCleanupService.class);

    private static final String KERBEROS_USER_PREFIX = "krbbind-";

    private static final String KEYTAB_USER_PREFIX = "kerberosbind-";

    private static final String LDAP_USER_PREFIX = "ldapbind-";

    private static final String ROLE_NAME_PREFIX = "hadoopadminrole-";

    private static final int POLL_INTERVAL = 5000;

    private static final int WAIT_SEC = 600;

    @Inject
    private FreeIpaV1Endpoint freeIpaV1Endpoint;

    @Inject
    private PollingService<FreeIpaOperationPollerObject> freeIpaOperationChecker;

    @Inject
    private OperationV1Endpoint operationV1Endpoint;

    @Inject
    private KerberosConfigService kerberosConfigService;

    @Inject
    private KerberosDetailService kerberosDetailService;

    @Inject
    private EnvironmentConfigProvider environmentConfigProvider;

    public void cleanup(Stack stack, boolean hostOnly, Set<String> hostNames) {
        Optional<KerberosConfig> kerberosConfig = kerberosConfigService.get(stack.getEnvironmentCrn(), stack.getName());
        boolean childEnvironment = environmentConfigProvider.isChildEnvironment(stack.getEnvironmentCrn());

        if (kerberosDetailService.keytabsShouldBeUpdated(stack.cloudPlatform(), childEnvironment, kerberosConfig)) {
            OperationStatus operationStatus = sendCleanupRequest(stack, hostOnly, hostNames);
            pollCleanupOperation(operationStatus);
        }
    }

    private void pollCleanupOperation(OperationStatus operationStatus) {
        FreeIpaOperationPollerObject opretaionPollerObject = new FreeIpaOperationPollerObject(operationStatus.getOperationId(),
                operationStatus.getOperationType().name(), operationV1Endpoint);
        Pair<PollingResult, Exception> pollingResult = freeIpaOperationChecker
                .pollWithAbsoluteTimeout(new FreeIpaOperationCheckerTask<>(), opretaionPollerObject, POLL_INTERVAL, WAIT_SEC, 1);
        if (!PollingResult.isSuccess(pollingResult.getLeft())) {
            Exception ex = pollingResult.getRight();
            LOGGER.error("Cleanup failed with state [{}] and message: [{}]", pollingResult.getLeft(), ex.getMessage());
            throw new FreeIpaOperationFailedException(
                    String.format("Cleanup failed with state [%s] and message: [%s]", pollingResult.getLeft(), ex.getMessage(), ex));
        }
    }

    private OperationStatus sendCleanupRequest(Stack stack, boolean hostOnly, Set<String> hostNames) {
        try {
            Set<String> fqdns = stack.getInstanceMetaDataAsList().stream().map(InstanceMetaData::getDiscoveryFQDN).filter(s -> StringUtils.isNotBlank(s))
                    .collect(Collectors.toSet());
            CleanupRequest cleanupRequest = new CleanupRequest();
            cleanupRequest.setHosts(hostNames == null ? fqdns : hostNames);
            cleanupRequest.setEnvironmentCrn(stack.getEnvironmentCrn());
            if (!hostOnly) {
                cleanupRequest.setClusterName(stack.getName());
                cleanupRequest.setUsers(Set.of(KERBEROS_USER_PREFIX + stack.getName(), KEYTAB_USER_PREFIX + stack.getName(),
                        LDAP_USER_PREFIX + stack.getName()));
                cleanupRequest.setRoles(Set.of(ROLE_NAME_PREFIX + stack.getName()));
            }
            LOGGER.info("Sending cleanup request to FreeIPA: [{}]", cleanupRequest);
            OperationStatus cleanup = freeIpaV1Endpoint.cleanup(cleanupRequest);
            LOGGER.info("Cleanup operation started: {}", cleanup);
            return cleanup;
        } catch (Exception e) {
            LOGGER.error("Couldn't start cleanup", e);
            throw new FreeIpaOperationFailedException("Couldn't start cleanup", e);
        }
    }
}
