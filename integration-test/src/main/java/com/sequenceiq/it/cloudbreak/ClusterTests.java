package com.sequenceiq.it.cloudbreak;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import com.sequenceiq.cloudbreak.api.endpoint.v4.imagecatalog.responses.ImageV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.imagecatalog.responses.ImagesV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.SSOType;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.StackResponseEntries;
import com.sequenceiq.it.cloudbreak.newway.CloudbreakClient;
import com.sequenceiq.it.cloudbreak.newway.CloudbreakClusterTestConfiguration;
import com.sequenceiq.it.cloudbreak.newway.Cluster;
import com.sequenceiq.it.cloudbreak.newway.ClusterGateway;
import com.sequenceiq.it.cloudbreak.newway.GatewayTopology;
import com.sequenceiq.it.cloudbreak.newway.HostGroups;
import com.sequenceiq.it.cloudbreak.newway.ImageSettingsEntity;
import com.sequenceiq.it.cloudbreak.newway.Stack;
import com.sequenceiq.it.cloudbreak.newway.StackGetWithEntriesStrategy;
import com.sequenceiq.it.cloudbreak.newway.StackImageChangeEntity;
import com.sequenceiq.it.cloudbreak.newway.StackOperationEntity;
import com.sequenceiq.it.cloudbreak.newway.cloud.AwsKerberos;
import com.sequenceiq.it.cloudbreak.newway.cloud.CloudProvider;
import com.sequenceiq.it.cloudbreak.newway.cloud.CloudProviderHelper;
import com.sequenceiq.it.cloudbreak.newway.cloud.HostGroupType;
import com.sequenceiq.it.cloudbreak.newway.v3.StackActionV4;

public class ClusterTests extends CloudbreakClusterTestConfiguration {

    private static final Logger LOGGER = LoggerFactory.getLogger(ClusterTests.class);

    private static final int DESIRED_NO = 2;

    @Test(dataProvider = "providernameblueprintimage", priority = 10)
    public void testCreateNewRegularCluster(CloudProvider cloudProvider, String clusterName, String clusterDefinitionName, String imageId) throws Exception {
        given(CloudbreakClient.created());
        given(cloudProvider.aValidCredential());
        given(Cluster.request()
                        .withAmbariRequest(cloudProvider.ambariRequestWithBlueprintName(clusterDefinitionName)),
                "a cluster request");
        given(ImageSettingsEntity.request()
                .withImageCatalog("")
                .withImageId(imageId));
        given(cloudProvider.aValidStackRequest()
                .withName(clusterName), "a stack request");
        when(Stack.postV3(), "post the stack request");
        then(Stack.waitAndCheckClusterAndStackAvailabilityStatus(),
                "wait and check availability");
        then(Stack.checkClusterHasAmbariRunning(
                getTestParameter().get(CloudProviderHelper.DEFAULT_AMBARI_PORT),
                getTestParameter().get(CloudProviderHelper.DEFAULT_AMBARI_USER),
                getTestParameter().get(CloudProviderHelper.DEFAULT_AMBARI_PASSWORD)),
                "check ambari is running and components available");
    }

    @Test(dataProvider = "providernameblueprintimage", priority = 10)
    public void testCreateHdfCluster(CloudProvider cloudProvider, String clusterName, String clusterDefinitionName, String imageId) throws Exception {
        given(CloudbreakClient.created());
        given(cloudProvider.aValidCredential());
        given(AwsKerberos.kerberosOnAws(getTestParameter()));
        given(Cluster.request()
                        .withKerberos(AwsKerberos.KERBEROS_CLOUDY)
                        .withAmbariRequest(AwsKerberos.getAmbariV2Request(cloudProvider, clusterDefinitionName, getTestParameter())),
                "a cluster request");
        given(ImageSettingsEntity.request()
                .withImageCatalog("")
                .withImageId(imageId));
        given(HostGroups.request()
                .addHostGroups(cloudProvider.instanceGroups(HostGroupType.SERVICES, HostGroupType.NIFI, HostGroupType.ZOOKEEPER)));
        given(cloudProvider.aValidStackRequest()
                .withName(clusterName)
                .withNetwork(AwsKerberos.getNetworkV2RequestForKerberosAws(getTestParameter())), "a stack request");
        when(Stack.postV3());
        then(Stack.waitAndCheckClusterAndStackAvailabilityStatus(),
                "wait and check availability");
        then(Stack.checkClusterHasAmbariRunning(
                getTestParameter().get(CloudProviderHelper.DEFAULT_AMBARI_PORT),
                AwsKerberos.USERNAME,
                getTestParameter().get(AwsKerberos.AD_PASSWORD)),
                "check ambari is running and components available");
    }

