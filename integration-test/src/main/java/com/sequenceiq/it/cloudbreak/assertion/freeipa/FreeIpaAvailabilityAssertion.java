package com.sequenceiq.it.cloudbreak.assertion.freeipa;

import static com.sequenceiq.freeipa.api.v1.operation.model.OperationState.COMPLETED;
import static com.sequenceiq.freeipa.api.v1.operation.model.OperationState.RUNNING;
import static com.sequenceiq.it.cloudbreak.context.RunningParameter.doNotWaitForFlow;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import jakarta.inject.Inject;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.instancegroup.instancemetadata.InstanceMetaDataV4Response;
import com.sequenceiq.cloudbreak.auth.crn.Crn;
import com.sequenceiq.cloudbreak.polling.AbsolutTimeBasedTimeoutChecker;
import com.sequenceiq.freeipa.api.v1.dns.model.AddDnsARecordRequest;
import com.sequenceiq.freeipa.api.v1.dns.model.AddDnsCnameRecordRequest;
import com.sequenceiq.freeipa.api.v1.freeipa.cleanup.CleanupRequest;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.binduser.BindUserCreateRequest;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.instance.InstanceMetaDataResponse;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.describe.DescribeFreeIpaResponse;
import com.sequenceiq.freeipa.api.v1.freeipa.user.model.SyncOperationStatus;
import com.sequenceiq.freeipa.api.v1.freeipa.user.model.SynchronizationStatus;
import com.sequenceiq.freeipa.api.v1.freeipa.user.model.SynchronizeAllUsersRequest;
import com.sequenceiq.freeipa.api.v1.freeipa.user.model.WorkloadCredentialsUpdateType;
import com.sequenceiq.freeipa.api.v1.kerberosmgmt.model.HostKeytabRequest;
import com.sequenceiq.freeipa.api.v1.kerberosmgmt.model.ServiceKeytabRequest;
import com.sequenceiq.freeipa.api.v1.operation.model.OperationStatus;
import com.sequenceiq.it.cloudbreak.assertion.Assertion;
import com.sequenceiq.it.cloudbreak.cloud.HostGroupType;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.freeipa.FreeIpaOperationStatusTestDto;
import com.sequenceiq.it.cloudbreak.dto.freeipa.FreeIpaTestDto;
import com.sequenceiq.it.cloudbreak.dto.sdx.SdxTestDto;
import com.sequenceiq.it.cloudbreak.exception.TestFailException;
import com.sequenceiq.it.cloudbreak.microservice.FreeIpaClient;
import com.sequenceiq.it.cloudbreak.util.ssh.action.SshJClientActions;
import com.sequenceiq.sdx.api.model.SdxClusterDetailResponse;

@Component
public class FreeIpaAvailabilityAssertion {

    private static final Logger LOGGER = LoggerFactory.getLogger(FreeIpaAvailabilityAssertion.class);

    private static final String CHECK_DNS_LOOKUPS_CMD = "ping -c 2 %s | grep -q '%s'";

    private static final String CHECK_RECORD = "sudo cat /srv/pillar/freeipa/init.sls " +
            "| grep -v json " +
            "| jq -r '.freeipa.password' " +
            "| kinit admin " +
            "&& ipa dnsrecord-show %s %s --raw" +
            "|grep cnamerecord" +
            "| awk '{print $2}' " +
            "| sed 's/\\.$//'";

    private static final String CHECK_UNBOUND = "grep \"forward-addr:\" /etc/unbound/conf.d/60-domain-dns.conf " +
            "| awk 'NR==1{print $2}'";

    private static final String CHECK_LB = "dig %s A +short";

    private static final int MAX_TOLERABLE_ERRORCOUNT = 3;

    private static final Duration ASSERTION_ERROR_SLEEPDURATION = Duration.ofSeconds(5);

    @Value("${integrationtest.freeipaAvailability.operationTimeoutInMinutes:5}")
    private Integer operationTimeoutInMinutes;

    @Inject
    private SshJClientActions sshJClientActions;

