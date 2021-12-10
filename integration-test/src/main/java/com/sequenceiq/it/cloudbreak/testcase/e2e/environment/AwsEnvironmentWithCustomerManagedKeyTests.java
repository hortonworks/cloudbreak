package com.sequenceiq.it.cloudbreak.testcase.e2e.environment;

import static com.sequenceiq.it.cloudbreak.cloud.HostGroupType.MASTER;
import static java.lang.String.format;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.Test;

import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.common.api.type.Tunnel;
import com.sequenceiq.environment.api.v1.environment.model.response.DetailedEnvironmentResponse;
import com.sequenceiq.environment.api.v1.environment.model.response.EnvironmentStatus;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.Status;
import com.sequenceiq.freeipa.api.v1.operation.model.OperationState;
import com.sequenceiq.it.cloudbreak.EnvironmentClient;
import com.sequenceiq.it.cloudbreak.FreeIpaClient;
import com.sequenceiq.it.cloudbreak.client.CredentialTestClient;
import com.sequenceiq.it.cloudbreak.client.EnvironmentTestClient;
import com.sequenceiq.it.cloudbreak.client.FreeIpaTestClient;
import com.sequenceiq.it.cloudbreak.cloud.v4.aws.AwsProperties;
import com.sequenceiq.it.cloudbreak.context.Description;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.credential.CredentialTestDto;
import com.sequenceiq.it.cloudbreak.dto.environment.EnvironmentNetworkTestDto;
import com.sequenceiq.it.cloudbreak.dto.environment.EnvironmentTestDto;
import com.sequenceiq.it.cloudbreak.dto.freeipa.FreeIpaTestDto;
import com.sequenceiq.it.cloudbreak.dto.freeipa.FreeIpaUserSyncTestDto;
import com.sequenceiq.it.cloudbreak.dto.telemetry.TelemetryTestDto;
import com.sequenceiq.it.cloudbreak.exception.TestFailException;
import com.sequenceiq.it.cloudbreak.log.Log;
import com.sequenceiq.it.cloudbreak.testcase.e2e.AbstractE2ETest;
import com.sequenceiq.it.cloudbreak.util.CloudFunctionality;
import com.sequenceiq.it.cloudbreak.util.FreeIpaInstanceUtil;
import com.sequenceiq.it.cloudbreak.util.spot.UseSpotInstances;

public class AwsEnvironmentWithCustomerManagedKeyTests extends AbstractE2ETest {

    private static final Logger LOGGER = LoggerFactory.getLogger(AwsEnvironmentWithCustomerManagedKeyTests.class);

    @Inject
    private EnvironmentTestClient environmentTestClient;

    @Inject
    private CredentialTestClient credentialTestClient;

    @Inject
    private AwsProperties awsProperties;

    @Inject
    private FreeIpaTestClient freeIpaTestClient;

    @Inject
    private FreeIpaInstanceUtil freeIpaInstanceUtil;

    @Override
    protected void setupTest(TestContext testContext) {
        testContext.getCloudProvider().getCloudFunctionality().cloudStorageInitialize();
        checkCloudPlatform(CloudPlatform.AWS);
        createDefaultUser(testContext);
    }

    @Test(dataProvider = TEST_CONTEXT)
    @UseSpotInstances
    @Description(
            given = "there is a running cloudbreak",
            when = "create an Environment with disk encryption",
            then = "should use encryption parameters for resource encryption.")
    public void testEnvironmentWithCustomerManagedKey(TestContext testContext) {
        testContext
                .given(CredentialTestDto.class)
                .when(credentialTestClient.create())
                .given(EnvironmentNetworkTestDto.class)
                .given("telemetry", TelemetryTestDto.class)
                    .withLogging()
                    .withReportClusterLogs()
                .given(EnvironmentTestDto.class)
                    .withNetwork()
                    .withResourceEncryption()
                    .withTelemetry("telemetry")
                    .withTunnel(Tunnel.CLUSTER_PROXY)
                    .withCreateFreeIpa(Boolean.FALSE)
                .when(environmentTestClient.create())
                .await(EnvironmentStatus.AVAILABLE)
                .given(FreeIpaTestDto.class)
                    .withEnvironment()
                    .withTelemetry("telemetry")
                    .withCatalog(commonCloudProperties().getImageValidation().getFreeIpaImageCatalog(),
                        commonCloudProperties().getImageValidation().getFreeIpaImageUuid())
                .when(freeIpaTestClient.create())
                .await(Status.AVAILABLE)
                .awaitForHealthyInstances()
                .given(FreeIpaUserSyncTestDto.class)
                .when(freeIpaTestClient.getLastSyncOperationStatus())
                .await(OperationState.COMPLETED)
                .given(EnvironmentTestDto.class)
                .when(environmentTestClient.describe())
                .then(this::verifyEnvironmentResponseKMSParameters)
                .given(FreeIpaTestDto.class)
                .then(this::verifyFreeIPAEC2VolumeKMSKey)
                .validate();
    }

    private EnvironmentTestDto verifyEnvironmentResponseKMSParameters(TestContext testContext, EnvironmentTestDto testDto,
            EnvironmentClient environmentClient) {
        DetailedEnvironmentResponse environment = environmentClient.getDefaultClient().environmentV1Endpoint().getByName(testDto.getName());

        if (CloudPlatform.AWS.name().equals(environment.getCloudPlatform())) {
            String encryptionKey = environment.getAws().getAwsDiskEncryptionParameters().getEncryptionKeyArn();
            String environmentName = testDto.getRequest().getName();

            if (StringUtils.isEmpty(encryptionKey)) {
                LOGGER.error(String.format("KMS key is not available for [%s] environment!", environmentName));
                throw new TestFailException(format("KMS key is not available for [%s] environment!", environmentName));
            } else {
                LOGGER.info(String.format(" Environment [%s] create has been done with [%s] KMS key. ", environmentName, encryptionKey));
                Log.then(LOGGER, format(" Environment '%s' create has been done with [%s] KMS key. ", environmentName, encryptionKey));
            }
        }
        return testDto;
    }

    private FreeIpaTestDto verifyFreeIPAEC2VolumeKMSKey(TestContext testContext, FreeIpaTestDto testDto, FreeIpaClient freeIpaClient) {
        CloudFunctionality cloudFunctionality = testContext.getCloudProvider().getCloudFunctionality();
        List<String> instanceIds = freeIpaInstanceUtil.getInstanceIds(testDto, freeIpaClient, MASTER.getName());
        String kmsKeyArn = awsProperties.getDiskEncryption().getEnvironmentKey();

        List<String> volumeKmsKeyIds = new ArrayList<>(cloudFunctionality.listVolumeKmsKeyIds(instanceIds));
        if (volumeKmsKeyIds.stream().noneMatch(keyId -> keyId.equalsIgnoreCase(kmsKeyArn))) {
            LOGGER.error("FreeIpa volume has not been encrypted with [{}] KMS key!", kmsKeyArn);
            throw new TestFailException(format("FreeIpa volume has not been encrypted with [%s] KMS key!", kmsKeyArn));
        } else {
            LOGGER.info(String.format("FreeIpa volume has been encrypted with [%s] KMS key!", kmsKeyArn));
            Log.then(LOGGER, format(" FreeIpa volume has not been encrypted with [%s] KMS key! ", kmsKeyArn));
        }
        return testDto;
    }
}