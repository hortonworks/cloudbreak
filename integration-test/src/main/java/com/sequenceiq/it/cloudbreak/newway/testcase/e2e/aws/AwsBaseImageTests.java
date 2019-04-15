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

    private static final String EDW_CLUSTER_DEFINITION = "HDP 3.1 - EDW-Analytics: Apache Hive 2 LLAP, Apache Zeppelin";

    private static final String BASE_IMAGE_DATA_PROVIDER = "BASE_IMAGE_DATA_PROVIDER";

    @Inject
    private StackTestClient stackTestClient;

    @Inject
    private AwsProperties awsProperties;

    @Test(dataProvider = BASE_IMAGE_DATA_PROVIDER)
    public void testBaseImagesOnAwsWithClusterDefs(
            TestContext testContext,
            String osName,
            String imageId,
            String clusterDefinition,
            @Description TestCaseDescription testCaseDescription) {

        if (EDW_CLUSTER_DEFINITION.equalsIgnoreCase(clusterDefinition)) {
            getTestParameter().put(InstanceCountParameter.WORKER_INSTANCE_COUNT.getName(), "3");
        } else {
            getTestParameter().put(InstanceCountParameter.WORKER_INSTANCE_COUNT.getName(), "1");
        }

        testContext.given(ClusterTestDto.class)
                .withClusterDefinitionName(clusterDefinition)
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
        List<OsImageIdClusterDef> osImagesWithClusterDefinitions = getOsImagesWithClusterDefinitions();
        int dataRows = osImagesWithClusterDefinitions.size();
        var data = new Object[dataRows][5];
        for (int row = 0; row < dataRows; row++) {
            OsImageIdClusterDef osImageWithClusterDef = osImagesWithClusterDefinitions.get(row);
            data[row][0] = getBean(TestContext.class);
            data[row][1] = osImageWithClusterDef.os;
            data[row][2] = osImageWithClusterDef.imageId;
            data[row][3] = osImageWithClusterDef.clusterDef;
            data[row][4] = new TestCaseDescriptionBuilder()
                    .given("there is a running cloudbreak")
                    .when(String.format("a stack create request is sent with '%s' OS image [image id: '%s'] and '%s' cluster definition",
                            osImageWithClusterDef.os, osImageWithClusterDef.imageId, osImageWithClusterDef.clusterDef))
                    .then("the stack creation should succeed");
        }
        return data;
    }

    private List<OsImageIdClusterDef> getOsImagesWithClusterDefinitions() {
        Baseimage baseimage = awsProperties.getBaseimage();
        List<String> amazonLinux2ClusterDefinitions = baseimage.getAmazonlinux2().getClusterDefinitions();
        List<String> redhat7ClusterDefinitions = baseimage.getRedhat7().getClusterDefinitions();
        List<String> sles12ClusterDefinitions = baseimage.getSles12().getClusterDefinitions();

        List<OsImageIdClusterDef> osImagesWithClusterDefinitions = new ArrayList<>();
        amazonLinux2ClusterDefinitions.forEach(clusterDef -> osImagesWithClusterDefinitions.add(
                new OsImageIdClusterDef(AMAZONLINUX2, baseimage.getAmazonlinux2().getImageId(), clusterDef)));
        redhat7ClusterDefinitions.forEach(clusterDef -> osImagesWithClusterDefinitions.add(
                new OsImageIdClusterDef(REDHAT7, baseimage.getRedhat7().getImageId(), clusterDef)));
        sles12ClusterDefinitions.forEach(clusterDef -> osImagesWithClusterDefinitions.add(
                new OsImageIdClusterDef(SLES12, baseimage.getSles12().getImageId(), clusterDef)
        ));
        return osImagesWithClusterDefinitions;
    }

    private static class OsImageIdClusterDef {
        private final String os;

        private final String imageId;

        private final String clusterDef;

        OsImageIdClusterDef(String os, String imageId, String clusterDef) {
            this.os = os;
            this.imageId = imageId;
            this.clusterDef = clusterDef;
        }
    }

}