    public Assertion<FreeIpaTestDto, FreeIpaClient> available() {
        return (testContext, testDto, client) -> {
            try {
                com.sequenceiq.freeipa.api.client.FreeIpaClient ipaClient = client.getDefaultClient(testContext);
                String environmentCrn = testDto.getResponse().getEnvironmentCrn();
                String accountId = Crn.safeFromString(environmentCrn).getAccountId();

                addAndDeleteDnsARecord(ipaClient, environmentCrn);
                addAndDeleteDnsCnameRecord(ipaClient, environmentCrn);
                createBindUser(testContext, ipaClient, environmentCrn);
                generateHostKeyTab(ipaClient, environmentCrn);
                generateServiceKeytab(ipaClient, environmentCrn);
                dnsLookups(testContext.get(SdxTestDto.class));
                cleanUp(testContext, ipaClient, environmentCrn);
                syncUsers(testContext, ipaClient, environmentCrn, accountId);
            } catch (TestFailException e) {
                throw e;
            } catch (Exception e) {
                LOGGER.error("Unexpected error during FreeIPA upgrade availability test", e);
                throw new TestFailException("Unexpected error during FreeIPA upgrade availability test: " + e.getMessage(), e);
            }
            return testDto;
        };
    }

    public Assertion<FreeIpaTestDto, FreeIpaClient> availableLoadBalancer() {
        return (testContext, testDto, client) -> {
            try {
                checkLoadBalancerIfRequired(
                        testContext,
                        client.getDefaultClient(testContext),
                        testDto.getResponse().getEnvironmentCrn()
                );
            } catch (TestFailException e) {
                throw e;
            } catch (Exception e) {
                throw new TestFailException("Unexpected error during FreeIPA loadbalancer availability test: " + e.getMessage(), e);
            }
            return testDto;
        };
    }

    private void checkLoadBalancerIfRequired(TestContext testContext, com.sequenceiq.freeipa.api.client.FreeIpaClient ipaClient, String environmentCrn) {
        try {
            DescribeFreeIpaResponse describeFreeIpaResponse = ipaClient.getFreeIpaV1Endpoint().describe(environmentCrn);
            Optional<SdxClusterDetailResponse> sdxClusterDetailResponse = Optional.ofNullable(testContext.given(SdxTestDto.class).getResponse());
            checkFreeipaLoadBalancerExist(describeFreeIpaResponse);
            checkFreeipaLoadBalancerIp(describeFreeIpaResponse);
            checkFreeipaRecordCname(sdxClusterDetailResponse, describeFreeIpaResponse, "kdc");
            checkFreeipaRecordCname(sdxClusterDetailResponse, describeFreeIpaResponse, "ldap");
            checkFreeipaRecordCname(sdxClusterDetailResponse, describeFreeIpaResponse, "kerberos");
        } catch (Exception e) {
            throw new TestFailException("FreeIpa upgrade loadbalancer test failed with unexpected error: " + e.getMessage(), e);
        }
    }

    private void checkFreeipaRecordCname(Optional<SdxClusterDetailResponse> sdxClusterDetailResponse,
        DescribeFreeIpaResponse describeFreeIpaResponse, String recordName) {
        String domain = describeFreeIpaResponse.getFreeIpa().getDomain();
        Set<InstanceMetaDataResponse> freeIpaMetadata = describeFreeIpaResponse.getInstanceGroups()
                .stream()
                .flatMap(instanceGroup -> instanceGroup.getMetaData().stream())
                .collect(Collectors.toSet());

        Map<String, Pair<Integer, String>> freeIpaRecordResults =
                sshJClientActions.executeSshCommandOnHost(
                        freeIpaMetadata, String.format(CHECK_RECORD, domain, recordName), false);
        for (Pair<Integer, String> value : freeIpaRecordResults.values()) {
            if (value.getLeft() != 0) {
                throw new TestFailException("Command Run failed with error: " + value.getRight());
            }
            String[] rows = value.getRight().split(" ");
            if (!rows[rows.length - 1].trim().equals(describeFreeIpaResponse.getLoadBalancer().getFqdn())) {
                throw new TestFailException(String.format("Loadbalancer fqdn %s does not match with freeIpa fqdn: %s on freeIpa.",
                        describeFreeIpaResponse.getLoadBalancer().getFqdn(), value.getRight()));
            }
        }
        if (sdxClusterDetailResponse.isPresent()) {
            List<String> sdxMetadata = sdxClusterDetailResponse.get().getStackV4Response().getInstanceGroups()
                    .stream()
                    .flatMap(instanceGroup -> instanceGroup.getMetadata().stream())
                    .map(InstanceMetaDataV4Response::getPrivateIp)
                    .collect(Collectors.toList());

            Map<String, Pair<Integer, String>> sdxRecordResults =
                    sshJClientActions.executeSshCommandOnHosts(
                            sdxMetadata, String.format(CHECK_LB, describeFreeIpaResponse.getLoadBalancer().getFqdn()));

            for (Pair<Integer, String> value : sdxRecordResults.values()) {
                if (value.getLeft() != 0) {
                    throw new TestFailException("Command Run failed with error: " + value.getRight());
                }
                for (String serverIp : describeFreeIpaResponse.getFreeIpa().getServerIp()) {
                    if (!value.getRight().trim().contains(serverIp)) {
                        throw new TestFailException(String.format("Loadbalancer fqdn %s does not match with freeIpa fqdn: %s on Sdx.",
                                value.getRight(),
                                serverIp));
                    }
                }
            }

            String unboundCommand = String.format(CHECK_UNBOUND, describeFreeIpaResponse.getLoadBalancer().getFqdn());
            Map<String, Pair<Integer, String>> sdxUnboundConfig =
                    sshJClientActions.executeSshCommandOnHosts(sdxMetadata, unboundCommand);

            for (Pair<Integer, String> value : sdxUnboundConfig.values()) {
                if (value.getLeft() != 0) {
                    throw new TestFailException("Unbound Command Run failed with error: " + value.getRight());
                }
                for (String serverIp : describeFreeIpaResponse.getFreeIpa().getServerIp()) {
                    if (!value.getRight().trim().contains(serverIp)) {
                        throw new TestFailException(String.format("Loadbalancer ip %s in unbound config does not match with freeIpa LB ip: %s on FreeIpa.",
                                value.getRight(),
                                serverIp));
                    }
                }
            }
        }
    }

