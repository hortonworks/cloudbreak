package com.sequenceiq.it.cloudbreak.testcase.e2e.gov;

import static java.lang.String.format;

import jakarta.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;

import com.sequenceiq.environment.api.v1.environment.model.request.AttachedFreeIpaRequest;
import com.sequenceiq.environment.api.v1.environment.model.request.aws.AwsFreeIpaParameters;
import com.sequenceiq.environment.api.v1.environment.model.request.aws.AwsFreeIpaSpotParameters;
import com.sequenceiq.it.cloudbreak.client.DistroXTestClient;
import com.sequenceiq.it.cloudbreak.client.FreeIpaTestClient;
import com.sequenceiq.it.cloudbreak.client.SdxTestClient;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.environment.EnvironmentTestDto;
import com.sequenceiq.it.cloudbreak.dto.sdx.SdxInternalTestDto;
import com.sequenceiq.it.cloudbreak.dto.telemetry.TelemetryTestDto;
import com.sequenceiq.it.cloudbreak.testcase.e2e.AbstractE2ETest;

public class PreconditionGovTest extends AbstractE2ETest {

    private static final Logger LOGGER = LoggerFactory.getLogger(PreconditionGovTest.class);

    @Inject
    private FreeIpaTestClient freeIpaTestClient;

    @Inject
    private SdxTestClient sdxTestClient;

    @Inject
    private DistroXTestClient distroXTestClient;

    @Value("${integrationtest.cloudbreak.server}")
    private String defaultServer;

    @Override
    protected void setupTest(TestContext testContext) {
        testContext.getCloudProvider().getCloudFunctionality().cloudStorageInitialize();
        initUsers(testContext);
        initializeDefaultBlueprints(testContext);
        createDefaultCredential(testContext);
    }

    protected SdxTestClient getSdxTestClient() {
        return sdxTestClient;
    }

    protected DistroXTestClient getDistroXTestClient() {
        return distroXTestClient;
    }

    @Override
    protected void createDefaultEnvironment(TestContext testContext) {
        initiateEnvironmentCreation(testContext);
        waitForEnvironmentCreation(testContext);
        waitForUserSync(testContext);
        setFreeIpaResponse(testContext);
    }

    @Override
    protected void initiateEnvironmentCreation(TestContext testContext) {
        testContext
                .given("telemetry", TelemetryTestDto.class)
                    .withLogging()
                    .withReportClusterLogs()
                .given(EnvironmentTestDto.class)
                    .withNetwork()
                    .withTelemetry("telemetry")
                    .withResourceEncryption(testContext.isResourceEncryptionEnabled())
                    .withTunnel(testContext.getTunnel())
                    .withCreateFreeIpa(Boolean.TRUE)
                    .withFreeIpaNodes(getFreeIpaInstanceCountByProvider(testContext))
                    .withFreeIpaImage(commonCloudProperties().getImageValidation().getFreeIpaImageCatalog(),
                        commonCloudProperties().getImageValidation().getFreeIpaImageUuid())
                .when(getEnvironmentTestClient().create())
                .validate();
    }

    @Override
    protected void createDefaultDatalake(TestContext testContext) {
        initiateEnvironmentCreation(testContext);
        initiateDatalakeCreation(testContext);
        waitForEnvironmentCreation(testContext);
        waitForUserSync(testContext);
        setFreeIpaResponse(testContext);
        waitForDatalakeCreation(testContext);
    }

    // This is needed until CB-16092 Support DB SSL in AWS GovCloud for DL and DH clusters using RDS
    // is going to be implemented
    // OR
    // /mock-thunderhead/src/main/resources/application.yml
    //    database.wire.encryption.enable: true
    // is going to be set to 'false'
    @Override
    protected void initiateDatalakeCreation(TestContext testContext) {

        testContext
                .given(SdxInternalTestDto.class)
                    .withCloudStorage(getCloudStorageRequest(testContext))
                .when(sdxTestClient.createInternal())
                .validate();
    }

    protected void initUsers(TestContext testContext) {
        testContext.checkNonEmpty("integrationtest.cloudbreak.server", defaultServer);
        if (StringUtils.containsIgnoreCase(defaultServer, "usg-1.cdp.mow-dev")) {
            LOGGER.info(format("Tested environmet is GOV Dev at '%s'. So we are initializing GOV Dev UMS users!", defaultServer));
            useRealUmsUser(testContext, GovUserKeys.USER_ACCOUNT_ADMIN);
            useRealUmsUser(testContext, GovUserKeys.ENV_CREATOR_A);
        } else {
            createDefaultUser(testContext);
        }
    }

    protected AttachedFreeIpaRequest attachedFreeIpaHARequestForTest() {
        AttachedFreeIpaRequest attachedFreeIpaRequest = new AttachedFreeIpaRequest();
        AwsFreeIpaParameters awsFreeIpaParameters = new AwsFreeIpaParameters();
        AwsFreeIpaSpotParameters awsFreeIpaSpotParameters = new AwsFreeIpaSpotParameters();

        // It won't use Spot instances for FreeIpa.
        awsFreeIpaSpotParameters.setPercentage(0);
        awsFreeIpaParameters.setSpot(awsFreeIpaSpotParameters);

        attachedFreeIpaRequest.setCreate(Boolean.TRUE);
        // FreeIpa HA with 2 instances is defined.
        attachedFreeIpaRequest.setInstanceCountByGroup(2);
        attachedFreeIpaRequest.setAws(awsFreeIpaParameters);
        return attachedFreeIpaRequest;
    }

}
