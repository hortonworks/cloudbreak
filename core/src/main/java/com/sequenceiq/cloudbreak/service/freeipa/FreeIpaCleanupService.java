package com.sequenceiq.cloudbreak.service.freeipa;

import static com.sequenceiq.freeipa.api.v1.freeipa.cleanup.CleanupStep.REMOVE_HOSTS;
import static com.sequenceiq.freeipa.api.v1.freeipa.cleanup.CleanupStep.REMOVE_ROLES;
import static com.sequenceiq.freeipa.api.v1.freeipa.cleanup.CleanupStep.REMOVE_USERS;
import static com.sequenceiq.freeipa.api.v1.freeipa.cleanup.CleanupStep.REMOVE_VAULT_ENTRIES;
import static com.sequenceiq.freeipa.api.v1.freeipa.cleanup.CleanupStep.REVOKE_CERTS;

import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import javax.inject.Inject;
import javax.ws.rs.WebApplicationException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.auth.crn.Crn;
import com.sequenceiq.cloudbreak.auth.crn.RegionAwareInternalCrnGeneratorFactory;
import com.sequenceiq.cloudbreak.common.exception.WebApplicationExceptionMessageExtractor;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.dto.KerberosConfig;
import com.sequenceiq.cloudbreak.kerberos.KerberosConfigService;
import com.sequenceiq.cloudbreak.polling.ExtendedPollingResult;
import com.sequenceiq.cloudbreak.polling.PollingService;
import com.sequenceiq.cloudbreak.service.environment.EnvironmentConfigProvider;
import com.sequenceiq.cloudbreak.template.kerberos.KerberosDetailService;
import com.sequenceiq.freeipa.api.v1.freeipa.cleanup.CleanupRequest;
import com.sequenceiq.freeipa.api.v1.freeipa.cleanup.CleanupStep;
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

    private static final Set<CleanupStep> STEPS_TO_SKIP_ON_RECOVER = Set.of(REMOVE_HOSTS, REMOVE_VAULT_ENTRIES, REMOVE_USERS, REMOVE_ROLES);

    private static final Set<CleanupStep> STEPS_TO_SKIP_ON_SCALE = Set.of(REMOVE_USERS, REMOVE_ROLES);

    private static final Set<CleanupStep> STEPS_TO_SKIP_WHEN_DNS_ONLY = Set.of(REMOVE_HOSTS, REMOVE_VAULT_ENTRIES, REMOVE_USERS, REVOKE_CERTS, REMOVE_ROLES);

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

    @Inject
    private WebApplicationExceptionMessageExtractor exceptionMessageExtractor;

    @Inject
    private InstanceMetadataProcessor instanceMetadataProcessor;

    @Inject
    private RegionAwareInternalCrnGeneratorFactory regionAwareInternalCrnGeneratorFactory;

    public void cleanupButIp(Stack stack) {
        Set<String> hostNames = instanceMetadataProcessor.extractFqdn(stack);
        Set<String> ips = Set.of();
        LOGGER.info("Full cleanup invoked with hostnames {} and IPs {}", hostNames, ips);
        cleanup(stack, Set.of(), hostNames, ips);
    }

    public void cleanupOnScale(Stack stack, Set<String> hostNames, Set<String> ips) {
        validateCleanupParametersSet(hostNames, ips);
        LOGGER.info("Cleanup on scale invoked with hostnames {} and IPs {}", hostNames, ips);
        cleanup(stack, STEPS_TO_SKIP_ON_SCALE, hostNames, ips);
    }

    public void cleanupOnRecover(Stack stack, Set<String> hostNames, Set<String> ips) {
        validateCleanupParametersSet(hostNames, ips);
        LOGGER.info("Cleanup on recover invoked with hostnames {} and IPs {}", hostNames, ips);
        cleanup(stack, STEPS_TO_SKIP_ON_RECOVER, hostNames, ips);
    }

    public void cleanupDnsOnly(Stack stack, Set<String> hostNames, Set<String> ips) {
        validateCleanupParametersSet(hostNames, ips);
        LOGGER.info("DNS only cleanup invoked with hostnames {} and IPs {}", hostNames, ips);
        cleanup(stack, STEPS_TO_SKIP_WHEN_DNS_ONLY, hostNames, ips);
    }

    private void validateCleanupParametersSet(Set<String> hostNames, Set<String> ips) {
        Objects.requireNonNull(hostNames, "Hostnames must be set");
        Objects.requireNonNull(ips, "IPs must be set");
    }

    private void cleanup(Stack stack, Set<CleanupStep> stepsToSkip, Set<String> hostNames, Set<String> ips) {
        Optional<KerberosConfig> kerberosConfig = kerberosConfigService.get(stack.getEnvironmentCrn(), stack.getName());
        boolean childEnvironment = environmentConfigProvider.isChildEnvironment(stack.getEnvironmentCrn());

        if (kerberosDetailService.keytabsShouldBeUpdated(stack.cloudPlatform(), childEnvironment, kerberosConfig)) {
            OperationStatus operationStatus = sendCleanupRequest(stack, stepsToSkip, hostNames, ips);
            pollCleanupOperation(operationStatus, Crn.safeFromString(stack.getResourceCrn()).getAccountId());
        }
    }

    private void pollCleanupOperation(OperationStatus operationStatus, String accountId) {
        FreeIpaOperationPollerObject opretaionPollerObject = new FreeIpaOperationPollerObject(operationStatus.getOperationId(),
                operationStatus.getOperationType().name(), operationV1Endpoint, accountId, regionAwareInternalCrnGeneratorFactory);
        ExtendedPollingResult pollingResult = freeIpaOperationChecker
                .pollWithAbsoluteTimeout(new FreeIpaOperationCheckerTask<>(), opretaionPollerObject, POLL_INTERVAL, WAIT_SEC, 1);
        if (!pollingResult.isSuccess()) {
            Exception ex = pollingResult.getException();
            LOGGER.error("Cleanup failed with state [{}] and message: [{}]", pollingResult.getPollingResult(), ex.getMessage());
            throw new FreeIpaOperationFailedException(
                    String.format("Cleanup failed with state [%s] and message: [%s]", pollingResult.getPollingResult(), ex.getMessage()), ex);
        }
    }

    private OperationStatus sendCleanupRequest(Stack stack, Set<CleanupStep> stepsToSkip, Set<String> hostNames, Set<String> ips) {
        try {
            CleanupRequest cleanupRequest = createCleanupRequest(stack, stepsToSkip, hostNames, ips);
            LOGGER.info("Sending cleanup request to FreeIPA: [{}]", cleanupRequest);
            OperationStatus cleanup = ThreadBasedUserCrnProvider.doAsInternalActor(
                    regionAwareInternalCrnGeneratorFactory.iam().getInternalCrnForServiceAsString(),
                    () ->
                    freeIpaV1Endpoint.internalCleanup(cleanupRequest, Crn.fromString(stack.getResourceCrn()).getAccountId()));
            LOGGER.info("Cleanup operation started: {}", cleanup);
            return cleanup;
        } catch (WebApplicationException e) {
            String errorMessage = exceptionMessageExtractor.getErrorMessage(e);
            String message = String.format("Couldn't start cleanup due to: '%s' ", errorMessage);
            LOGGER.error(message, e);
            throw new FreeIpaOperationFailedException(message, e);
        } catch (Exception e) {
            LOGGER.error("Couldn't start cleanup", e);
            throw new FreeIpaOperationFailedException("Couldn't start cleanup", e);
        }
    }

    private CleanupRequest createCleanupRequest(Stack stack, Set<CleanupStep> stepsToSkip, Set<String> hostNames, Set<String> ips) {
        CleanupRequest cleanupRequest = new CleanupRequest();
        cleanupRequest.setHosts(hostNames);
        cleanupRequest.setIps(ips);
        cleanupRequest.setEnvironmentCrn(stack.getEnvironmentCrn());
        cleanupRequest.setClusterName(stack.getName());
        cleanupRequest.setUsers(Set.of(KERBEROS_USER_PREFIX + stack.getName(), KEYTAB_USER_PREFIX + stack.getName(),
                LDAP_USER_PREFIX + stack.getName()));
        cleanupRequest.setRoles(Set.of(ROLE_NAME_PREFIX + stack.getName()));
        cleanupRequest.setCleanupStepsToSkip(stepsToSkip);
        return cleanupRequest;
    }
}
