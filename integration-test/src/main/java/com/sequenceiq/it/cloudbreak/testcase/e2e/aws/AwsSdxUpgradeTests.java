package com.sequenceiq.it.cloudbreak.testcase.e2e.aws;

import static com.sequenceiq.it.cloudbreak.context.RunningParameter.key;
import static java.lang.String.format;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.Test;

import com.sequenceiq.it.cloudbreak.client.SdxTestClient;
import com.sequenceiq.it.cloudbreak.cloud.v4.aws.AwsProperties;
import com.sequenceiq.it.cloudbreak.context.Description;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.ClouderaManagerTestDto;
import com.sequenceiq.it.cloudbreak.dto.ClusterTestDto;
import com.sequenceiq.it.cloudbreak.dto.ImageSettingsTestDto;
import com.sequenceiq.it.cloudbreak.dto.sdx.SdxInternalTestDto;
import com.sequenceiq.it.cloudbreak.dto.stack.StackTestDto;
import com.sequenceiq.it.cloudbreak.exception.TestFailException;
import com.sequenceiq.it.cloudbreak.log.Log;
import com.sequenceiq.it.cloudbreak.testcase.e2e.BasicSdxTests;

public class AwsSdxUpgradeTests extends BasicSdxTests {
    private static final Logger LOGGER = LoggerFactory.getLogger(AwsSdxUpgradeTests.class);

    @Inject
    private SdxTestClient sdxTestClient;

    @Inject
    private AwsProperties awsProperties;

    @Override
    protected void setupTest(TestContext testContext) {
        createDefaultUser(testContext);
        createDefaultCredential(testContext);
        createEnvironmentForSdx(testContext);
        initializeDefaultBlueprints(testContext);
    }

    @Test(dataProvider = TEST_CONTEXT)
    @Description(
            given = "there is a running Cloudbreak, and an SDX cluster in available state",
            when = "a newer image is available for CentOS7 on AWS in eu-west-1",
            then = "image upgrade notification should be available"
    )
    public void testSDXWithOlderImageCanBeCreatedSuccessfully(TestContext testContext) {
        String sdxInternal = resourcePropertyProvider().getName();
        String cluster = resourcePropertyProvider().getName();
        String clouderaManager = resourcePropertyProvider().getName();
        String imageSettings = resourcePropertyProvider().getName();
        String stack = resourcePropertyProvider().getName();

        testContext
                .given(clouderaManager, ClouderaManagerTestDto.class)
                .given(cluster, ClusterTestDto.class).withClouderaManager(clouderaManager)
                .given(imageSettings, ImageSettingsTestDto.class)
                .given(stack, StackTestDto.class).withCluster(cluster).withImageSettings(imageSettings)
                .given(sdxInternal, SdxInternalTestDto.class)
                .withStackRequest()
                .when(sdxTestClient.createInternal(), key(sdxInternal))
                .await(SDX_RUNNING)
                .then((tc, dto, client) -> {
                    Log.log(LOGGER, format(" Image Catalog Name: %s ", dto.getResponse().getStackV4Response().getImage().getCatalogName()));
                    Log.log(LOGGER, format(" Image Catalog URL: %s ", dto.getResponse().getStackV4Response().getImage().getCatalogUrl()));
                    Log.log(LOGGER, format(" Image ID: %s ", dto.getResponse().getStackV4Response().getImage().getId()));

                    if (!dto.getResponse().getStackV4Response().getImage().getId().equals(awsProperties.getBaseimage().getImageId())) {
                        throw new TestFailException(" The selected image ID is: " + dto.getResponse().getStackV4Response().getImage().getId() + " instead of: "
                                + awsProperties.getBaseimage().getImageId());
                    }
                    return dto;
                })
                .validate();
    }
}
