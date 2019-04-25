package com.sequenceiq.it.cloudbreak.newway.testcase.e2e.aws;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import com.sequenceiq.it.cloudbreak.newway.client.StackTestClient;
import com.sequenceiq.it.cloudbreak.newway.cloud.InstanceCountParameter;
import com.sequenceiq.it.cloudbreak.newway.cloud.v2.aws.AwsProperties;
import com.sequenceiq.it.cloudbreak.newway.cloud.v2.aws.AwsProperties.Baseimage;
import com.sequenceiq.it.cloudbreak.newway.context.Description;
import com.sequenceiq.it.cloudbreak.newway.context.TestCaseDescription;
import com.sequenceiq.it.cloudbreak.newway.context.TestCaseDescription.TestCaseDescriptionBuilder;
import com.sequenceiq.it.cloudbreak.newway.context.TestContext;
import com.sequenceiq.it.cloudbreak.newway.dto.ClusterTestDto;
import com.sequenceiq.it.cloudbreak.newway.dto.ImageSettingsTestDto;
import com.sequenceiq.it.cloudbreak.newway.dto.stack.StackTestDto;
import com.sequenceiq.it.cloudbreak.newway.testcase.e2e.AbstractE2ETest;

public class AwsBaseImageTests extends AbstractE2ETest {

    private static final String AMAZONLINUX2 = "amazonlinux2";

    private static final String REDHAT7 = "redhat7";

    private static final String SLES12 = "sles12";

    private static final String EDW_BLUEPRINT = "HDP 3.1 - EDW-Analytics: Apache Hive 2 LLAP, Apache Zeppelin";

    private static final String BASE_IMAGE_DATA_PROVIDER = "BASE_IMAGE_DATA_PROVIDER";

    @Inject
    private StackTestClient stackTestClient;

    @Inject
    private AwsProperties awsProperties;

    @Test(dataProvider = BASE_IMAGE_DATA_PROVIDER)
    public void testBaseImagesOnAwsWithBlueprints(
            TestContext testContext,
            String osName,
            String imageId,
            String blueprint,
            @Description TestCaseDescription testCaseDescription) {

        if (EDW_BLUEPRINT.equalsIgnoreCase(blueprint)) {
            getTestParameter().put(InstanceCountParameter.WORKER_INSTANCE_COUNT.getName(), "3");
        } else {
            getTestParameter().put(InstanceCountParameter.WORKER_INSTANCE_COUNT.getName(), "1");
        }

        testContext.given(ClusterTestDto.class)
                .withBlueprintName(blueprint)
                .given(ImageSettingsTestDto.class)
                .withOs(osName)
                .withImageId(imageId)
                .given(StackTestDto.class)
                .when(stackTestClient.createV4())
                .await(STACK_AVAILABLE)
                .validate();
    }

    @DataProvider(name = BASE_IMAGE_DATA_PROVIDER)
    public Object[][] dataProvider() {
        List<OsImageIdBlueprint> osImagesWithBlueprints = getOsImagesWithBlueprints();
        int dataRows = osImagesWithBlueprints.size();
        var data = new Object[dataRows][5];
        for (int row = 0; row < dataRows; row++) {
            OsImageIdBlueprint osImageWithBlueprint = osImagesWithBlueprints.get(row);
            data[row][0] = getBean(TestContext.class);
            data[row][1] = osImageWithBlueprint.os;
            data[row][2] = osImageWithBlueprint.imageId;
            data[row][3] = osImageWithBlueprint.blueprint;
            data[row][4] = new TestCaseDescriptionBuilder()
                    .given("there is a running cloudbreak")
                    .when(String.format("a stack create request is sent with '%s' OS image [image id: '%s'] and '%s' blueprint",
                            osImageWithBlueprint.os, osImageWithBlueprint.imageId, osImageWithBlueprint.blueprint))
                    .then("the stack creation should succeed");
        }
        return data;
    }

    private List<OsImageIdBlueprint> getOsImagesWithBlueprints() {
        Baseimage baseimage = awsProperties.getBaseimage();
        List<String> amazonLinux2Blueprints = baseimage.getAmazonlinux2().getBlueprints();
        List<String> redhat7Blueprints = baseimage.getRedhat7().getBlueprints();
        List<String> sles12Blueprints = baseimage.getSles12().getBlueprints();

        List<OsImageIdBlueprint> osImagesWithBlueprints = new ArrayList<>();
        amazonLinux2Blueprints.forEach(blueprint -> osImagesWithBlueprints.add(
                new OsImageIdBlueprint(AMAZONLINUX2, baseimage.getAmazonlinux2().getImageId(), blueprint)));
        redhat7Blueprints.forEach(blueprint -> osImagesWithBlueprints.add(
                new OsImageIdBlueprint(REDHAT7, baseimage.getRedhat7().getImageId(), blueprint)));
        sles12Blueprints.forEach(blueprint -> osImagesWithBlueprints.add(
                new OsImageIdBlueprint(SLES12, baseimage.getSles12().getImageId(), blueprint)
        ));
        return osImagesWithBlueprints;
    }

    private static class OsImageIdBlueprint {
        private final String os;

        private final String imageId;

        private final String blueprint;

        OsImageIdBlueprint(String os, String imageId, String blueprint) {
            this.os = os;
            this.imageId = imageId;
            this.blueprint = blueprint;
        }
    }

}
