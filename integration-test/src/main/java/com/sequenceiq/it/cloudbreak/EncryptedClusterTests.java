package com.sequenceiq.it.cloudbreak;

import static com.sequenceiq.it.cloudbreak.newway.cloud.AwsCloudProvider.AWS;
import static com.sequenceiq.it.cloudbreak.newway.cloud.GcpCloudProvider.GCP;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.SkipException;
import org.testng.annotations.Test;

import com.sequenceiq.cloudbreak.api.model.v2.template.AwsEncryption;
import com.sequenceiq.cloudbreak.api.model.v2.template.AwsParameters;
import com.sequenceiq.cloudbreak.api.model.v2.template.GcpEncryption;
import com.sequenceiq.cloudbreak.api.model.v2.template.GcpParameters;
import com.sequenceiq.it.cloudbreak.newway.CloudbreakClient;
import com.sequenceiq.it.cloudbreak.newway.Cluster;
import com.sequenceiq.it.cloudbreak.newway.Stack;
import com.sequenceiq.it.cloudbreak.newway.StackEntity;
import com.sequenceiq.it.cloudbreak.newway.TestParameter;
import com.sequenceiq.it.cloudbreak.newway.cloud.CloudProvider;
import com.sequenceiq.it.cloudbreak.newway.cloud.CloudProviderHelper;

public class EncryptedClusterTests extends ClusterTests {

    private static final Logger LOGGER = LoggerFactory.getLogger(EncryptedClusterTests.class);

    @Test(dataProvider = "providernameblueprint", priority = 10)
    public void testCreateNewEncryptedCluster(CloudProvider cloudProvider, String clusterName, String blueprintName) throws Exception {
        given(CloudbreakClient.created());
        given(cloudProvider.aValidCredential());
        given(Cluster.request()
                        .withAmbariRequest(cloudProvider.ambariRequestWithBlueprintName(blueprintName)),
                "a cluster request");
        given(aValidStackRequestWithDifferentEncryptedTypes(cloudProvider).withName(clusterName), "a stack request");
        when(Stack.post(), "post the stack request");
        then(Stack.waitAndCheckClusterAndStackAvailabilityStatus(),
                "wait and check availability");
        then(Stack.checkClusterHasAmbariRunning(
                getTestParameter().get(CloudProviderHelper.DEFAULT_AMBARI_PORT),
                getTestParameter().get(CloudProviderHelper.DEFAULT_AMBARI_USER),
                getTestParameter().get(CloudProviderHelper.DEFAULT_AMBARI_PASSWORD)),
                "check ambari is running and components available");
    }

    private StackEntity aValidStackRequestWithDifferentEncryptedTypes(CloudProvider cloudProvider) {
        var stack = cloudProvider.aValidStackRequest();
        if (stack.getRequest() != null && stack.getRequest().getInstanceGroups() != null && stack.getRequest().getInstanceGroups().size() == 3) {
            if (AWS.equalsIgnoreCase(cloudProvider.getPlatform())) {
                stack.getRequest().getInstanceGroups().get(0).getTemplate().setAwsParameters(getAwsParametersWithEncryption(EncryptionType.DEFAULT));
                stack.getRequest().getInstanceGroups().get(1).getTemplate().setAwsParameters(getAwsParametersWithEncryption(EncryptionType.CUSTOM));
                stack.getRequest().getInstanceGroups().get(2).getTemplate().setAwsParameters(getAwsParametersWithEncryption(EncryptionType.NONE));
            } else if (GCP.equalsIgnoreCase(cloudProvider.getPlatform())) {
                stack.getRequest().getInstanceGroups().get(0).getTemplate().setGcpParameters(getGcpParametersWithEncryption(EncryptionMethod.RAW));
                stack.getRequest().getInstanceGroups().get(1).getTemplate().setGcpParameters(getGcpParametersWithEncryption(EncryptionMethod.RSA));
                stack.getRequest().getInstanceGroups().get(2).getTemplate().setGcpParameters(getGcpParametersWithEncryption(EncryptionMethod.KMS));
            }
        } else {
            throw new SkipException("Unable to set encrypted aws templates for instance groups!");
        }
        return stack;
    }

    private GcpParameters getGcpParametersWithEncryption(EncryptionMethod encryptionMethod) {
        var params = new GcpParameters();
        params.setEncryption(encryptionMethod.getEncryption(getTestParameter()));
        return params;
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

    private enum EncryptionMethod {
        RAW {
            public GcpEncryption getEncryption(TestParameter testParameter) {
                var encryption = new GcpEncryption();
                encryption.setType(EncryptionType.CUSTOM.name());
                encryption.setKeyEncryptionMethod(RAW.name());
                encryption.setKey("Hello World");
                return encryption;
            }
        },
        RSA {
            public GcpEncryption getEncryption(TestParameter testParameter) {
                var encryption = new GcpEncryption();
                encryption.setType(EncryptionType.CUSTOM.name());
                encryption.setKeyEncryptionMethod(RSA.name());
                encryption.setKey("Hello World");
                return encryption;
            }
        },
        KMS {
            public GcpEncryption getEncryption(TestParameter testParameter) {
                var encryption = new GcpEncryption();
                encryption.setType(EncryptionType.CUSTOM.name());
                encryption.setKeyEncryptionMethod(KMS.name());
                encryption.setKey(testParameter.getRequired("INTEGRATIONTEST_GCP_DISKENCRYPTIONKEY"));
                return encryption;
            }
        };

        public abstract GcpEncryption getEncryption(TestParameter testParameter);
    }
}
