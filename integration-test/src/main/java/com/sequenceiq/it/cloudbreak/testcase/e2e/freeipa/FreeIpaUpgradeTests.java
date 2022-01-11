package com.sequenceiq.it.cloudbreak.testcase.e2e.freeipa;

import static com.sequenceiq.freeipa.api.v1.operation.model.OperationState.COMPLETED;
import static com.sequenceiq.freeipa.api.v1.operation.model.OperationState.RUNNING;
import static com.sequenceiq.it.cloudbreak.context.RunningParameter.key;
import static com.sequenceiq.it.cloudbreak.context.RunningParameter.waitForFlow;

import java.util.List;
import java.util.Set;

import javax.inject.Inject;

import org.junit.jupiter.api.Assertions;
import org.testng.annotations.Test;

import com.sequenceiq.cloudbreak.auth.crn.Crn;
import com.sequenceiq.cloudbreak.polling.AbsolutTimeBasedTimeoutChecker;
import com.sequenceiq.freeipa.api.v1.dns.model.AddDnsARecordRequest;
import com.sequenceiq.freeipa.api.v1.dns.model.AddDnsCnameRecordRequest;
import com.sequenceiq.freeipa.api.v1.dns.model.AddDnsZoneForSubnetsRequest;
import com.sequenceiq.freeipa.api.v1.freeipa.cleanup.CleanupRequest;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.binduser.BindUserCreateRequest;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.Status;
import com.sequenceiq.freeipa.api.v1.operation.model.OperationState;
import com.sequenceiq.freeipa.api.v1.operation.model.OperationStatus;
import com.sequenceiq.it.cloudbreak.FreeIpaClient;
import com.sequenceiq.it.cloudbreak.client.FreeIpaTestClient;
import com.sequenceiq.it.cloudbreak.context.Description;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.freeipa.FreeIpaOperationStatusTestDto;
import com.sequenceiq.it.cloudbreak.dto.freeipa.FreeIpaTestDto;
import com.sequenceiq.it.cloudbreak.dto.telemetry.TelemetryTestDto;
import com.sequenceiq.it.cloudbreak.exception.TestFailException;
import com.sequenceiq.it.cloudbreak.testcase.e2e.AbstractE2ETest;

public class FreeIpaUpgradeTests extends AbstractE2ETest {

    protected static final Status FREEIPA_AVAILABLE = Status.AVAILABLE;

    protected static final Status FREEIPA_DELETE_COMPLETED = Status.DELETE_COMPLETED;

    private static final long TWO_HOURS_IN_SEC = 2L * 60 * 60;

    private static final long FIVE_MINUTES_IN_SEC = 5L * 60;

    @Inject
    private FreeIpaTestClient freeIpaTestClient;

    @Test(dataProvider = TEST_CONTEXT)
    @Description(
            given = "there is a running cloudbreak",
            when = "a valid stack create request is sent with 1 FreeIPA instances " +
                    "AND the stack is upgraded one node at a time",
            then = "the stack should be available AND deletable")
    public void testSingleFreeIpaInstanceUpgrade(TestContext testContext) {
        String freeIpa = resourcePropertyProvider().getName();

        testContext
                .given("telemetry", TelemetryTestDto.class)
                .withLogging()
                .withReportClusterLogs()
                .given(freeIpa, FreeIpaTestDto.class)
                .withTelemetry("telemetry")
                .withUpgradeCatalogAndImage()
                .when(freeIpaTestClient.create(), key(freeIpa))
                .await(FREEIPA_AVAILABLE)
                .when(freeIpaTestClient.upgrade())
                .await(Status.UPDATE_IN_PROGRESS, waitForFlow().withWaitForFlow(Boolean.FALSE))
                .given(FreeIpaOperationStatusTestDto.class)
                .withOperationId(((FreeIpaTestDto) testContext.get(freeIpa)).getOperationId())
                .then((tc, testDto, freeIpaClient) -> testFreeIpaAvailabilityDuringUpgrade(tc, testDto, freeIpaClient, freeIpa))
                .await(COMPLETED)
                .given(freeIpa, FreeIpaTestDto.class)
                .await(FREEIPA_AVAILABLE, waitForFlow().withWaitForFlow(Boolean.FALSE))
                .then((tc, testDto, client) -> freeIpaTestClient.delete().action(tc, testDto, client))
                .await(FREEIPA_DELETE_COMPLETED, waitForFlow().withWaitForFlow(Boolean.FALSE))
                .validate();
    }

