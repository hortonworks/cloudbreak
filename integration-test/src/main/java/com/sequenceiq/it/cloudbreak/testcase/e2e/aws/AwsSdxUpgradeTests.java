package com.sequenceiq.it.cloudbreak.testcase.e2e.aws;

import static com.sequenceiq.it.cloudbreak.context.RunningParameter.key;
import static java.lang.String.format;

import java.util.concurrent.atomic.AtomicReference;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.it.cloudbreak.client.SdxTestClient;
import com.sequenceiq.it.cloudbreak.cloud.v4.aws.AwsCloudProvider;
import com.sequenceiq.it.cloudbreak.context.Description;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.ClouderaManagerTestDto;
import com.sequenceiq.it.cloudbreak.dto.ClusterTestDto;
import com.sequenceiq.it.cloudbreak.dto.ImageSettingsTestDto;
import com.sequenceiq.it.cloudbreak.dto.imagecatalog.ImageCatalogTestDto;
import com.sequenceiq.it.cloudbreak.dto.sdx.SdxInternalTestDto;
import com.sequenceiq.it.cloudbreak.dto.stack.StackTestDto;
import com.sequenceiq.it.cloudbreak.exception.TestFailException;
import com.sequenceiq.it.cloudbreak.log.Log;
import com.sequenceiq.it.cloudbreak.testcase.e2e.BasicSdxTests;
import com.sequenceiq.sdx.api.model.SdxClusterStatusResponse;

public class AwsSdxUpgradeTests extends BasicSdxTests {
    private static final Logger LOGGER = LoggerFactory.getLogger(AwsSdxUpgradeTests.class);

    @Inject
    private SdxTestClient sdxTestClient;

    @Inject
    private AwsCloudProvider awsCloudProvider;

    @Override
    protected void setupTest(TestContext testContext) {
        createDefaultUser(testContext);
        createDefaultCredential(testContext);
        createEnvironmentForSdx(testContext);
        initializeDefaultBlueprints(testContext);
    }

    /**
     * This test case is disabled right now.
     *
     * @param testContext   Stores and shares test objects through test execution between individual test cases.
     *
     * The 'disabled' tag on method name and the '@Test(dataProvider = TEST_CONTEXT)' annotation should be restored in case of resume this test case.
     */
    @Description(
            given = "there is a running Cloudbreak, and an SDX cluster in available state",
            when = "a newer image is available for CentOS7 on AWS in eu-west-1",
            then = "image upgrade notification should be available"
    )
    public void disabledTestSDXWithOlderImageCanBeCreatedSuccessfully(TestContext testContext) {
        String sdxInternal = resourcePropertyProvider().getName();
        String cluster = resourcePropertyProvider().getName();
        String clouderaManager = resourcePropertyProvider().getName();
        String imageSettings = resourcePropertyProvider().getName();
        String imageCatalog = resourcePropertyProvider().getName();
        String stack = resourcePropertyProvider().getName();
        AtomicReference<String> selectedImageID = new AtomicReference<>();

        testContext
                .given(imageCatalog, ImageCatalogTestDto.class)
                .when((tc, dto, client) -> {
                    selectedImageID.set(awsCloudProvider.getPreviousAWSPreWarmedImageID(tc, dto, client));
                    return dto;
                })
                .given(imageSettings, ImageSettingsTestDto.class)
                .given(clouderaManager, ClouderaManagerTestDto.class)
                .given(cluster, ClusterTestDto.class).withClouderaManager(clouderaManager)
                .given(stack, StackTestDto.class).withCluster(cluster).withImageSettings(imageSettings)
                .given(sdxInternal, SdxInternalTestDto.class).withStackRequest(stack, cluster)
                .when(sdxTestClient.createInternal(), key(sdxInternal))
                .await(SdxClusterStatusResponse.RUNNING)
                .then((tc, dto, client) -> {
                    Log.log(LOGGER, format(" Image Catalog Name: %s ", dto.getResponse().getStackV4Response().getImage().getCatalogName()));
                    Log.log(LOGGER, format(" Image Catalog URL: %s ", dto.getResponse().getStackV4Response().getImage().getCatalogUrl()));
                    Log.log(LOGGER, format(" Image ID: %s ", dto.getResponse().getStackV4Response().getImage().getId()));

                    if (!dto.getResponse().getStackV4Response().getImage().getId().equals(selectedImageID.get())) {
                        throw new TestFailException(" The selected image ID is: " + dto.getResponse().getStackV4Response().getImage().getId() + " instead of: "
                                + selectedImageID.get());
                    }
                    return dto;
                })
                .validate();
    }
}
