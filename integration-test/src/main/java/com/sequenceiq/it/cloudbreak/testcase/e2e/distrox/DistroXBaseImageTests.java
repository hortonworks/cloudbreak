package com.sequenceiq.it.cloudbreak.testcase.e2e.distrox;

import static com.sequenceiq.it.cloudbreak.context.RunningParameter.key;
import static java.lang.String.format;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.Test;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.InstanceStatus;
import com.sequenceiq.it.cloudbreak.client.DistroXTestClient;
import com.sequenceiq.it.cloudbreak.cloud.HostGroupType;
import com.sequenceiq.it.cloudbreak.cloud.v4.CloudProvider;
import com.sequenceiq.it.cloudbreak.context.Description;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.distrox.DistroXTestDto;
import com.sequenceiq.it.cloudbreak.dto.distrox.image.DistroXImageTestDto;
import com.sequenceiq.it.cloudbreak.dto.imagecatalog.ImageCatalogTestDto;
import com.sequenceiq.it.cloudbreak.exception.TestFailException;
import com.sequenceiq.it.cloudbreak.log.Log;
import com.sequenceiq.it.cloudbreak.testcase.e2e.AbstractE2ETest;
import com.sequenceiq.it.cloudbreak.util.wait.WaitUtil;

public class DistroXBaseImageTests extends AbstractE2ETest {
    private static final Logger LOGGER = LoggerFactory.getLogger(DistroXBaseImageTests.class);

    private Map<String, InstanceStatus> instancesHealthy = new HashMap<>() {{
        put(HostGroupType.MASTER.getName(), InstanceStatus.SERVICES_HEALTHY);
        put(HostGroupType.COMPUTE.getName(), InstanceStatus.SERVICES_HEALTHY);
        put(HostGroupType.WORKER.getName(), InstanceStatus.SERVICES_HEALTHY);
    }};

    @Inject
    private DistroXTestClient distroXTestClient;

    @Inject
    private WaitUtil waitUtil;

    @Override
    protected void setupTest(TestContext testContext) {
        createDefaultUser(testContext);
        createDefaultCredential(testContext);
        createEnvironmentWithNetworkAndFreeIPA(testContext);
        initializeDefaultBlueprints(testContext);
    }

    @Test(dataProvider = TEST_CONTEXT)
    @Description(
            given = "there is a running cloudbreak",
            when = "a valid DistroX create request is sent (latest Base Image)",
            then = "DistroX should be available and deletable")
    public void testDistroXWithBaseImageCanBeCreatedSuccessfully(TestContext testContext) {
        String imageSettings = resourcePropertyProvider().getName();
        String imageCatalog = resourcePropertyProvider().getName();
        String distrox = resourcePropertyProvider().getName();
        AtomicReference<String> selectedImageID = new AtomicReference<>();
        CloudProvider cloudProvider = testContext.getCloudProvider();

        testContext
                .given(imageCatalog, ImageCatalogTestDto.class)
                .when((tc, dto, client) -> {
                    selectedImageID.set(cloudProvider.getLatestBaseImageID(tc, dto, client));
                    return dto;
                })
                .given(imageSettings, DistroXImageTestDto.class).withImageCatalog(cloudProvider.getImageCatalogName()).withImageId(selectedImageID.get())
                .given(distrox, DistroXTestDto.class).withImageSettings(imageSettings)
                .when(distroXTestClient.create(), key(distrox))
                .await(STACK_AVAILABLE)
                .then((tc, testDto, client) -> {
                    return waitUtil.waitForDistroxInstancesStatus(testDto, client, instancesHealthy);
                })
                .then((tc, dto, client) -> {
                    Log.log(LOGGER, format(" Image Catalog Name: %s ", dto.getResponse().getImage().getCatalogName()));
                    Log.log(LOGGER, format(" Image Catalog URL: %s ", dto.getResponse().getImage().getCatalogUrl()));
                    Log.log(LOGGER, format(" Image ID: %s ", dto.getResponse().getImage().getId()));

                    if (!dto.getResponse().getImage().getId().equals(selectedImageID.get())) {
                        throw new TestFailException(" The selected image ID is: " + dto.getResponse().getImage().getId() + " instead of: "
                                + selectedImageID.get());
                    }
                    return dto;
                })
                .validate();
    }
}