    @Test(dataProvider = TEST_CONTEXT)
    @Description(
            given = "there is a running cloudbreak",
            when = "a valid stack create request is sent with 3 FreeIPA instances " +
                    "AND the stack is upgraded one node at a time",
            then = "the stack should be available AND deletable")
    public void testHAFreeIpaInstanceUpgrade(TestContext testContext) {
        String freeIpa = resourcePropertyProvider().getName();

        testContext
                .given("telemetry", TelemetryTestDto.class)
                .withLogging()
                .withReportClusterLogs()
                .given(freeIpa, FreeIpaTestDto.class)
                .withFreeIpaHa(1, 3)
                .withTelemetry("telemetry")
                .withUpgradeCatalogAndImage()
                .when(freeIpaTestClient.create(), key(freeIpa))
                .await(FREEIPA_AVAILABLE)
                .when(freeIpaTestClient.upgrade())
                .await(Status.UPDATE_IN_PROGRESS, waitForFlow().withWaitForFlow(Boolean.FALSE))
                .given(FreeIpaOperationStatusTestDto.class)
                .withOperationId(((FreeIpaTestDto) testContext.get(freeIpa)).getOperationId())
                .then((tc, testDto, freeIpaClient) -> testFreeIpaAvailabilityDuringUpgrade(tc, testDto, freeIpaClient, freeIpa))
                .await(COMPLETED, waitForFlow().withWaitForFlow(Boolean.FALSE).withTimeoutChecker(new AbsolutTimeBasedTimeoutChecker(TWO_HOURS_IN_SEC)))
                .given(freeIpa, FreeIpaTestDto.class)
                .await(FREEIPA_AVAILABLE, waitForFlow().withWaitForFlow(Boolean.FALSE))
                .then((tc, testDto, client) -> freeIpaTestClient.delete().action(tc, testDto, client))
                .await(FREEIPA_DELETE_COMPLETED, waitForFlow().withWaitForFlow(Boolean.FALSE))
                .validate();
    }

