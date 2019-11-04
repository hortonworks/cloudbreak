package com.sequenceiq.it.cloudbreak.newway.cloud;

import static java.util.Set.of;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;

import com.google.common.base.Preconditions;
import com.sequenceiq.cloudbreak.api.model.AmbariRepoDetailsJson;
import com.sequenceiq.cloudbreak.api.model.AmbariStackDetailsJson;
import com.sequenceiq.cloudbreak.api.model.stack.StackAuthenticationRequest;
import com.sequenceiq.cloudbreak.api.model.v2.AmbariV2Request;
import com.sequenceiq.cloudbreak.api.model.v2.NetworkV2Request;
import com.sequenceiq.cloudbreak.api.model.v2.TemplateV2Request;
import com.sequenceiq.it.cloudbreak.newway.Cluster;
import com.sequenceiq.it.cloudbreak.newway.Credential;
import com.sequenceiq.it.cloudbreak.newway.CredentialEntity;
import com.sequenceiq.it.cloudbreak.newway.StackAction;
import com.sequenceiq.it.cloudbreak.newway.StackCreation;
import com.sequenceiq.it.cloudbreak.newway.StackEntity;
import com.sequenceiq.it.cloudbreak.newway.TestParameter;
import com.sequenceiq.it.cloudbreak.parameters.RequiredInputParameters.Aws.Database.Hive;
import com.sequenceiq.it.cloudbreak.parameters.RequiredInputParameters.Aws.Database.Ranger;

public class AwsCloudProvider extends CloudProviderHelper {

    public static final String AWS = "aws";

    public static final String AWS_CAPITAL = "AWS";

    public static final String AWS_CLUSTER_DEFAULT_NAME = "autotesting-aws-cluster";

    public static final String KEY_BASED_CREDENTIAL = "key";

    private static final String CREDENTIAL_DEFAULT_NAME = "autotesting-aws-cred";

    private static final String CREDENTIAL_DEFAULT_DESCRIPTION = "autotesting aws credential";

    private static final String BLUEPRINT_DEFAULT_NAME = "Data Science: Apache Spark 2, Apache Zeppelin";

    private static final String NETWORK_DEFAULT_NAME = "autotesting-aws-net";

    private static final String VPC_DEFAULT_ID = "vpc-e623b28d";

    private static final String INTERNET_GATEWAY_ID = "igw-b55b26dd";

    private static final String SUBNET_DEFAULT_ID = "subnet-83901cfe";

    private static final String DEFAULT_SUBNET_CIDR = "10.0.0.0/16";

    private static final String NETWORK_DEFAULT_DESCRIPTION = "autotesting aws network";

    private final ResourceHelper<?> resourceHelper;

    public AwsCloudProvider(TestParameter testParameter) {
        super(testParameter);
        resourceHelper = new AwsResourceHelper(testParameter, "-aws");
    }

    @Override
    public CredentialEntity aValidCredential(boolean create) {
        String credentialType = getTestParameter().get("awsCredentialType");
        Map<String, Object> credentialParameters;
        credentialParameters = KEY_BASED_CREDENTIAL.equals(credentialType) ? awsCredentialDetailsKey() : awsCredentialDetailsArn();
        CredentialEntity credential = create ? Credential.created() : Credential.request();
        return credential
                .withName(getCredentialName())
                .withDescription(CREDENTIAL_DEFAULT_DESCRIPTION)
                .withCloudPlatform(AWS_CAPITAL)
                .withParameters(credentialParameters);
    }

    public Map<String, Object> awsCredentialDetailsArn() {
        return Map.of("selector", "role-based", "roleArn", getTestParameter().get("integrationtest.awscredential.roleArn"));
    }

    public Map<String, Object> awsCredentialDetailsInvalidArn() {
        return Map.of("selector", "role-based", "roleArn", "arn:aws:iam::123456789012:role/fake");
    }

    public Map<String, Object> awsCredentialDetailsKey() {
        return Map.of("selector", "key-based", "accessKey", getTestParameter().get("integrationtest.awscredential.accessKey"), "secretKey",
                getTestParameter().get("integrationtest.awscredential.secretKey"));
    }

    public Map<String, Object> awsCredentialDetailsInvalidAccessKey() {
        return Map.of("selector", "key-based", "accessKey", "ABCDEFGHIJKLMNOPQRST", "secretKey",
                getTestParameter().get("integrationtest.awscredential.secretKey"));
    }

    public Map<String, Object> awsCredentialDetailsInvalidSecretKey() {
        return Map.of("selector", "key-based", "accessKey", getTestParameter().get("integrationtest.awscredential.accessKey"), "secretKey",
                "123456789ABCDEFGHIJKLMNOP0123456789=ABC+");
    }

    @Override
    public String availabilityZone() {
        return getTestParameter().getWithDefault("awsAvailabilityZone", "eu-central-1a");
    }

    @Override
    public String region() {
        return getTestParameter().getWithDefault("awsRegion", "eu-central-1");
    }

    @Override
    public StackAuthenticationRequest stackauth() {
        StackAuthenticationRequest stackauth = new StackAuthenticationRequest();

        stackauth.setPublicKey(getTestParameter().get(CloudProviderHelper.INTEGRATIONTEST_PUBLIC_KEY_FILE).substring(BEGIN_INDEX));
        return stackauth;
    }

    @Override
    public TemplateV2Request template() {
        TemplateV2Request t = new TemplateV2Request();
        t.setInstanceType(getTestParameter().getWithDefault("awsInstanceType", "m5.xlarge"));
        t.setVolumeCount(Integer.parseInt(getTestParameter().getWithDefault("awsInstanceVolumeCount", "1")));
        t.setVolumeSize(Integer.parseInt(getTestParameter().getWithDefault("awsInstanceVolumeSize", "100")));
        t.setVolumeType(getTestParameter().getWithDefault("awsInstanceVolumeType", "gp2"));
        return t;
    }

