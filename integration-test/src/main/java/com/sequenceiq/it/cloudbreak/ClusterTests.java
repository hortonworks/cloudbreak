package com.sequenceiq.it.cloudbreak;

import com.sequenceiq.cloudbreak.api.model.stack.instance.InstanceGroupType;
import com.sequenceiq.cloudbreak.api.model.imagecatalog.ImageResponse;
import com.sequenceiq.cloudbreak.api.model.imagecatalog.ImagesResponse;
import com.sequenceiq.it.cloudbreak.newway.CloudbreakClient;
import com.sequenceiq.it.cloudbreak.newway.CloudbreakTest;
import com.sequenceiq.it.cloudbreak.newway.Cluster;
import com.sequenceiq.it.cloudbreak.newway.HostGroups;
import com.sequenceiq.it.cloudbreak.newway.ImageSettings;
import com.sequenceiq.it.cloudbreak.newway.Stack;
import com.sequenceiq.it.cloudbreak.newway.StackOperation;
import com.sequenceiq.it.cloudbreak.newway.cloud.CloudProvider;
import com.sequenceiq.it.cloudbreak.newway.cloud.CloudProviderHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.util.List;

public class ClusterTests extends CloudbreakTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(ClusterTests.class);

    private static final int DESIRED_NO = 2;

    @Test(dataProvider = "providernameblueprintimage", priority = 10)
    public void testCreateNewRegularCluster(CloudProvider cloudProvider, String clusterName, String blueprintName, String imageId) throws Exception {
        given(CloudbreakClient.isCreated());
        given(cloudProvider.aValidCredential());
        given(Cluster.request()
                        .withAmbariRequest(cloudProvider.ambariRequestWithBlueprintName(blueprintName)),
                "a cluster request");
        given(ImageSettings.request()
                .withImageCatalog("")
                .withImageId(imageId));
        given(cloudProvider.aValidStackRequest()
                .withName(clusterName), "a stack request");
        when(Stack.post(), "post the stack request");
        then(Stack.waitAndCheckClusterAndStackAvailabilityStatus(),
                "wait and check availability");
        then(Stack.checkClusterHasAmbariRunning(
                getTestParameter().get(CloudProviderHelper.DEFAULT_AMBARI_PORT),
                getTestParameter().get(CloudProviderHelper.DEFAULT_AMBARI_USER),
                getTestParameter().get(CloudProviderHelper.DEFAULT_AMBARI_PASSWORD)),
                "check ambari is running and components available");
    }

    @Test(dataProvider = "providernameblueprintimage", priority = 10)
    public void testCreateNewHdfCluster(CloudProvider cloudProvider, String clusterName, String blueprintName, String imageId) throws Exception {
        given(CloudbreakClient.isCreated());
        given(cloudProvider.aValidCredential());
        given(Cluster.request()
                        .withAmbariRequest(cloudProvider.ambariRequestWithBlueprintName(blueprintName)),
                "a cluster request");
        given(ImageSettings.request()
                .withImageCatalog("")
                .withImageId(imageId));
        given(HostGroups.request()
                .addHostGroup(cloudProvider.hostgroup("Services", InstanceGroupType.GATEWAY, 1))
                .addHostGroup(cloudProvider.hostgroup("NiFi", InstanceGroupType.CORE, 1)));
        given(cloudProvider.aValidStackRequest()
                .withName(clusterName), "a stack request");
        when(Stack.post(), "post the stack request");
        then(Stack.waitAndCheckClusterAndStackAvailabilityStatus(),
                "wait and check availability");
        then(Stack.checkClusterHasAmbariRunning(
                getTestParameter().get(CloudProviderHelper.DEFAULT_AMBARI_PORT),
                getTestParameter().get(CloudProviderHelper.DEFAULT_AMBARI_USER),
                getTestParameter().get(CloudProviderHelper.DEFAULT_AMBARI_PASSWORD)),
                "check ambari is running and components available");
    }

    @Test(dataProvider = "providernamehostgroupdesiredno", priority = 20)
    public void testScaleCluster(CloudProvider cloudProvider, String clusterName, String hostgroupName, int desiredCount) throws Exception {
        given(CloudbreakClient.isCreated());
        given(cloudProvider.aValidCredential());
        given(cloudProvider.aValidStackIsCreated()
                .withName(clusterName), "a stack is created");
        given(StackOperation.request()
                .withGroupName(hostgroupName)
                .withDesiredCount(desiredCount), "a scale request to " + hostgroupName);
        when(StackOperation.scale(), "scale");
        when(Stack.get());
        then(Stack.waitAndCheckClusterAndStackAvailabilityStatus(), "wait for availability");
        then(Stack.checkClusterHasAmbariRunning(
                getTestParameter().get(CloudProviderHelper.DEFAULT_AMBARI_PORT),
                getTestParameter().get(CloudProviderHelper.DEFAULT_AMBARI_USER),
                getTestParameter().get(CloudProviderHelper.DEFAULT_AMBARI_PASSWORD)),
                "check ambari");
    }

    @Test(dataProvider = "providername", priority = 30)
    public void testStopCluster(CloudProvider cloudProvider, String clusterName) throws Exception {
        given(CloudbreakClient.isCreated());
        given(cloudProvider.aValidCredential());
        given(cloudProvider.aValidStackIsCreated()
                .withName(clusterName), "a stack is created");
        given(StackOperation.request());
        when(StackOperation.stop());
        when(Stack.get());
        then(Stack.waitAndCheckClusterAndStackStoppedStatus(), "stack has been stopped");
    }

    @Test(dataProvider = "providername", priority = 40)
    public void testStartCluster(CloudProvider cloudProvider, String clusterName) throws Exception {
        given(CloudbreakClient.isCreated());
        given(cloudProvider.aValidCredential());
        given(cloudProvider.aValidStackIsCreated()
                .withName(clusterName), "a stack is created");
        given(StackOperation.request());
        when(StackOperation.start());
        when(Stack.get());
        then(Stack.waitAndCheckClusterAndStackAvailabilityStatus(), "stack has been started");
        then(Stack.checkClusterHasAmbariRunning(
                getTestParameter().get(CloudProviderHelper.DEFAULT_AMBARI_PORT),
                getTestParameter().get(CloudProviderHelper.DEFAULT_AMBARI_USER),
                getTestParameter().get(CloudProviderHelper.DEFAULT_AMBARI_PASSWORD)),
                "ambari check");
    }

    @Test(alwaysRun = true, dataProvider = "providername", priority = 50)
    public void testTerminateCluster(CloudProvider cloudProvider, String clusterName) throws Exception {
        given(CloudbreakClient.isCreated());
        given(cloudProvider.aValidCredential());
        given(cloudProvider.aValidStackIsCreated()
                .withName(clusterName), "a stack is created");
        when(Stack.delete());
        then(Stack.waitAndCheckClusterDeleted(), "stack has been deleted");
    }

    @DataProvider(name = "providernameblueprintimage")
    public Object[][] providerAndImage() throws Exception {
        String blueprint = getTestParameter().get("blueprintName");
        String provider = getTestParameter().get("provider").toLowerCase();
        String imageDescription = getTestParameter().get("image");
        CloudProvider cloudProvider = CloudProviderHelper.providerFactory(provider, getTestParameter());
        //String imageCatalog = getTestParameter().get("imageCatalog");
        String clusterName = getTestParameter().get("clusterName");
        String image = getImageId(provider, imageDescription);
        return new Object[][]{
                {cloudProvider, clusterName, blueprint, image}
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

    private String getImageId(String provider, String imageDescription) throws Exception {
        given(CloudbreakClient.isCreated());
        CloudbreakClient clientContext = CloudbreakClient.getTestContextCloudbreakClient().apply(getItContext());
        com.sequenceiq.cloudbreak.client.CloudbreakClient client = clientContext.getCloudbreakClient();
        ImagesResponse imagesByProvider = client.imageCatalogEndpoint().getImagesByProvider(provider);
        switch (imageDescription) {
            case "hdf":
                return getLastUuid(imagesByProvider.getHdfImages());
            case "hdp":
                return getLastUuid(imagesByProvider.getHdpImages());
            default:
                return getLastUuid(imagesByProvider.getBaseImages());
        }
    }

    private String getLastUuid(List<? extends ImageResponse> images) {
        return images.get(images.size() - 1).getUuid();
    }
}
