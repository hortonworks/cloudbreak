package com.sequenceiq.it.cloudbreak;

import static com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.KeyEncryptionMethod.KMS;
import static com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.KeyEncryptionMethod.RAW;
import static com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.KeyEncryptionMethod.RSA;
import static com.sequenceiq.it.cloudbreak.newway.cloud.AwsCloudProvider.AWS;
import static com.sequenceiq.it.cloudbreak.newway.cloud.GcpCloudProvider.GCP;

import org.testng.SkipException;
import org.testng.annotations.Test;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.KeyEncryptionMethod;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.parameter.template.AwsEncryptionV4Parameters;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.parameter.template.AwsInstanceTemplateV4Parameters;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.parameter.template.GcpEncryptionV4Parameters;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.parameter.template.GcpInstanceTemplateV4Parameters;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.instancegroup.template.InstanceTemplateV4Request;
import com.sequenceiq.it.cloudbreak.newway.CloudbreakClient;
import com.sequenceiq.it.cloudbreak.newway.Cluster;
import com.sequenceiq.it.cloudbreak.newway.ImageSettingsEntity;
import com.sequenceiq.it.cloudbreak.newway.Stack;
import com.sequenceiq.it.cloudbreak.newway.entity.stack.StackTestDto;
import com.sequenceiq.it.cloudbreak.newway.TestParameter;
import com.sequenceiq.it.cloudbreak.newway.cloud.CloudProvider;
import com.sequenceiq.it.cloudbreak.newway.cloud.CloudProviderHelper;

public class EncryptedClusterTests extends ClusterTests {

    @Test(dataProvider = "providernameblueprintimage", priority = 10)
    public void testCreateNewEncryptedCluster(CloudProvider cloudProvider, String clusterName, String blueprintName, String imageId) throws Exception {
        given(CloudbreakClient.created());
        given(cloudProvider.aValidCredential());
        given(Cluster.request()
                        .withAmbariRequest(cloudProvider.ambariRequestWithBlueprintName(blueprintName)),
                "a cluster request");
        given(ImageSettingsEntity.request()
                .withImageCatalog("")
                .withImageId(imageId));
        given(aValidStackRequestWithDifferentEncryptedTypes(cloudProvider).withName(clusterName), "a stack request");
        when(Stack.postV3(), "post the stack request");
        then(Stack.waitAndCheckClusterAndStackAvailabilityStatus(),
                "wait and check availability");
        then(Stack.checkClusterHasAmbariRunning(
                getTestParameter().get(CloudProviderHelper.DEFAULT_AMBARI_PORT),
                getTestParameter().get(CloudProviderHelper.DEFAULT_AMBARI_USER),
                getTestParameter().get(CloudProviderHelper.DEFAULT_AMBARI_PASSWORD)),
                "check ambari is running and components available");
    }

    private StackTestDto aValidStackRequestWithDifferentEncryptedTypes(CloudProvider cloudProvider) {
        var stack = cloudProvider.aValidStackRequest();
        if (stack.getRequest() != null && stack.getRequest().getInstanceGroups() != null && stack.getRequest().getInstanceGroups().size() == 3) {
            if (AWS.equalsIgnoreCase(cloudProvider.getPlatform())) {
                getInstanceGroup(stack, 0).setAws(getAwsParametersWithEncryption(getInstanceGroup(stack, 0).getAws(), EncryptionType.DEFAULT));
                getInstanceGroup(stack, 1).setAws(getAwsParametersWithEncryption(getInstanceGroup(stack, 0).getAws(), EncryptionType.CUSTOM));
                getInstanceGroup(stack, 2).setAws(getAwsParametersWithEncryption(getInstanceGroup(stack, 0).getAws(), EncryptionType.NONE));
            } else if (GCP.equalsIgnoreCase(cloudProvider.getPlatform())) {
                getInstanceGroup(stack, 0).setGcp(getGcpParametersWithEncryption(getInstanceGroup(stack, 0).getGcp(), RAW));
                getInstanceGroup(stack, 1).setGcp(getGcpParametersWithEncryption(getInstanceGroup(stack, 1).getGcp(), RSA));
                getInstanceGroup(stack, 2).setGcp(getGcpParametersWithEncryption(getInstanceGroup(stack, 2).getGcp(), KMS));
            }
        } else {
            throw new SkipException("Unable to set encrypted aws templates for instance groups!");
        }
        return stack;
    }

    private InstanceTemplateV4Request getInstanceGroup(StackTestDto stack, int id) {
        return stack.getRequest().getInstanceGroups().get(id).getTemplate();
    }

    private GcpInstanceTemplateV4Parameters getGcpParametersWithEncryption(GcpInstanceTemplateV4Parameters parameters, KeyEncryptionMethod encryptionMethod) {
        var templateParams = parameters;
        var params = new GcpEncryptionV4Parameters();
        params.setKeyEncryptionMethod(encryptionMethod);
        if (parameters == null) {
            templateParams = new GcpInstanceTemplateV4Parameters();
        }
        templateParams.setEncryption(params);
        return templateParams;
    }

    private AwsInstanceTemplateV4Parameters getAwsParametersWithEncryption(AwsInstanceTemplateV4Parameters parameters, EncryptionType type) {
        var templateParams = parameters;
        if (parameters == null) {
            templateParams = new AwsInstanceTemplateV4Parameters();
        }
        templateParams.setEncryption(type.getEncryption(getTestParameter()));
        return templateParams;
    }

    private enum EncryptionType {
        DEFAULT {
            public AwsEncryptionV4Parameters getEncryption(TestParameter testParameter) {
                var encryption = new AwsEncryptionV4Parameters();
                encryption.setType(com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.EncryptionType.DEFAULT);
                return encryption;
            }
        },
        CUSTOM {
            public AwsEncryptionV4Parameters getEncryption(TestParameter testParameter) {
                var encryption = new AwsEncryptionV4Parameters();
                encryption.setType(com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.EncryptionType.CUSTOM);
                encryption.setKey(testParameter.getRequired("INTEGRATIONTEST_AWS_DISKENCRYPTIONKEY"));
                return encryption;
            }
        },
        NONE {
            public AwsEncryptionV4Parameters getEncryption(TestParameter testParameter) {
                return null;
            }
        };

        public abstract AwsEncryptionV4Parameters getEncryption(TestParameter testParameter);
    }
}