    private void checkFreeipaLoadBalancerExist(DescribeFreeIpaResponse describeFreeIpaResponse) {
        if (describeFreeIpaResponse.getLoadBalancer() == null || describeFreeIpaResponse.getLoadBalancer().getFqdn() == null) {
            throw new TestFailException("FreeIPA upgrade did not upgraded to a loadbalancer which is required.");
        }
    }

    private void checkFreeipaLoadBalancerIp(DescribeFreeIpaResponse describeFreeIpaResponse) {
        Set<String> freeIpaServerIps = describeFreeIpaResponse.getFreeIpa().getServerIp();
        Set<String> loadBalancerServerIps = describeFreeIpaResponse.getLoadBalancer().getPrivateIps();
        if (!freeIpaServerIps.equals(loadBalancerServerIps)) {
            LOGGER.error("FreeIpa loadbalancer IPs {} did not match with the freeIpa serverIPs {}.",
                    loadBalancerServerIps,
                    freeIpaServerIps);
            throw new TestFailException(
                    String.format("FreeIpa upgrade loadbalancer test failed with unexpected error: " +
                            "loadbalancer IPs %s and freeIpa serverIPs %s.", loadBalancerServerIps, freeIpaServerIps));
        }
    }

    public Assertion<FreeIpaOperationStatusTestDto, FreeIpaClient> availableDuringOperation() {
        return (testContext, testDto, client) -> {
            com.sequenceiq.freeipa.api.client.FreeIpaClient ipaClient = client.getDefaultClient(testContext);
            FreeIpaTestDto freeIpaTestDto = testContext.get(FreeIpaTestDto.class);
            String environmentCrn = freeIpaTestDto.getResponse().getEnvironmentCrn();
            String accountId = Crn.safeFromString(environmentCrn).getAccountId();
            int errorCount = 0;
            while (ipaClient.getOperationV1Endpoint().getOperationStatus(testDto.getOperationId(), accountId).getStatus() == RUNNING) {
                try {
                    available().doAssertion(testContext, freeIpaTestDto, client);
                    errorCount = 0;
                    Thread.sleep(ASSERTION_ERROR_SLEEPDURATION);
                } catch (TestFailException ex) {
                    errorCount++;
                    LOGGER.warn("Freeipa availabilty assertion error during upgrade, tolerable count is {}, actual count is {}",
                            MAX_TOLERABLE_ERRORCOUNT, errorCount, ex);
                    if (errorCount >= MAX_TOLERABLE_ERRORCOUNT) {
                        LOGGER.error("Freeipa availabilty assertion error count reached the maximum during upgrade, test will fail", ex);
                        throw ex;
                    } else {
                        Thread.sleep(ASSERTION_ERROR_SLEEPDURATION);
                    }
                }
            }
            return testDto;
        };
    }

