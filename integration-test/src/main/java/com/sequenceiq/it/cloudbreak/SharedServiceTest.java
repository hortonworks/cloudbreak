package com.sequenceiq.it.cloudbreak;

import com.sequenceiq.cloudbreak.api.model.imagecatalog.ImageResponse;
import com.sequenceiq.cloudbreak.api.model.imagecatalog.ImagesResponse;
import com.sequenceiq.cloudbreak.api.model.stack.instance.InstanceGroupType;
import com.sequenceiq.cloudbreak.api.model.v2.CloudStorageRequest;
import com.sequenceiq.cloudbreak.api.model.v2.InstanceGroupV2Request;
import com.sequenceiq.cloudbreak.api.model.v2.StorageLocationRequest;
import com.sequenceiq.cloudbreak.api.model.v2.filesystem.S3CloudStorageParameters;
import com.sequenceiq.it.cloudbreak.newway.CloudbreakClient;
import com.sequenceiq.it.cloudbreak.newway.CloudbreakTest;
import com.sequenceiq.it.cloudbreak.newway.Cluster;
import com.sequenceiq.it.cloudbreak.newway.HiveRdsConfig;
import com.sequenceiq.it.cloudbreak.newway.LdapConfig;
import com.sequenceiq.it.cloudbreak.newway.RangerRdsConfig;
import com.sequenceiq.it.cloudbreak.newway.Stack;
import com.sequenceiq.it.cloudbreak.newway.cloud.CloudProvider;
import com.sequenceiq.it.cloudbreak.newway.cloud.CloudProviderHelper;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static java.util.Collections.singletonList;

public class SharedServiceTest extends CloudbreakTest {

    private static final String DATALAKE_BLUEPRINT_NAME = "Data Lake: Apache Ranger, Apache Hive Metastore";

    private static final String ATTACHED_CLUSTER_BLUEPRINT_NAME = "Data Science: Apache Spark 2, Apache Zeppelin";

    private static final String DATALAKE_CLUSTER_NAME = "datalake-cluster";

    private static final String ATTACHED_CLUSTER_NAME = "attached-cluster";

    @Test(dataProvider = "providerAndBlueprintAndClusterNameForDatalake")
    public void testADatalakeClusterCreation(CloudProvider cloudProvider, String clusterName, String blueprintName) throws Exception {
        given(CloudbreakClient.isCreated());
        given(cloudProvider.aValidCredential());
        given(RangerRdsConfig.isCreatedWithParameters(getTestParameter()));
        given(HiveRdsConfig.isCreatedWithParameters(getTestParameter()));
        given(LdapConfig.isCreatedWithParameters(getTestParameter()));
        given(Cluster.request()
                        .withAmbariRequest(cloudProvider.ambariRequestWithBlueprintName(blueprintName))
                        .withCloudStorage(getCloudStorageForDatalake())
                        .withRdsConfigNames(new HashSet<>(Arrays.asList(getTestParameter().get("NN_RDS_RANGER_CONFIG_NAME"),
                                getTestParameter().get("NN_RDS_HIVE_CONFIG_NAME"))))
                        .withLdapConfigName(getTestParameter().get("NN_LDAP")),
                "a datalake cluster request");
        InstanceGroupV2Request hg = cloudProvider.hostgroup("master", InstanceGroupType.GATEWAY, 1);
        given(cloudProvider.aValidStackRequest().withInstanceGroups(singletonList(hg))
                .withName(clusterName));
        when(Stack.post());
        then(Stack.waitAndCheckClusterAndStackAvailabilityStatus(),
                "wait and check availability");
        then(Stack.checkClusterHasAmbariRunning(
                getTestParameter().get(CloudProviderHelper.DEFAULT_AMBARI_PORT),
                getTestParameter().get(CloudProviderHelper.DEFAULT_AMBARI_USER),
                getTestParameter().get(CloudProviderHelper.DEFAULT_AMBARI_PASSWORD)),
                "check ambari is running and components available");
    }

    @Test(dataProvider = "providerAndBlueprintAndClusterNameForAttachedCluster")
    public void testClusterAttachedToDatalakeCluster(CloudProvider cloudProvider, String clusterName, String datalakeClusterName, String blueprintName)
            throws Exception {
        given(CloudbreakClient.isCreated());
        given(cloudProvider.aValidCredential());
        given(Cluster.request()
                        .withSharedService(datalakeClusterName)
                        .withAmbariRequest(cloudProvider.ambariRequestWithBlueprintName(blueprintName))
                        .withCloudStorage(getCloudStorageForAttachedCluster())
                        .withRdsConfigNames(new HashSet<>(Arrays.asList(
                                getTestParameter().get("NN_RDS_RANGER_CONFIG_NAME"),
                                getTestParameter().get("NN_RDS_HIVE_CONFIG_NAME"))))
                        .withLdapConfigName(getTestParameter().get("NN_LDAP")),
                "an attached cluster request");
        given(cloudProvider.aValidStackRequest().withName(clusterName));

        when(Stack.post());

        then(Stack.waitAndCheckClusterAndStackAvailabilityStatus(), "wait and check availability");
        then(Stack.checkClusterHasAmbariRunning(
                getTestParameter().get(CloudProviderHelper.DEFAULT_AMBARI_PORT),
                getTestParameter().get(CloudProviderHelper.DEFAULT_AMBARI_USER),
                getTestParameter().get(CloudProviderHelper.DEFAULT_AMBARI_PASSWORD)),
                "check ambari is running and components available");
    }

