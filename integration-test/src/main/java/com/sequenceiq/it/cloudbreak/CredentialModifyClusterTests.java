package com.sequenceiq.it.cloudbreak;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;

import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import com.sequenceiq.cloudbreak.api.model.BlueprintResponse;
import com.sequenceiq.cloudbreak.api.model.imagecatalog.ImageResponse;
import com.sequenceiq.cloudbreak.api.model.imagecatalog.ImagesResponse;
import com.sequenceiq.it.cloudbreak.newway.CloudbreakClient;
import com.sequenceiq.it.cloudbreak.newway.CloudbreakClusterTestConfiguration;
import com.sequenceiq.it.cloudbreak.newway.Cluster;
import com.sequenceiq.it.cloudbreak.newway.Credential;
import com.sequenceiq.it.cloudbreak.newway.Stack;
import com.sequenceiq.it.cloudbreak.newway.StackOperationEntity;
import com.sequenceiq.it.cloudbreak.newway.cloud.AwsCloudProvider;
import com.sequenceiq.it.cloudbreak.newway.cloud.CloudProvider;
import com.sequenceiq.it.cloudbreak.newway.cloud.CloudProviderHelper;

public class CredentialModifyClusterTests extends CloudbreakClusterTestConfiguration {

    private static final Logger LOGGER = LoggerFactory.getLogger(CredentialModifyClusterTests.class);