    private void syncUsers(TestContext testContext, com.sequenceiq.freeipa.api.client.FreeIpaClient ipaClient, String environmentCrn, String accountId) {
        try {
            SyncOperationStatus lastSyncOperationStatus = ipaClient.getUserV1Endpoint().getLastSyncOperationStatus(environmentCrn);
            if (lastSyncOperationStatus.getStatus() == SynchronizationStatus.RUNNING) {
                waitToCompleted(testContext, lastSyncOperationStatus.getOperationId(), "Initial or periodic usersync");
            }
            SynchronizeAllUsersRequest request = new SynchronizeAllUsersRequest();
            request.setAccountId(accountId);
            request.setEnvironments(Set.of(environmentCrn));
            request.setWorkloadCredentialsUpdateType(WorkloadCredentialsUpdateType.FORCE_UPDATE);
            try {
                SyncOperationStatus syncOperationStatus = ipaClient.getUserV1Endpoint().synchronizeAllUsers(request);
                waitToCompleted(testContext, syncOperationStatus.getOperationId(), "Full forced usersync");
            } catch (WebApplicationException e) {
                if (e.getResponse() != null && Response.Status.CONFLICT.getStatusCode() == e.getResponse().getStatus()) {
                    LOGGER.info("Usersync is already running");
                } else {
                    throw e;
                }
            }
        } catch (Exception e) {
            LOGGER.error("Full forced usersync test failed", e);
            throw new TestFailException("Full forced usersync test failed with: " + e.getMessage(), e);
        }
    }

    private void addAndDeleteDnsARecord(com.sequenceiq.freeipa.api.client.FreeIpaClient ipaClient, String environmentCrn) {
        try {
            AddDnsARecordRequest aRecordRequest = new AddDnsARecordRequest();
            aRecordRequest.setHostname("test-a-record");
            aRecordRequest.setCreateReverse(true);
            aRecordRequest.setEnvironmentCrn(environmentCrn);
            aRecordRequest.setIp("1.2.3.4");
            ipaClient.getDnsV1Endpoint().addDnsARecord(aRecordRequest);
            ipaClient.getDnsV1Endpoint().deleteDnsARecord(environmentCrn, null, aRecordRequest.getHostname());
        } catch (Exception e) {
            LOGGER.error("DNS A record test failed", e);
            throw new TestFailException("DNS A record test failed with: " + e.getMessage(), e);
        }
    }

    private void addAndDeleteDnsCnameRecord(com.sequenceiq.freeipa.api.client.FreeIpaClient ipaClient, String environmentCrn) {
        try {
            AddDnsCnameRecordRequest request = new AddDnsCnameRecordRequest();
            request.setCname("test-cname-record");
            request.setTargetFqdn("cloudera.com");
            request.setEnvironmentCrn(environmentCrn);
            ipaClient.getDnsV1Endpoint().addDnsCnameRecord(request);
            ipaClient.getDnsV1Endpoint().deleteDnsCnameRecord(environmentCrn, null, request.getCname());
        } catch (Exception e) {
            LOGGER.error("DNS CNAME record test failed", e);
            throw new TestFailException("DNS CNAME record test failed with: " + e.getMessage(), e);
        }
    }

    private void createBindUser(TestContext testContext, com.sequenceiq.freeipa.api.client.FreeIpaClient ipaClient, String environmentCrn) {
        try {
            BindUserCreateRequest bindUserCreateRequest = new BindUserCreateRequest();
            bindUserCreateRequest.setEnvironmentCrn(environmentCrn);
            bindUserCreateRequest.setBindUserNameSuffix("testuser");
            String initiatorUserCrn = "__internal__actor__";
            OperationStatus operationStatus = ipaClient.getFreeIpaV1Endpoint().createE2ETestBindUser(bindUserCreateRequest, initiatorUserCrn);
            waitToCompleted(testContext, operationStatus.getOperationId(), "createBindUserOperation");
        } catch (BadRequestException e) {
            LOGGER.warn("CREATE BIND USER failed to start, possibly because of missing E2E_TEST_ONLY entitlement, which is mock ums only", e);
        } catch (Exception e) {
            LOGGER.error("CREATE BIND USER test failed", e);
            throw new TestFailException("CREATE BIND USER test failed with: " + e.getMessage(), e);
        }
    }

    private void generateHostKeyTab(com.sequenceiq.freeipa.api.client.FreeIpaClient ipaClient, String environmentCrn) {
        try {
            HostKeytabRequest hostKeytabRequest = new HostKeytabRequest();
            hostKeytabRequest.setEnvironmentCrn(environmentCrn);
            hostKeytabRequest.setServerHostName("test.local");
            hostKeytabRequest.setDoNotRecreateKeytab(Boolean.FALSE);
            ipaClient.getKerberosMgmtV1Endpoint().generateHostKeytab(hostKeytabRequest);
        } catch (Exception e) {
            LOGGER.error("Generate Host keytab test failed", e);
            throw new TestFailException("Generate Host keytab test failed with: " + e.getMessage(), e);
        }
    }