    @Test(alwaysRun = true, dataProvider = "providerAndBlueprintAndClusterNameForAttachedCluster")
    public void testTerminateAttachedCluster(CloudProvider cloudProvider, String attachedClusterName, String datalakeClusterName, String blueprintName)
            throws Exception {
        given(CloudbreakClient.isCreated());
        given(cloudProvider.aValidCredential());
        given(cloudProvider.aValidStackIsCreated()
                .withName(attachedClusterName), "a stack is created");
        when(Stack.delete());
        then(Stack.waitAndCheckClusterDeleted(), "stack has been deleted");
    }

    @Test(alwaysRun = true, dataProvider = "providerAndBlueprintAndClusterNameForAttachedCluster")
    public void testTerminateDatalakeCluster(CloudProvider cloudProvider, String attachedClusterName, String datalakeClusterName, String blueprintName)
            throws Exception {
        given(CloudbreakClient.isCreated());
        given(cloudProvider.aValidCredential());
        given(cloudProvider.aValidStackIsCreated()
                .withName(datalakeClusterName), "a stack is created");
        when(Stack.delete());
        then(Stack.waitAndCheckClusterDeleted(), "stack has been deleted");
    }

    private CloudStorageRequest getCloudStorageForDatalake() {
        CloudStorageRequest request = new CloudStorageRequest();
        request.setS3(s3CloudStorage());
        request.setLocations(defaultDatalakeStorageLocationsForProvider());

        return request;
    }

    private CloudStorageRequest getCloudStorageForAttachedCluster() {
        CloudStorageRequest request = new CloudStorageRequest();
        Set<StorageLocationRequest> locations = new LinkedHashSet<>(1);
        locations.add(
                createLocation(
                        String.format("s3a://%s/attached/apps/hive/warehouse", getTestParameter().get("NN_AWS_S3_BUCKET_NAME")),
                        "hive-site",
                        "hive.metastore.warehouse.dir"));
        request.setLocations(locations);
        request.setS3(s3CloudStorage());
        return request;
    }

    private S3CloudStorageParameters s3CloudStorage() {
        S3CloudStorageParameters s3 = new S3CloudStorageParameters();
        s3.setInstanceProfile(getTestParameter().get("NN_AWS_INSTANCE_PROFILE"));
        return s3;
    }

    private Set<StorageLocationRequest> defaultDatalakeStorageLocationsForProvider() {
        Set<StorageLocationRequest> request = new LinkedHashSet<>(2);

        request.add(createLocation(
                String.format("s3a://%s/apps/hive/warehouse", getTestParameter().get("NN_AWS_S3_BUCKET_NAME")),
                "hive-site",
                "hive.metastore.warehouse.dir"));
        request.add(createLocation(
                String.format("s3a://%s/apps/ranger/audit", getTestParameter().get("NN_AWS_S3_BUCKET_NAME")),
                "ranger-env",
                "xasecure.audit.destination.hdfs.dir"));

        return request;
    }

    private StorageLocationRequest createLocation(String value, String propertyFile, String propertyName) {
        StorageLocationRequest location = new StorageLocationRequest();
        location.setValue(value);
        location.setPropertyFile(propertyFile);
        location.setPropertyName(propertyName);
        return location;
    }

    @DataProvider(name = "providernameimage")
    public Object[][] providerAndImage() throws Exception {
        String provider = getTestParameter().get("provider").toLowerCase();
        CloudProvider cloudProvider = CloudProviderHelper.providerFactory(provider, getTestParameter());
        String clusterName = getTestParameter().get("clusterName");
        String image = getImageId(provider);
        return new Object[][]{
                {cloudProvider, clusterName, image}
        };
    }

    @DataProvider(name = "providerAndBlueprintAndClusterNameForDatalake")
    public Object[][] providerClusterNameForDatalake() {
        CloudProvider cloudProvider = CloudProviderHelper.providerFactory("aws", getTestParameter());
        return new Object[][]{
                {cloudProvider, DATALAKE_CLUSTER_NAME, DATALAKE_BLUEPRINT_NAME}
        };
    }

    @DataProvider(name = "providerAndBlueprintAndClusterNameForAttachedCluster")
    public Object[][] providerClusterNameAndBlueprintForAttachedCluster() {
        CloudProvider cloudProvider = CloudProviderHelper.providerFactory("aws", getTestParameter());
        return new Object[][]{
                {cloudProvider, ATTACHED_CLUSTER_NAME, DATALAKE_CLUSTER_NAME, ATTACHED_CLUSTER_BLUEPRINT_NAME}
        };
    }

    private String getImageId(String provider) throws Exception {
        given(CloudbreakClient.isCreated());
        CloudbreakClient clientContext = CloudbreakClient.getTestContextCloudbreakClient().apply(getItContext());
        com.sequenceiq.cloudbreak.client.CloudbreakClient client = clientContext.getCloudbreakClient();
        ImagesResponse imagesByProvider = client.imageCatalogEndpoint().getImagesByProvider(provider);
        return getLastUuid(imagesByProvider.getHdpImages());
    }

    private String getLastUuid(List<? extends ImageResponse> images) {
        List<? extends ImageResponse> result = images.stream().filter(ImageResponse::isDefaultImage).collect(Collectors.toList());
        if (result.isEmpty()) {
            result = images;
        }
        result = result.stream().sorted(Comparator.comparing(ImageResponse::getDate)).collect(Collectors.toList());
        return result.get(result.size() - 1).getUuid();
    }
}