    @Test(dataProvider = "providernameblueprint", priority = 10)
    public void testCreateNewRegularCluster(CloudProvider cloudProvider, String clusterName, String credentialName, String blueprintName, String imageId)
            throws Exception {
        given(CloudbreakClient.created());
        given(cloudProvider.aValidCredential()
                .withName(credentialName), "a credential is created.");
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

    @Test(dataProvider = "providername", priority = 20)
    public void testModifyAWSCredential(CloudProvider cloudProvider, String clusterName, String credentialName) throws Exception {
        AwsCloudProvider provider = new AwsCloudProvider(getTestParameter());

        LOGGER.debug("Credential Name is ::: {}", credentialName);
        given(CloudbreakClient.created());
        given(provider.aValidCredential()
                .withName(credentialName), "a credential is created.");
        given(Credential.request()
                .withName(credentialName)
                .withParameters(provider.awsCredentialDetailsKey())
                .withCloudPlatform(provider.getPlatform()), " credential Key Based modification is requested.");
        when(Credential.put(), " credential has been modified.");
        then(Credential.assertThis(
                (credential, t) -> {
                    for (Map.Entry<String, Object> parameterMapping : credential.getResponse().getParameters().entrySet()) {
                        LOGGER.debug("Parameter is ::: {}", parameterMapping.getKey());
                        LOGGER.debug("Value is ::: {}", parameterMapping.getValue());
                        if ("selector".equalsIgnoreCase(parameterMapping.getKey())) {
                            Assert.assertEquals(parameterMapping.getValue(), "key-based",
                                    "Key Based should be present as Selector in response!");
                        }
                    }
                }), "Credential Parameter Mapping should be part of the response."
        );
    }

    @Test(dataProvider = "providernamehostgroupdesiredno", priority = 20)
    public void testScaleCluster(CloudProvider cloudProvider, String clusterName, String credentialName, String hostgroupName, int desiredCount)
            throws Exception {
        given(CloudbreakClient.created());
        given(cloudProvider.aValidCredential()
                .withName(credentialName), "a credential is created.");
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

    @Test(dataProvider = "providername", priority = 30)
    public void testStopCluster(CloudProvider cloudProvider, String clusterName, String credentialName) throws Exception {
        given(CloudbreakClient.created());
        given(cloudProvider.aValidCredential()
                .withName(credentialName), "a credential is created.");
        given(cloudProvider.aValidStackCreated()
                .withName(clusterName), "a stack is created");
        given(StackOperationEntity.request());
        when(StackOperationEntity.stop());
        when(Stack.get());
        then(Stack.waitAndCheckClusterAndStackStoppedStatus(), "stack has been stopped");
    }

    @Test(dataProvider = "providername", priority = 40)
    public void testStartCluster(CloudProvider cloudProvider, String clusterName, String credentialName) throws Exception {
        given(CloudbreakClient.created());
        given(cloudProvider.aValidCredential()
                .withName(credentialName), "a credential is created.");
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

    @Test(alwaysRun = true, dataProvider = "providername", priority = 50)
    public void testTerminateCluster(CloudProvider cloudProvider, String clusterName, String credentialName) throws Exception {
        given(CloudbreakClient.created());
        given(cloudProvider.aValidCredential()
                .withName(credentialName), "a credential is created.");
        given(cloudProvider.aValidStackCreated()
                .withName(clusterName), "a stack is created");
        when(Stack.delete());
        then(Stack.waitAndCheckClusterDeleted(), "stack has been deleted");
    }

    @DataProvider(name = "providernameblueprint")
    public Object[][] providerAndImage() throws Exception {
        String blueprint = getTestParameter().get("blueprintName");
        String provider = getTestParameter().get("provider").toLowerCase();

        CloudProvider cloudProvider = CloudProviderHelper.providerFactory(provider, getTestParameter());

        String clusterName = getTestParameter().get("clusterName");
        String credentialName = getTestParameter().get("credentialName");
        return new Object[][]{
                {cloudProvider, clusterName, credentialName, blueprint}
        };
    }

    @DataProvider(name = "providernamehostgroupdesiredno")
    public Object[][] providerAndHostgroup() {
        String hostgroupName = getTestParameter().get("instancegroupName");
        int desiredCount = Integer.parseInt(getTestParameter().get("desiredCount"));
        String provider = getTestParameter().get("provider").toLowerCase();

        CloudProvider cloudProvider = CloudProviderHelper.providerFactory(provider, getTestParameter());

        String clusterName = getTestParameter().get("clusterName");
        String credentialName = getTestParameter().get("credentialName");
        return new Object[][]{
                {cloudProvider, clusterName, credentialName, hostgroupName, desiredCount}
        };
    }

    @DataProvider(name = "providername")
    public Object[][] providerClusterName() {
        String provider = getTestParameter().get("provider").toLowerCase();

        CloudProvider cloudProvider = CloudProviderHelper.providerFactory(provider, getTestParameter());

        String clusterName = getTestParameter().get("clusterName");
        String credentialName = getTestParameter().get("credentialName");
        return new Object[][]{
                {cloudProvider, clusterName, credentialName}
        };
    }

    private String getImageId(String provider, String imageDescription, String blueprintName) throws Exception {
        given(CloudbreakClient.created());
        CloudbreakClient clientContext = CloudbreakClient.getTestContextCloudbreakClient().apply(getItContext());
        com.sequenceiq.cloudbreak.client.CloudbreakClient client = clientContext.getCloudbreakClient();
        ImagesResponse imagesByProvider = client.imageCatalogEndpoint().getImagesByProvider(provider);
        BlueprintResponse blueprint = client.blueprintEndpoint().getPublic(blueprintName);
        String stackVersion = new JSONObject(blueprint.getAmbariBlueprint())
                .getJSONObject("Blueprints").getString("stack_version");
        switch (imageDescription) {
            case "hdf":
                return getLastUuid(imagesByProvider.getHdfImages(), stackVersion);
            case "hdp":
                return getLastUuid(imagesByProvider.getHdpImages(), stackVersion);
            default:
                return getLastUuid(imagesByProvider.getBaseImages(), stackVersion);
        }
    }

    private String getLastUuid(List<? extends ImageResponse> images, String stackVersion) {
        List<? extends ImageResponse> result = images.stream()
                .filter(ImageResponse::isDefaultImage)
                .filter(image -> {
                    ImageResponse imageResponse = image;
                    if (!StringUtils.isEmpty(imageResponse.getVersion())) {
                        return imageResponse.getVersion().startsWith(stackVersion);
                    }
                    if (imageResponse.getStackDetails() == null) {
                        return true;
                    }
                    if (imageResponse.getStackDetails().getVersion() == null) {
                        return true;
                    }
                    return image.getStackDetails().getVersion().startsWith(stackVersion);
                })
                .collect(Collectors.toList());
        if (result.isEmpty()) {
            result = images;
        }
        result = result.stream().sorted(Comparator.comparing(ImageResponse::getDate)).collect(Collectors.toList());
        return result.get(result.size() - 1).getUuid();
    }

    @AfterClass(alwaysRun = true)
    public void cleanUp() throws Exception {
        String credentialName = getTestParameter().get("credentialName");

        LOGGER.info("Delete credential: [{}]", credentialName);
        try {
            given(CloudbreakClient.created());
            given(Credential.request().withName(credentialName));
            when(Credential.delete());
        } catch (WebApplicationException webappExp) {
            try (Response response = webappExp.getResponse()) {
                String exceptionMessage = response.readEntity(String.class);
                String errorMessage = exceptionMessage.substring(exceptionMessage.lastIndexOf(':') + 1);
                LOGGER.info("Cloudbreak Delete Credential Exception message ::: " + errorMessage);
            } finally {
                LOGGER.info("Cloudbreak Delete Credential have been done.");
            }
        }
    }

}
