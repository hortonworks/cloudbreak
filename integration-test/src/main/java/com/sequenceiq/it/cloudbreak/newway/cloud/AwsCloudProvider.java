package com.sequenceiq.it.cloudbreak.newway.cloud;

import static java.util.Set.of;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;

import com.google.common.base.Preconditions;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.mappable.CloudPlatform;
import com.sequenceiq.cloudbreak.api.endpoint.v4.credentials.parameters.aws.AwsCredentialV4Parameters;
import com.sequenceiq.cloudbreak.api.endpoint.v4.credentials.parameters.aws.KeyBasedCredentialParameters;
import com.sequenceiq.cloudbreak.api.endpoint.v4.credentials.parameters.aws.RoleBasedCredentialParameters;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.parameter.network.AwsNetworkV4Parameters;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.authentication.StackAuthenticationV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.cluster.ambari.AmbariV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.cluster.ambari.ambarirepository.AmbariRepositoryV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.cluster.ambari.stackrepository.StackRepositoryV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.instancegroup.template.InstanceTemplateV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.instancegroup.template.volume.VolumeV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.network.NetworkV4Request;
import com.sequenceiq.it.cloudbreak.newway.Cluster;
import com.sequenceiq.it.cloudbreak.newway.Credential;
import com.sequenceiq.it.cloudbreak.newway.entity.credential.CredentialTestDto;
import com.sequenceiq.it.cloudbreak.newway.StackAction;
import com.sequenceiq.it.cloudbreak.newway.StackCreation;
import com.sequenceiq.it.cloudbreak.newway.entity.stack.StackTestDto;
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
    public CredentialTestDto aValidCredential(boolean create) {
        String credentialType = getTestParameter().get("awsCredentialType");
        AwsCredentialV4Parameters parameters;
        if (KEY_BASED_CREDENTIAL.equals(credentialType)) {
            parameters = awsCredentialDetailsKey();
        } else {
            parameters = awsCredentialDetailsArn();
        }
        CredentialTestDto credential = create ? Credential.created() : Credential.request();
        return credential
                .withName(getCredentialName())
                .withDescription(CREDENTIAL_DEFAULT_DESCRIPTION)
                .withCloudPlatform(AWS_CAPITAL)
                .withAwsParameters(parameters);
    }

    public AwsCredentialV4Parameters awsCredentialDetailsArn() {
        AwsCredentialV4Parameters parameters = new AwsCredentialV4Parameters();
        RoleBasedCredentialParameters roleBasedCredentialParameters = new RoleBasedCredentialParameters();
        roleBasedCredentialParameters.setRoleArn(getTestParameter().get("integrationtest.awscredential.roleArn"));
        parameters.setRoleBased(roleBasedCredentialParameters);
        return parameters;
    }

    public AwsCredentialV4Parameters awsCredentialDetailsInvalidArn() {
        AwsCredentialV4Parameters parameters = new AwsCredentialV4Parameters();
        RoleBasedCredentialParameters roleBasedCredentialParameters = new RoleBasedCredentialParameters();
        roleBasedCredentialParameters.setRoleArn("arn:aws:iam::123456789012:role/fake");
        parameters.setRoleBased(roleBasedCredentialParameters);
        return parameters;
    }

    public AwsCredentialV4Parameters awsCredentialDetailsKey() {
        AwsCredentialV4Parameters parameters = new AwsCredentialV4Parameters();
        KeyBasedCredentialParameters keyBasedCredentialParameters = new KeyBasedCredentialParameters();
        keyBasedCredentialParameters.setAccessKey(getTestParameter().get("integrationtest.awscredential.accessKey"));
        keyBasedCredentialParameters.setSecretKey(getTestParameter().get("integrationtest.awscredential.secretKey"));
        parameters.setKeyBased(keyBasedCredentialParameters);
        return parameters;
    }

    public AwsCredentialV4Parameters awsCredentialDetailsInvalidAccessKey() {
        AwsCredentialV4Parameters parameters = new AwsCredentialV4Parameters();
        KeyBasedCredentialParameters keyBasedCredentialParameters = new KeyBasedCredentialParameters();
        keyBasedCredentialParameters.setAccessKey("ABCDEFGHIJKLMNOPQRST");
        keyBasedCredentialParameters.setSecretKey(getTestParameter().get("integrationtest.awscredential.secretKey"));
        parameters.setKeyBased(keyBasedCredentialParameters);
        return parameters;
    }

    public AwsCredentialV4Parameters awsCredentialDetailsInvalidSecretKey() {
        AwsCredentialV4Parameters parameters = new AwsCredentialV4Parameters();
        KeyBasedCredentialParameters keyBasedCredentialParameters = new KeyBasedCredentialParameters();
        keyBasedCredentialParameters.setSecretKey("123456789ABCDEFGHIJKLMNOP0123456789=ABC+");
        keyBasedCredentialParameters.setAccessKey(getTestParameter().get("integrationtest.awscredential.accessKey"));
        parameters.setKeyBased(keyBasedCredentialParameters);
        return parameters;
    }

    @Override
    public String availabilityZone() {
        return getTestParameter().getWithDefault("awsAvailabilityZone", "eu-west-1a");
    }

    @Override
    public String region() {
        return getTestParameter().getWithDefault("awsRegion", "eu-west-1");
    }

    @Override
    public StackAuthenticationV4Request stackauth() {
        StackAuthenticationV4Request stackauth = new StackAuthenticationV4Request();
        stackauth.setPublicKey(getTestParameter().get(CloudProviderHelper.INTEGRATIONTEST_PUBLIC_KEY_FILE).substring(BEGIN_INDEX));
        return stackauth;
    }

    @Override
    public InstanceTemplateV4Request template() {
        InstanceTemplateV4Request t = new InstanceTemplateV4Request();
        t.setInstanceType(getTestParameter().getWithDefault("awsInstanceType", "m5.xlarge"));
        VolumeV4Request volumeV4Request = new VolumeV4Request();
        volumeV4Request.setCount(Integer.parseInt(getTestParameter().getWithDefault("awsInstanceVolumeCount", "1")));
        volumeV4Request.setType(getTestParameter().getWithDefault("awsInstanceVolumeType", "gp2"));
        volumeV4Request.setSize(Integer.parseInt(getTestParameter().getWithDefault("awsInstanceVolumeSize", "100")));
        t.setAttachedVolumes(Set.of(volumeV4Request));
        t.setCloudPlatform(CloudPlatform.AWS);
        return t;
    }

    @Override
    public String getClusterName() {
        return getTestParameter().getWithDefault("awsClusterName", AWS_CLUSTER_DEFAULT_NAME);
    }

    public StackAuthenticationV4Request stackAuthentication() {
        StackAuthenticationV4Request stackAuthentication = new StackAuthenticationV4Request();
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
    public NetworkV4Request newNetwork() {
        NetworkV4Request network = new NetworkV4Request();
        network.setSubnetCIDR(getSubnetCIDR());
        return network;
    }

    @Override
    public NetworkV4Request existingNetwork() {
        NetworkV4Request network = new NetworkV4Request();
        network.setSubnetCIDR(getSubnetCIDR());
        AwsNetworkV4Parameters parameters = new AwsNetworkV4Parameters();
        parameters.setVpcId(getVpcId());
        parameters.setInternetGatewayId(getInternetGatewayId());
        network.setAws(parameters);
        return network;
    }

    @Override
    public NetworkV4Request existingSubnet() {
        NetworkV4Request network = new NetworkV4Request();
        AwsNetworkV4Parameters parameters = new AwsNetworkV4Parameters();
        parameters.setVpcId(getVpcId());
        parameters.setSubnetId(getSubnetId());
        network.setAws(parameters);
        return network;
    }

    @Override
    public AmbariV4Request getAmbariRequestWithNoConfigStrategyAndEmptyMpacks(String blueprintName) {
        var ambari = ambariRequestWithBlueprintName(blueprintName);
        var stackRepoDetails = new StackRepositoryV4Request();
        stackRepoDetails.setMpacks(Collections.emptyList());
        ambari.setConfigStrategy(null);
        ambari.setStackRepository(stackRepoDetails);
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
    public Cluster aValidAttachedCluster() {
        return Cluster.request()
                .withAmbariRequest(ambariRequestWithBlueprintName(getBlueprintName()))
                .withCloudStorage(resourceHelper.getCloudStorageRequestForAttachedCluster())
                .withRdsConfigNames(new HashSet<>(Arrays.asList(
                        getTestParameter().get(Ranger.CONFIG_NAME),
                        getTestParameter().get(Hive.CONFIG_NAME))))
                .withLdapConfigName(resourceHelper.getLdapConfigName());
    }

    @Override
    public StackTestDto aValidAttachedStackRequest(String datalakeName) {
        var request = new StackCreation(aValidStackRequest());
        request.setCreationStrategy(StackAction::determineNetworkAwsFromDatalakeStack);
        request.withSharedService(datalakeName);
        return request.getStack();
    }

    @Override
    public AmbariV4Request ambariRequestWithBlueprintNameAndCustomAmbari(String bluePrintName, String customAmbariVersion,
            String customAmbariRepoUrl, String customAmbariRepoGpgKey) {
        var req = ambariRequestWithBlueprintName(bluePrintName);
        if (StringUtils.isNoneEmpty(customAmbariVersion)) {
            Preconditions.checkNotNull(customAmbariRepoUrl);
            Preconditions.checkNotNull(customAmbariRepoGpgKey);
            AmbariRepositoryV4Request ambariRepo = new AmbariRepositoryV4Request();
            ambariRepo.setVersion(customAmbariVersion);
            ambariRepo.setBaseUrl(customAmbariRepoUrl);
            ambariRepo.setGpgKeyUrl(customAmbariRepoGpgKey);
            req.setRepository(ambariRepo);
        }
        return req;
    }
}