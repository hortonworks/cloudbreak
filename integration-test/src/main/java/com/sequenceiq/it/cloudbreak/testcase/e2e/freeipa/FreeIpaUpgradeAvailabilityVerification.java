package com.sequenceiq.it.cloudbreak.testcase.e2e.freeipa;

import static com.sequenceiq.freeipa.api.v1.operation.model.OperationState.COMPLETED;
import static com.sequenceiq.freeipa.api.v1.operation.model.OperationState.RUNNING;
import static com.sequenceiq.it.cloudbreak.context.RunningParameter.waitForFlow;

import java.util.List;
import java.util.Map;
import java.util.Set;

import jakarta.inject.Inject;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.Assertions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.instancegroup.instancemetadata.InstanceMetaDataV4Response;
import com.sequenceiq.cloudbreak.auth.crn.Crn;
import com.sequenceiq.cloudbreak.polling.AbsolutTimeBasedTimeoutChecker;
import com.sequenceiq.freeipa.api.v1.dns.model.AddDnsARecordRequest;
import com.sequenceiq.freeipa.api.v1.dns.model.AddDnsCnameRecordRequest;
import com.sequenceiq.freeipa.api.v1.freeipa.cleanup.CleanupRequest;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.binduser.BindUserCreateRequest;
import com.sequenceiq.freeipa.api.v1.freeipa.user.model.SyncOperationStatus;
import com.sequenceiq.freeipa.api.v1.freeipa.user.model.SynchronizationStatus;
import com.sequenceiq.freeipa.api.v1.freeipa.user.model.SynchronizeAllUsersRequest;
import com.sequenceiq.freeipa.api.v1.freeipa.user.model.WorkloadCredentialsUpdateType;
import com.sequenceiq.freeipa.api.v1.kerberosmgmt.model.HostKeytabRequest;
import com.sequenceiq.freeipa.api.v1.kerberosmgmt.model.ServiceKeytabRequest;
import com.sequenceiq.freeipa.api.v1.operation.model.OperationStatus;
import com.sequenceiq.it.cloudbreak.cloud.HostGroupType;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.freeipa.FreeIpaOperationStatusTestDto;
import com.sequenceiq.it.cloudbreak.dto.freeipa.FreeIpaTestDto;
import com.sequenceiq.it.cloudbreak.dto.sdx.SdxTestDto;
import com.sequenceiq.it.cloudbreak.exception.TestFailException;
import com.sequenceiq.it.cloudbreak.microservice.FreeIpaClient;
import com.sequenceiq.it.cloudbreak.testcase.AbstractIntegrationTest;
import com.sequenceiq.it.cloudbreak.util.ssh.action.SshJClientActions;

@Component
public class FreeIpaUpgradeAvailabilityVerification {

    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractIntegrationTest.class);

    private static final long FIVE_MINUTES_IN_SEC = 5L * 60;

    private static final String CHECK_DNS_LOOKUPS_CMD = "ping -c 2 %s | grep -q '%s'";

    @Inject
    private SshJClientActions sshJClientActions;

    public FreeIpaOperationStatusTestDto testFreeIpaAvailabilityDuringUpgrade(TestContext testContext, FreeIpaOperationStatusTestDto testDto,
            FreeIpaClient freeIpaClient, String freeIpa) {
        try {
            com.sequenceiq.freeipa.api.client.FreeIpaClient ipaClient = freeIpaClient.getDefaultClient();
            FreeIpaTestDto freeIpaTestDto = testContext.get(freeIpa);
            String environmentCrn = freeIpaTestDto.getResponse().getEnvironmentCrn();
            String accountId = Crn.safeFromString(environmentCrn).getAccountId();
            boolean firstTestFailure = true;
            while (ipaClient.getOperationV1Endpoint().getOperationStatus(testDto.getOperationId(), accountId).getStatus() == RUNNING) {
                addAndDeleteDnsARecord(ipaClient, environmentCrn);
                addAndDeleteDnsCnameRecord(ipaClient, environmentCrn);
                createBindUser(testContext, ipaClient, environmentCrn);
                generateHostKeyTab(ipaClient, environmentCrn);
                generateServiceKeytab(ipaClient, environmentCrn);
                dnsLookups(testContext.given(SdxTestDto.class));
                cleanUp(testContext, ipaClient, environmentCrn);
//              kinit(testContext.given(SdxTestDto.class), ipaClient, environmentCrn);
                syncUsers(testContext, ipaClient, environmentCrn, accountId);
            }
        } catch (TestFailException e) {
            throw e;
        } catch (Exception e) {
            LOGGER.error("Unexpected error during FreeIPA upgrade availability test", e);
            throw new TestFailException("Unexpected error during FreeIPA upgrade availability test: " + e.getMessage(), e);
        }
        return testDto;
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
            LOGGER.error("Full forced usersync test failed during upgrade", e);
            throw new TestFailException("Full forced usersync test failed during upgrade with: " + e.getMessage(), e);
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
            LOGGER.error("DNS A record test failed during upgrade", e);
            throw new TestFailException("DNS A record test failed during upgrade with: " + e.getMessage(), e);
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
            LOGGER.error("DNS CNAME record test failed during upgrade", e);
            throw new TestFailException("DNS CNAME record test failed during upgrade with: " + e.getMessage(), e);
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
        } catch (Exception e) {
            LOGGER.error("CREATE BIND USER test failed during upgrade", e);
            throw new TestFailException("CREATE BIND USER test failed during upgrade with: " + e.getMessage(), e);
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
            LOGGER.error("Generate Host keytab test failed during upgrade", e);
            throw new TestFailException("Generate Host keytab test failed during upgrade with: " + e.getMessage(), e);
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
            LOGGER.error("Generate Service keytab test failed during upgrade", e);
            throw new TestFailException("Generate Service keytab test failed during upgrade with: " + e.getMessage(), e);
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
            LOGGER.error("CLEANUP test failed during upgrade", e);
//            throw new TestFailException("CLEANUP test failed during upgrade with: " + e.getMessage(), e);
        }
    }

    private void waitToCompleted(TestContext testContext, String operationId, String operationName) {
        testContext
                .given(operationName, FreeIpaOperationStatusTestDto.class)
                .withOperationId(operationId)
                .await(COMPLETED, waitForFlow().withWaitForFlow(Boolean.FALSE).withTimeoutChecker(new AbsolutTimeBasedTimeoutChecker(FIVE_MINUTES_IN_SEC)));
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
            LOGGER.error("FreeIPA upgrade kinit test failed with unexpected error", e);
            throw new TestFailException("FreeIPA upgrade kinit test failed with unexpected error: " + e.getMessage(), e);
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
            results.values().forEach(result -> Assertions.assertEquals(0, result.getLeft()));
        } catch (Exception e) {
            LOGGER.error("FreeIPA upgrade DNS lookups test failed with unexpected error", e);
            throw new TestFailException("FreeIPA upgrade DNS lookups test failed with unexpected error: " + e.getMessage(), e);
        }
    }
}