    @Test(dataProvider = "providernameblueprintimageos", priority = 10)
    public void testCreateNewClusterWithOs(CloudProvider cloudProvider, String clusterName, String clusterDefinitionName, String os)
            throws Exception {
        given(CloudbreakClient.created());
        given(cloudProvider.aValidCredential());
        given(Cluster.request()
                        .withAmbariRequest(cloudProvider.ambariRequestWithBlueprintName(clusterDefinitionName)),
                "a cluster request");
        given(ImageSettingsEntity.request()
                .withImageCatalog("")
                .withOs(os));
        given(cloudProvider.aValidStackRequest()
                .withName(clusterName), "a stack request");
        when(Stack.postV3(), "post the stack request");
        then(Stack.waitAndCheckClusterAndStackAvailabilityStatus(),
                "wait and check availability");
        then(Stack.checkClusterHasAmbariRunning(
                getTestParameter().get(CloudProviderHelper.DEFAULT_AMBARI_PORT),
                getTestParameter().get(CloudProviderHelper.DEFAULT_AMBARI_USER),
                getTestParameter().get(CloudProviderHelper.DEFAULT_AMBARI_PASSWORD)),
                "check ambari is running and components available");
    }

    @Test(dataProvider = "providernameblueprintimage", priority = 10)
    public void testCreateNewClusterWithKnox(CloudProvider cloudProvider, String clusterName, String clusterDefinitionName, String imageId) throws Exception {
        given(CloudbreakClient.created());
        given(cloudProvider.aValidCredential());
        given(Cluster.request()
                        .withAmbariRequest(cloudProvider.ambariRequestWithBlueprintName(clusterDefinitionName)),
                "a cluster request");
        given(ImageSettingsEntity.request()
                .withImageCatalog("")
                .withImageId(imageId));
        given(ClusterGateway.request()
                .withPath("test-gateway")
                .withSsoType(SSOType.NONE)
                .withTopology(GatewayTopology.request()
                        .withName("test-topology")
                        .withExposedServices(Collections.singletonList("ALL"))
                )
        );
        given(cloudProvider.aValidStackRequest().withName(clusterName), "a stack request");

        when(Stack.postV3(), "post the stack request");

        then(Stack.waitAndCheckClusterAndStackAvailabilityStatus(),
                "wait and check availability");
        then(Stack.checkClusterHasAmbariRunningThroughKnox(),
                "check if ambari is available through knox");
    }

    @Test(dataProvider = "providername", priority = 15)
    public void testModifyImage(CloudProvider cloudProvider, String clusterName) throws Exception {
        given(CloudbreakClient.created());
        given(cloudProvider.aValidCredential());
        given(cloudProvider.aValidStackCreated()
                .withName(clusterName), "a stack is created");
        String provider = getTestParameter().get("provider").toLowerCase();
        String imageId = getImageIdWithPkgVersions(provider, "");
        given(StackImageChangeEntity.request().withImageId(imageId));

        when(StackImageChangeEntity.changeImage(), "changeImage");
        when(Stack.get());

        then(Stack.waitAndCheckClusterAndStackAvailabilityStatus(),
                "wait and check availability");
        when(Stack.get());
        then(Stack.checkImage(imageId, null));
    }

