package com.sequenceiq.it.cloudbreak;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import com.sequenceiq.cloudbreak.api.model.imagecatalog.ImageResponse;
import com.sequenceiq.cloudbreak.api.model.imagecatalog.ImagesResponse;
import com.sequenceiq.cloudbreak.api.model.stack.StackResponseEntries;
import com.sequenceiq.cloudbreak.api.model.stack.cluster.gateway.SSOType;
import com.sequenceiq.cloudbreak.api.model.v2.AmbariV2Request;
import com.sequenceiq.it.cloudbreak.newway.CloudbreakClient;
import com.sequenceiq.it.cloudbreak.newway.CloudbreakClusterTestConfiguration;
import com.sequenceiq.it.cloudbreak.newway.Cluster;
import com.sequenceiq.it.cloudbreak.newway.ClusterGateway;
import com.sequenceiq.it.cloudbreak.newway.GatewayTopology;
import com.sequenceiq.it.cloudbreak.newway.HostGroups;
import com.sequenceiq.it.cloudbreak.newway.ImageSettingsEntity;
import com.sequenceiq.it.cloudbreak.newway.KerberosEntity;
import com.sequenceiq.it.cloudbreak.newway.Stack;
import com.sequenceiq.it.cloudbreak.newway.StackGetWithEntriesStrategy;
import com.sequenceiq.it.cloudbreak.newway.StackImageChangeEntity;
import com.sequenceiq.it.cloudbreak.newway.StackOperationEntity;
import com.sequenceiq.it.cloudbreak.newway.cloud.CloudProvider;
import com.sequenceiq.it.cloudbreak.newway.cloud.CloudProviderHelper;
import com.sequenceiq.it.cloudbreak.newway.cloud.HostGroupType;
import com.sequenceiq.it.cloudbreak.newway.v3.StackV3Action;

public class ClusterTests extends CloudbreakClusterTestConfiguration {

    private static final Logger LOGGER = LoggerFactory.getLogger(ClusterTests.class);

    private static final int DESIRED_NO = 2;