    private void generateServiceKeytab(com.sequenceiq.freeipa.api.client.FreeIpaClient ipaClient, String environmentCrn) {
        try {
            ServiceKeytabRequest serviceKeytabRequest = new ServiceKeytabRequest();
            serviceKeytabRequest.setEnvironmentCrn(environmentCrn);
            serviceKeytabRequest.setServiceName("test");
            serviceKeytabRequest.setServerHostName("test.local");
            serviceKeytabRequest.setDoNotRecreateKeytab(Boolean.FALSE);
            ipaClient.getKerberosMgmtV1Endpoint().generateServiceKeytab(serviceKeytabRequest, null);
        } catch (Exception e) {
            LOGGER.error("Generate Service keytab test failed", e);
            throw new TestFailException("Generate Service keytab test failed with: " + e.getMessage(), e);
        }
    }

    private void cleanUp(TestContext testContext, com.sequenceiq.freeipa.api.client.FreeIpaClient ipaClient, String environmentCrn) {
        try {
            CleanupRequest cleanupRequest = new CleanupRequest();
            cleanupRequest.setEnvironmentCrn(environmentCrn);
            cleanupRequest.setClusterName("testuser");
            cleanupRequest.setUsers(Set.of("kerberosbind-testuser", "ldapbind-testuser"));
            OperationStatus operationStatus = ipaClient.getFreeIpaV1Endpoint().cleanup(cleanupRequest);
            waitToCompleted(testContext, operationStatus.getOperationId(), "cleanupOperation");
            String awaitExceptionKey = testContext.given("cleanupOperation", FreeIpaOperationStatusTestDto.class).getAwaitExceptionKey(COMPLETED);
            testContext.getExceptionMap().remove(awaitExceptionKey);
        } catch (Exception e) {
            LOGGER.error("CLEANUP test failed", e);
            throw new TestFailException("CLEANUP test failed with: " + e.getMessage(), e);
        }
    }

    private void waitToCompleted(TestContext testContext, String operationId, String operationName) {
        testContext
                .given(operationName, FreeIpaOperationStatusTestDto.class)
                    .withOperationId(operationId)
                .await(COMPLETED, doNotWaitForFlow().withTimeoutChecker(
                        new AbsolutTimeBasedTimeoutChecker(TimeUnit.MINUTES.toSeconds(operationTimeoutInMinutes))));
    }

    private void kinit(SdxTestDto sdxTestDto, com.sequenceiq.freeipa.api.client.FreeIpaClient ipaClient, String environmentCrn) {
        try {
            SyncOperationStatus lastSyncOperationStatus = ipaClient.getUserV1Endpoint().getLastSyncOperationStatus(environmentCrn);
            if (lastSyncOperationStatus.getStatus() == SynchronizationStatus.COMPLETED) {
                sshJClientActions.checkKinitDuringFreeipaUpgrade(sdxTestDto, sdxTestDto.getResponse().getStackV4Response().getInstanceGroups(),
                        List.of(HostGroupType.MASTER.getName()));
            } else {
                LOGGER.debug("Skipping kinit test because usersync state is not COMPLETED: " + lastSyncOperationStatus);
            }
        } catch (TestFailException e) {
            throw e;
        } catch (Exception e) {
            LOGGER.error("FreeIPA kinit test failed with unexpected error", e);
            throw new TestFailException("FreeIPA kinit test failed with unexpected error: " + e.getMessage(), e);
        }
    }

    private void dnsLookups(SdxTestDto sdxTestDto) {
        InstanceMetaDataV4Response instanceGroupMetadata = sdxTestDto.getResponse().getStackV4Response().getInstanceGroups().stream()
                .flatMap(instanceGroup -> instanceGroup.getMetadata().stream())
                .filter(metadata -> metadata.getInstanceGroup().equals("idbroker"))
                .filter(metadata -> StringUtils.isNoneBlank(metadata.getDiscoveryFQDN(), metadata.getPrivateIp()))
                .findFirst().orElseThrow(() -> new TestFailException("FreeIPA upgrade DNS lookups test failed, idbroker instance group was not found."));
        try {
            String cmd = String.format(CHECK_DNS_LOOKUPS_CMD, instanceGroupMetadata.getDiscoveryFQDN(), instanceGroupMetadata.getPrivateIp());
            Map<String, Pair<Integer, String>> results = sshJClientActions.executeSshCommandOnHost(
                    sdxTestDto.getResponse().getStackV4Response().getInstanceGroups(), List.of(HostGroupType.MASTER.getName()), cmd, false);
            results.values().forEach(result -> assertEquals(0, result.getLeft()));
        } catch (Exception e) {
            LOGGER.error("FreeIPA upgrade DNS lookups test failed with unexpected error", e);
            throw new TestFailException("FreeIPA upgrade DNS lookups test failed with unexpected error: " + e.getMessage(), e);
        }
    }
}