    @Test(dataProvider = "providernamehostgroupdesiredno", priority = 20)
    public void testScaleCluster(CloudProvider cloudProvider, String clusterName, String hostgroupName, int desiredCount) throws Exception {
        given(CloudbreakClient.created());
        given(cloudProvider.aValidCredential());
        given(cloudProvider.aValidStackCreated()
                .withName(clusterName), "a stack is created");
        given(StackOperationEntity.request()
                .withGroupName(hostgroupName)
                .withDesiredCount(desiredCount), "a scale request to " + hostgroupName);
        when(StackOperationEntity.scale(), "scale");
        when(Stack.get());
        then(Stack.waitAndCheckClusterAndStackAvailabilityStatus(), "wait for availability");
        then(Stack.checkClusterHasAmbariRunning(
                getTestParameter().get(CloudProviderHelper.DEFAULT_AMBARI_PORT),
                getTestParameter().get(CloudProviderHelper.DEFAULT_AMBARI_USER),
                getTestParameter().get(CloudProviderHelper.DEFAULT_AMBARI_PASSWORD)),
                "check ambari");
    }

    @Test(dataProvider = "providername", priority = 25)
    public void testStackImagesDifferent(CloudProvider cloudProvider, String clusterName) throws Exception {
        given(CloudbreakClient.created());
        given(cloudProvider.aValidCredential());
        given(cloudProvider.aValidStackCreated()
                .withName(clusterName), "a stack is created");

        when(Stack.get(StackGetWithEntriesStrategy.create(Collections.singleton(StackResponseEntries.HARDWARE_INFO))));

        then(Stack.waitAndCheckClusterAndStackAvailabilityStatus(),
                "wait and check availability");
        then(Stack.checkImagesDifferent());

    }

    @Test(dataProvider = "providername", priority = 30)
    public void testStopCluster(CloudProvider cloudProvider, String clusterName) throws Exception {
        given(CloudbreakClient.created());
        given(cloudProvider.aValidCredential());
        given(cloudProvider.aValidStackCreated()
                .withName(clusterName), "a stack is created");
        given(StackOperationEntity.request());
        when(StackOperationEntity.stop());
        when(Stack.get());
        then(Stack.waitAndCheckClusterAndStackStoppedStatus(), "stack has been stopped");
    }

    @Test(dataProvider = "providername", priority = 40)
    public void testStartCluster(CloudProvider cloudProvider, String clusterName) throws Exception {
        startClusterWithAmbariCredential(cloudProvider,
                clusterName,
                getTestParameter().get(CloudProviderHelper.DEFAULT_AMBARI_USER),
                getTestParameter().get(CloudProviderHelper.DEFAULT_AMBARI_PASSWORD));
    }

    @Test(dataProvider = "providername", priority = 40)
    public void testStartKerberizedCluster(CloudProvider cloudProvider, String clusterName) throws Exception {
        startClusterWithAmbariCredential(cloudProvider,
                clusterName,
                AwsKerberos.USERNAME,
                getTestParameter().get(AwsKerberos.AD_PASSWORD));
    }

    @Test(alwaysRun = true, dataProvider = "providername", priority = 50)
    public void testTerminateCluster(CloudProvider cloudProvider, String clusterName) throws Exception {
        given(CloudbreakClient.created());
        given(cloudProvider.aValidCredential());
        given(cloudProvider.aValidStackCreated()
                .withName(clusterName), "a stack is created");
        when(Stack.delete());
        then(Stack.waitAndCheckClusterDeleted(), "stack has been deleted");
    }

    @Test(alwaysRun = true, dataProvider = "providername", priority = 50)
    public void testTerminateKerberizedCluster(CloudProvider cloudProvider, String clusterName) throws Exception {
        given(CloudbreakClient.created());
        given(cloudProvider.aValidCredential());
        given(cloudProvider.aValidStackCreated()
                .withName(clusterName), "a stack is created");
        when(Stack.delete(StackActionV4::deleteWithKerberos));
        then(Stack.waitAndCheckClusterDeleted(), "stack has been deleted");
    }

    @DataProvider(name = "providernameblueprintimageos")
    public Object[][] providerAndImageOs() {
        String clusterDefinition = getTestParameter().get("clusterDefinitionName");
        String provider = getTestParameter().get("provider").toLowerCase();
        String imageOs = getTestParameter().get("imageos");
        CloudProvider cloudProvider = CloudProviderHelper.providerFactory(provider, getTestParameter());
        String clusterName = getTestParameter().get("clusterName");
        return new Object[][]{
                {cloudProvider, clusterName, clusterDefinition, imageOs}
        };
    }