    @Test(dataProvider = "providernameblueprint", priority = 10)
    public void testCreateNewRegularCluster(CloudProvider cloudProvider, String clusterName, String blueprintName) throws Exception {
        given(CloudbreakClient.created());
        given(cloudProvider.aValidCredential());
        given(Cluster.request()
                        .withAmbariRequest(cloudProvider.ambariRequestWithBlueprintName(blueprintName)),
                "a cluster request");
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

    @Test(dataProvider = "providernameblueprintkerberos", priority = 10)
    public void testCreateNewHdfCluster(CloudProvider cloudProvider, String clusterName, String blueprintName, boolean enableKerberos)
            throws Exception {
        given(CloudbreakClient.created());
        given(cloudProvider.aValidCredential());
        if (enableKerberos) {
            KerberosEntity kerberos = KerberosEntity.request()
                    .withMasterKey(KerberosEntity.DEFAULT_MASTERKEY)
                    .withAdmin(KerberosEntity.DEFAULT_ADMIN_USER)
                    .withPassword(KerberosEntity.DEFAULT_ADMIN_PASSWORD);
            given(kerberos);
        }
        AmbariV2Request ambariV2Request = cloudProvider.ambariRequestWithBlueprintName(blueprintName);
        given(Cluster.request().withAmbariRequest(ambariV2Request), "a cluster request");
        given(HostGroups.request()
                .addHostGroups(cloudProvider.instanceGroups(HostGroupType.SERVICES, HostGroupType.NIFI, HostGroupType.ZOOKEEPER)));
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

    @Test(dataProvider = "providernameblueprintimageos", priority = 10)
    public void testCreateNewClusterWithOs(CloudProvider cloudProvider, String clusterName, String blueprintName, String os, KerberosEntity kerberos)
            throws Exception {
        given(CloudbreakClient.created());
        given(cloudProvider.aValidCredential());
        given(kerberos);
        given(Cluster.request()
                        .withAmbariRequest(cloudProvider.ambariRequestWithBlueprintName(blueprintName)),
                "a cluster request");
        given(ImageSettingsEntity.request()
                .withImageCatalog("")
                .withOs(os));
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

    @Test(dataProvider = "providernameblueprint", priority = 10)
    public void testCreateNewClusterWithKnox(CloudProvider cloudProvider, String clusterName, String blueprintName) throws Exception {
        given(CloudbreakClient.created());
        given(cloudProvider.aValidCredential());
        given(Cluster.request()
                        .withAmbariRequest(cloudProvider.ambariRequestWithBlueprintName(blueprintName)),
                "a cluster request");
        given(ClusterGateway.request()
                .withPath("test-gateway")
                .withSsoType(SSOType.NONE)
                .withTopology(GatewayTopology.request()
                        .withName("test-topology")
                        .withExposedServices(Collections.singletonList("ALL"))
                )
        );
        given(cloudProvider.aValidStackRequest().withName(clusterName), "a stack request");

        when(Stack.post(), "post the stack request");

        then(Stack.waitAndCheckClusterAndStackAvailabilityStatus(),
                "wait and check availability");
        then(Stack.checkClusterHasAmbariRunningThroughKnox(
                getTestParameter().get(CloudProviderHelper.DEFAULT_AMBARI_USER),
                getTestParameter().get(CloudProviderHelper.DEFAULT_AMBARI_PASSWORD)),
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
                getTestParameter().get(CloudProviderHelper.DEFAULT_AMBARI_USER),
                getTestParameter().get(CloudProviderHelper.DEFAULT_AMBARI_PASSWORD)),
                "ambari check");
    }

    @Test(alwaysRun = true, dataProvider = "providernamekerberos", priority = 50)
    public void testTerminateCluster(CloudProvider cloudProvider, String clusterName, boolean enableKerberos) throws Exception {
        given(CloudbreakClient.created());
        given(cloudProvider.aValidCredential());
        given(cloudProvider.aValidStackCreated()
                .withName(clusterName), "a stack is created");
        if (enableKerberos) {
            when(Stack.delete(StackV3Action::deleteWithKerberos));
        } else {
            when(Stack.delete());
        }
        then(Stack.waitAndCheckClusterDeleted(), "stack has been deleted");
    }

    @DataProvider(name = "providernameblueprint")
    public Object[][] providerAndImage() throws Exception {
        String blueprint = getTestParameter().get("blueprintName");
        String provider = getTestParameter().get("provider").toLowerCase();
        CloudProvider cloudProvider = CloudProviderHelper.providerFactory(provider, getTestParameter());
        String clusterName = getTestParameter().get("clusterName");
        return new Object[][]{
                {cloudProvider, clusterName, blueprint}
        };
    }

    @DataProvider(name = "providernameblueprintkerberos")
    public Object[][] providerAndImageAndKerberos() throws Exception {
        String blueprint = getTestParameter().get("blueprintName");
        String provider = getTestParameter().get("provider").toLowerCase();
        CloudProvider cloudProvider = CloudProviderHelper.providerFactory(provider, getTestParameter());
        String clusterName = getTestParameter().get("clusterName");
        Boolean enableKerberos = Boolean.valueOf(getTestParameter().get("enableKerberos"));
        return new Object[][]{
                {cloudProvider, clusterName, blueprint, enableKerberos}
        };
    }

    @DataProvider(name = "providernameblueprintimageos")
    public Object[][] providerAndImageOs() {
        String blueprint = getTestParameter().get("blueprintName");
        String provider = getTestParameter().get("provider").toLowerCase();
        String imageOs = getTestParameter().get("imageos");
        CloudProvider cloudProvider = CloudProviderHelper.providerFactory(provider, getTestParameter());
        String clusterName = getTestParameter().get("clusterName");
        KerberosEntity kerberos = KerberosEntity.request()
                .withMasterKey(KerberosEntity.DEFAULT_MASTERKEY)
                .withAdmin(KerberosEntity.DEFAULT_ADMIN_USER)
                .withPassword(KerberosEntity.DEFAULT_ADMIN_PASSWORD);
        return new Object[][]{
                {cloudProvider, clusterName, blueprint, imageOs, kerberos}
        };
    }

    @DataProvider(name = "providernamehostgroupdesiredno")
    public Object[][] providerAndHostgroup() {
        String hostgroupName = getTestParameter().get("instancegroupName");
        String provider = getTestParameter().get("provider").toLowerCase();
        CloudProvider cloudProvider = CloudProviderHelper.providerFactory(provider, getTestParameter());
        String clusterName = getTestParameter().get("clusterName");
        return new Object[][]{
                {cloudProvider, clusterName, hostgroupName, DESIRED_NO}
        };
    }

    @DataProvider(name = "providername")
    public Object[][] providerClusterName() {
        String provider = getTestParameter().get("provider").toLowerCase();
        CloudProvider cloudProvider = CloudProviderHelper.providerFactory(provider, getTestParameter());
        String clusterName = getTestParameter().get("clusterName");
        return new Object[][]{
                {cloudProvider, clusterName}
        };
    }

    @DataProvider(name = "providernamekerberos")
    public Object[][] providerClusterNameWithKerberos() {
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
        ImagesResponse imagesByProvider = client.imageCatalogEndpoint().getImagesByProvider(provider);
        List<? extends ImageResponse> images;
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
                .max(Comparator.comparing(ImageResponse::getDate)).orElseThrow().getUuid();
    }
}