    @Override
    public String getClusterName() {
        return getTestParameter().getWithDefault("awsClusterName", AWS_CLUSTER_DEFAULT_NAME);
    }

    public StackAuthenticationRequest stackAuthentication() {
        StackAuthenticationRequest stackAuthentication = new StackAuthenticationRequest();
        stackAuthentication.setPublicKeyId("aszegedi");
        return stackAuthentication;
    }

    @Override
    public String getPlatform() {
        return AWS_CAPITAL;
    }

    @Override
    public String getCredentialName() {
        return getTestParameter().getWithDefault("awsCredentialName", CREDENTIAL_DEFAULT_NAME);
    }

    @Override
    public String getBlueprintName() {
        return getTestParameter().getWithDefault("awsBlueprintName", BLUEPRINT_DEFAULT_NAME);
    }

    @Override
    public String getNetworkName() {
        return getTestParameter().getWithDefault("awsNetworkName", NETWORK_DEFAULT_NAME);
    }

    @Override
    public String getSubnetCIDR() {
        return getTestParameter().getWithDefault("awsSubnetCIDR", DEFAULT_SUBNET_CIDR);
    }

    @Override
    public String getVpcId() {
        return getTestParameter().getWithDefault("awsVpcId", VPC_DEFAULT_ID);
    }

    @Override
    public String getSubnetId() {
        return getTestParameter().getWithDefault("awsSubnetId", SUBNET_DEFAULT_ID);
    }

    public String getInternetGatewayId() {
        return getTestParameter().getWithDefault("awsInternetGatewayId", INTERNET_GATEWAY_ID);
    }

    @Override
    public Map<String, Object> newNetworkProperties() {
        return null;
    }

    @Override
    public Map<String, Object> networkProperties() {
        return Map.of("internetGatewayId", getInternetGatewayId(), "vpcId", getVpcId());
    }

    @Override
    public Map<String, Object> subnetProperties() {
        return Map.of("subnetId", getSubnetId(), "vpcId", getVpcId());
    }

    @Override
    public NetworkV2Request newNetwork() {
        NetworkV2Request network = new NetworkV2Request();
        network.setSubnetCIDR(getSubnetCIDR());
        return network;
    }

    @Override
    public NetworkV2Request existingNetwork() {
        NetworkV2Request network = new NetworkV2Request();
        network.setSubnetCIDR(getSubnetCIDR());
        network.setParameters(networkProperties());
        return network;
    }

    @Override
    public NetworkV2Request existingSubnet() {
        NetworkV2Request network = new NetworkV2Request();
        network.setParameters(subnetProperties());
        return network;
    }

    @Override
    public AmbariV2Request getAmbariRequestWithNoConfigStrategyAndEmptyMpacks(String blueprintName) {
        var ambari = ambariRequestWithBlueprintName(blueprintName);
        var stackDetails = new AmbariStackDetailsJson();
        stackDetails.setMpacks(Collections.emptyList());
        ambari.setConfigStrategy(null);
        ambari.setAmbariStackDetails(stackDetails);
        return ambari;
    }

    @Override
    public ResourceHelper getResourceHelper() {
        return resourceHelper;
    }

    @Override
    public Cluster aValidDatalakeCluster() {
        return Cluster.request()
                .withAmbariRequest(ambariRequestWithBlueprintName(getDatalakeBlueprintName()))
                .withCloudStorage(resourceHelper.getCloudStorageRequestForDatalake())
                .withRdsConfigNames(of(getTestParameter().get(Ranger.CONFIG_NAME),
                        getTestParameter().get(Hive.CONFIG_NAME)))
                .withLdapConfigName(resourceHelper.getLdapConfigName());
    }

    @Override
    public Cluster aValidAttachedCluster(String datalakeClusterName) {
        return Cluster.request()
                .withSharedService(datalakeClusterName)
                .withAmbariRequest(ambariRequestWithBlueprintName(getBlueprintName()))
                .withCloudStorage(resourceHelper.getCloudStorageRequestForAttachedCluster())
                .withRdsConfigNames(new HashSet<>(Arrays.asList(
                        getTestParameter().get(Ranger.CONFIG_NAME),
                        getTestParameter().get(Hive.CONFIG_NAME))))
                .withLdapConfigName(resourceHelper.getLdapConfigName());
    }

    @Override
    public StackEntity aValidAttachedStackRequest() {
        var request = new StackCreation(aValidStackRequest());
        request.setCreationStrategy(StackAction::determineNetworkAwsFromDatalakeStack);
        return request.getStack();
    }

    @Override
    public AmbariV2Request ambariRequestWithBlueprintNameAndCustomAmbari(String bluePrintName, String customAmbariVersion,
            String customAmbariRepoUrl, String customAmbariRepoGpgKey) {
        var req = ambariRequestWithBlueprintName(bluePrintName);
        if (StringUtils.isNoneEmpty(customAmbariVersion)) {
            Preconditions.checkNotNull(customAmbariRepoUrl);
            Preconditions.checkNotNull(customAmbariRepoGpgKey);
            AmbariRepoDetailsJson ambariRepoDetailsJson = new AmbariRepoDetailsJson();
            ambariRepoDetailsJson.setVersion(customAmbariVersion);
            ambariRepoDetailsJson.setBaseUrl(customAmbariRepoUrl);
            ambariRepoDetailsJson.setGpgKeyUrl(customAmbariRepoGpgKey);
            req.setAmbariRepoDetailsJson(ambariRepoDetailsJson);
        }
        return req;
    }
}