    private FreeIpaOperationStatusTestDto testFreeIpaAvailabilityDuringUpgrade(TestContext testContext, FreeIpaOperationStatusTestDto testDto,
            FreeIpaClient freeIpaClient, String freeIpa) {
        try {
            com.sequenceiq.freeipa.api.client.FreeIpaClient ipaClient = freeIpaClient.getDefaultClient();
            FreeIpaTestDto freeIpaTestDto = testContext.get(freeIpa);
            String environmentCrn = freeIpaTestDto.getResponse().getEnvironmentCrn();
            String accountId = Crn.safeFromString(environmentCrn).getAccountId();
            while (ipaClient.getOperationV1Endpoint().getOperationStatus(testDto.getOperationId(), accountId).getStatus() == RUNNING) {
                addAndDeleteDnsARecord(ipaClient, environmentCrn);
                addAndDeleteDnsCnameRecord(ipaClient, environmentCrn);
                addListDeleteDnsZonesBySubnet(ipaClient, environmentCrn);
                createBindUser(testContext, ipaClient, environmentCrn, accountId);
                cleanUp(testContext, ipaClient, environmentCrn, accountId);
            }
        } catch (TestFailException e) {
            throw e;
        } catch (Exception e) {
            logger.error("Unexpected error during FreeIPA upgrade availability test", e);
            throw new TestFailException("Unexpected error during FreeIPA upgrade availability test: " + e.getMessage(), e);
        }
        return testDto;
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
            logger.error("DNS A record test failed during upgrade", e);
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
            logger.error("DNS CNAME record test failed during upgrade", e);
            throw new TestFailException("DNS CNAME record test failed during upgrade with: " + e.getMessage(), e);
        }
    }

    private void addListDeleteDnsZonesBySubnet(com.sequenceiq.freeipa.api.client.FreeIpaClient ipaClient, String environmentCrn) {
        try {
            AddDnsZoneForSubnetsRequest request = new AddDnsZoneForSubnetsRequest();
            request.setSubnets(List.of("10.0.1.0/24", "192.168.1.0/24"));
            request.setEnvironmentCrn(environmentCrn);
            ipaClient.getDnsV1Endpoint().addDnsZoneForSubnets(request);
            Set<String> dnsZones = ipaClient.getDnsV1Endpoint().listDnsZones(environmentCrn);
            Assertions.assertTrue(dnsZones.stream().anyMatch(dnsZone -> dnsZone.startsWith("1.0.10")));
            Assertions.assertTrue(dnsZones.stream().anyMatch(dnsZone -> dnsZone.startsWith("1.168.192")));

            ipaClient.getDnsV1Endpoint().deleteDnsZoneBySubnet(environmentCrn, "192.168.1.0/24");
            ipaClient.getDnsV1Endpoint().deleteDnsZoneBySubnet(environmentCrn, "10.0.1.0/24");
            dnsZones = ipaClient.getDnsV1Endpoint().listDnsZones(environmentCrn);
            Assertions.assertFalse(dnsZones.stream().anyMatch(dnsZone -> dnsZone.startsWith("1.0.10")));
            Assertions.assertFalse(dnsZones.stream().anyMatch(dnsZone -> dnsZone.startsWith("1.168.192")));
        } catch (Exception e) {
            logger.error("DNS ZONE test failed during upgrade", e);
            throw new TestFailException("DNS ZONE test failed during upgrade with: " + e.getMessage(), e);
        }
    }

    private void createBindUser(TestContext testContext, com.sequenceiq.freeipa.api.client.FreeIpaClient ipaClient, String environmentCrn, String accountId) {
        BindUserCreateRequest bindUserCreateRequest = new BindUserCreateRequest();
        bindUserCreateRequest.setEnvironmentCrn(environmentCrn);
        bindUserCreateRequest.setBindUserNameSuffix("testuser");
        String initiatorUserCrn = "__internal__actor__";
        OperationStatus operationStatus = ipaClient.getFreeIpaV1Endpoint().createE2ETestBindUser(bindUserCreateRequest, initiatorUserCrn);
        operationStatus = waitToCompleted(testContext, ipaClient, operationStatus, accountId, "createBindUserOperation");
        if (!isSuccess(operationStatus)) {
            throw new TestFailException("Freeipa createBindUser() test has failed: " + operationStatus);
        }
    }

    private void cleanUp(TestContext testContext, com.sequenceiq.freeipa.api.client.FreeIpaClient ipaClient, String environmentCrn, String accountId) {
        CleanupRequest cleanupRequest = new CleanupRequest();
        cleanupRequest.setEnvironmentCrn(environmentCrn);
        cleanupRequest.setClusterName("testuser");
        cleanupRequest.setUsers(Set.of("kerberosbind-testuser", "ldapbind-testuser"));
        OperationStatus operationStatus = ipaClient.getFreeIpaV1Endpoint().cleanup(cleanupRequest);
        operationStatus = waitToCompleted(testContext, ipaClient, operationStatus, accountId, "cleanupOperation");
        if (!isSuccess(operationStatus)) {
            throw new TestFailException("Freeipa cleanUp() test has failed: " + operationStatus);
        }
    }

    private OperationStatus waitToCompleted(TestContext testContext, com.sequenceiq.freeipa.api.client.FreeIpaClient ipaClient, OperationStatus operationStatus,
            String accountId, String operationName) {
        testContext
                .given(operationName, FreeIpaOperationStatusTestDto.class)
                .withOperationId(operationStatus.getOperationId())
                .await(COMPLETED, waitForFlow().withWaitForFlow(Boolean.FALSE).withTimeoutChecker(new AbsolutTimeBasedTimeoutChecker(FIVE_MINUTES_IN_SEC)));
        return ipaClient.getOperationV1Endpoint().getOperationStatus(operationStatus.getOperationId(), accountId);
    }

    private boolean isSuccess(OperationStatus operationStatus) {
        OperationState operationState = operationStatus.getStatus();
        return COMPLETED == operationState;
    }
}