    @DataProvider(name = "providernamehostgroupdesiredno")
    public Object[][] providerAndHostgroup() throws Exception {
        String hostgroupName = getTestParameter().get("instancegroupName");
        String provider = getTestParameter().get("provider").toLowerCase();
        CloudProvider cloudProvider = CloudProviderHelper.providerFactory(provider, getTestParameter());
        String clusterName = getTestParameter().get("clusterName");
        return new Object[][]{
                {cloudProvider, clusterName, hostgroupName, DESIRED_NO}
        };
    }

    @DataProvider(name = "providername")
    public Object[][] providerClusterName() throws Exception {
        String provider = getTestParameter().get("provider").toLowerCase();
        CloudProvider cloudProvider = CloudProviderHelper.providerFactory(provider, getTestParameter());
        String clusterName = getTestParameter().get("clusterName");
        return new Object[][]{
                {cloudProvider, clusterName}
        };
    }

    @DataProvider(name = "providernamekerberos")
    public Object[][] providerClusterNameWithKerberos() throws Exception {
        String provider = getTestParameter().get("provider").toLowerCase();
        CloudProvider cloudProvider = CloudProviderHelper.providerFactory(provider, getTestParameter());
        String clusterName = getTestParameter().get("clusterName");
        Boolean enableKerberos = Boolean.valueOf(getTestParameter().get("enableKerberos"));
        return new Object[][]{
                {cloudProvider, clusterName, enableKerberos}
        };
    }

    protected String getImageIdWithPkgVersions(String provider, String imageDescription) throws Exception {
        given(CloudbreakClient.created());
        CloudbreakClient clientContext = CloudbreakClient.getTestContextCloudbreakClient().apply(getItContext());
        com.sequenceiq.cloudbreak.client.CloudbreakClient client = clientContext.getCloudbreakClient();
        ImagesV4Response imagesByProvider = client.imageCatalogV4Endpoint().getImages(clientContext.getWorkspaceId(), null, provider);
        List<? extends ImageV4Response> images;
        switch (imageDescription) {
            case "hdf":
                images = imagesByProvider.getHdfImages();
                break;
            case "hdp":
                images = imagesByProvider.getHdpImages();
                break;
            default:
                images = imagesByProvider.getBaseImages();
                break;
        }

        return images.stream().filter(imageResponse -> imageResponse.getPackageVersions() != null)
                .max(Comparator.comparing(ImageV4Response::getDate)).orElseThrow().getUuid();
    }

    private String getLastUuid(List<? extends ImageV4Response> images, String stackVersion) {
        List<? extends ImageV4Response> result = images.stream()
                .filter(ImageV4Response::isDefaultImage)
                .filter(image -> {
                    ImageV4Response imageV4Response = (ImageV4Response) image;
                    if (!StringUtils.isEmpty(imageV4Response.getVersion())) {
                        return imageV4Response.getVersion().startsWith(stackVersion);
                    }
                    if (imageV4Response.getStackDetails() == null) {
                        return true;
                    }
                    if (imageV4Response.getStackDetails().getVersion() == null) {
                        return true;
                    }
                    return image.getStackDetails().getVersion().startsWith(stackVersion);
                })
                .collect(Collectors.toList());
        if (result.isEmpty()) {
            result = images;
        }
        result = result.stream().sorted(Comparator.comparing(ImageV4Response::getDate)).collect(Collectors.toList());
        return result.get(result.size() - 1).getUuid();
    }

    private void startClusterWithAmbariCredential(CloudProvider cloudProvider, String clusterName, String ambariUser, String ambariPassword) throws Exception {
        given(CloudbreakClient.created());
        given(cloudProvider.aValidCredential());
        given(cloudProvider.aValidStackCreated()
                .withName(clusterName), "a stack is created");
        given(StackOperationEntity.request());
        when(StackOperationEntity.start());
        when(Stack.get());
        then(Stack.waitAndCheckClusterAndStackAvailabilityStatus(), "stack has been started");
        then(Stack.checkClusterHasAmbariRunning(
                getTestParameter().get(CloudProviderHelper.DEFAULT_AMBARI_PORT),
                ambariUser,
                ambariPassword),
                "ambari check");
    }
}
