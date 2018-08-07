package com.sequenceiq.it.cloudbreak;

import static com.sequenceiq.it.cloudbreak.newway.cloud.AwsCloudProvider.AWS;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.SkipException;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import com.sequenceiq.cloudbreak.api.model.imagecatalog.ImageResponse;
import com.sequenceiq.cloudbreak.api.model.v2.template.AwsEncryption;
import com.sequenceiq.cloudbreak.api.model.v2.template.AwsParameters;
import com.sequenceiq.it.cloudbreak.newway.CloudbreakClient;
import com.sequenceiq.it.cloudbreak.newway.CloudbreakTest;
import com.sequenceiq.it.cloudbreak.newway.Cluster;
import com.sequenceiq.it.cloudbreak.newway.ImageSettings;
import com.sequenceiq.it.cloudbreak.newway.Stack;
import com.sequenceiq.it.cloudbreak.newway.StackEntity;
import com.sequenceiq.it.cloudbreak.newway.StackOperation;
import com.sequenceiq.it.cloudbreak.newway.TestParameter;
import com.sequenceiq.it.cloudbreak.newway.cloud.AwsCloudProvider;
import com.sequenceiq.it.cloudbreak.newway.cloud.CloudProviderHelper;

public class EncryptedClusterTests extends CloudbreakTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(EncryptedClusterTests.class);

    private AwsCloudProvider cloudProvider;

    private String clusterName;

    @BeforeTest
    public void initializeCloudProviderAndClusterNameValues() {
        cloudProvider = (AwsCloudProvider) CloudProviderHelper.providerFactory(AWS, getTestParameter());
        clusterName = getTestParameter().get("clusterName");
    }

    @Test(dataProvider = "providernameblueprintimage", priority = 10)
    public void testCreateNewEncryptedCluster(String blueprintName, String imageId) throws Exception {
        given(CloudbreakClient.isCreated());
        given(cloudProvider.aValidCredential());
        given(Cluster.request()
                        .withAmbariRequest(cloudProvider.ambariRequestWithBlueprintName(blueprintName)),
                "a cluster request");
        given(ImageSettings.request()
                .withImageCatalog("")
                .withImageId(imageId));
        given(aValidStackRequestWithDifferentEncryptedTypes().withName(clusterName), "a stack request");
        when(Stack.post(), "post the stack request");
        then(Stack.waitAndCheckClusterAndStackAvailabilityStatus(),
                "wait and check availability");
        then(Stack.checkClusterHasAmbariRunning(
                getTestParameter().get(CloudProviderHelper.DEFAULT_AMBARI_PORT),
                getTestParameter().get(CloudProviderHelper.DEFAULT_AMBARI_USER),
                getTestParameter().get(CloudProviderHelper.DEFAULT_AMBARI_PASSWORD)),
                "check ambari is running and components available");
    }

    @Test(priority = 30)
    public void testStopCluster() throws Exception {
        given(CloudbreakClient.isCreated());
        given(cloudProvider.aValidCredential());
        given(cloudProvider.aValidStackIsCreated()
                .withName(clusterName), "a stack is created");
        given(StackOperation.request());
        when(StackOperation.stop());
        when(Stack.get());
        then(Stack.waitAndCheckClusterAndStackStoppedStatus(), "stack has been stopped");
    }

    @Test(priority = 40)
    public void testStartCluster() throws Exception {
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

    @Test(alwaysRun = true, priority = 50)
    public void testTerminateCluster() throws Exception {
        given(CloudbreakClient.isCreated());
        given(cloudProvider.aValidCredential());
        given(cloudProvider.aValidStackIsCreated()
                .withName(clusterName), "a stack is created");
        when(Stack.delete());
        then(Stack.waitAndCheckClusterDeleted(), "stack has been deleted");
    }

    @DataProvider(name = "providernameblueprintimage")
    public Object[][] providerAndImage() throws Exception {
        var blueprint = getTestParameter().get("blueprintName");
        var imageDescription = getTestParameter().get("image");
        var image = getImageId(imageDescription);
        return new Object[][]{
                {blueprint, image}
        };
    }

    private String getImageId(String imageDescription) throws Exception {
        given(CloudbreakClient.isCreated());
        var clientContext = CloudbreakClient.getTestContextCloudbreakClient().apply(getItContext());
        var client = clientContext.getCloudbreakClient();
        var imagesByProvider = client.imageCatalogEndpoint().getImagesByProvider(AWS);
        switch (imageDescription) {
            case "hdf":
                return getLastUuid(imagesByProvider.getHdfImages());
            case "hdp":
                return getLastUuid(imagesByProvider.getHdpImages());
            default:
                return getLastUuid(imagesByProvider.getBaseImages());
        }
    }

    private StackEntity aValidStackRequestWithDifferentEncryptedTypes() {
        var stack = cloudProvider.aValidStackRequest();
        if (stack.getRequest() != null && stack.getRequest().getInstanceGroups() != null && stack.getRequest().getInstanceGroups().size() == 3) {
            stack.getRequest().getInstanceGroups().get(0).getTemplate().setAwsParameters(getAwsParametersWithEncryption(EncryptionType.DEFAULT));
            stack.getRequest().getInstanceGroups().get(1).getTemplate().setAwsParameters(getAwsParametersWithEncryption(EncryptionType.CUSTOM));
            stack.getRequest().getInstanceGroups().get(2).getTemplate().setAwsParameters(getAwsParametersWithEncryption(EncryptionType.NONE));
        } else {
            throw new SkipException("Unable to set encrypted aws templates for instance groups!");
        }
        return stack;
    }

    private String getLastUuid(List<? extends ImageResponse> images) {
        var result = images.stream().filter(ImageResponse::isDefaultImage).collect(Collectors.toList());
        if (result.isEmpty()) {
            result = images;
        }
        result = result.stream().sorted(Comparator.comparing(ImageResponse::getDate)).collect(Collectors.toList());
        return result.get(result.size() - 1).getUuid();
    }

    private AwsParameters getAwsParametersWithEncryption(EncryptionType type) {
        var params = new AwsParameters();
        params.setEncryption(type.getEncryption(getTestParameter()));
        return params;
    }

    private enum EncryptionType {
        DEFAULT {
            public AwsEncryption getEncryption(TestParameter testParameter) {
                var encryption = new AwsEncryption();
                encryption.setType("DEFAULT");
                return encryption;
            }
        },
        CUSTOM {
            public AwsEncryption getEncryption(TestParameter testParameter) {
                var encryption = new AwsEncryption();
                encryption.setType("CUSTOM");
                encryption.setKey(testParameter.getRequired("INTEGRATIONTEST_AWS_DISKENCRYPTIONKEY"));
                return encryption;
            }
        },
        NONE {
            public AwsEncryption getEncryption(TestParameter testParameter) {
                return null;
            }
        };

        public abstract AwsEncryption getEncryption(TestParameter testParameter);
    }
}
