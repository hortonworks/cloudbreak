package com.sequenceiq.cloudbreak.cloud.aws;

import static com.amazonaws.services.cloudformation.model.Capability.CAPABILITY_IAM;
import static java.util.Arrays.asList;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.amazonaws.services.cloudformation.model.CreateStackRequest;
import com.amazonaws.services.cloudformation.model.DeleteStackRequest;
import com.amazonaws.services.cloudformation.model.OnFailure;
import com.amazonaws.services.cloudformation.model.Parameter;
import com.amazonaws.services.cloudformation.model.UpdateStackRequest;
import com.amazonaws.services.ec2.AmazonEC2Client;
import com.amazonaws.services.ec2.model.DescribeImagesRequest;
import com.amazonaws.services.ec2.model.DescribeImagesResult;
import com.amazonaws.services.ec2.model.Image;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Strings;
import com.sequenceiq.cloudbreak.cloud.aws.view.AuthenticatedContextView;
import com.sequenceiq.cloudbreak.cloud.aws.view.AwsInstanceProfileView;
import com.sequenceiq.cloudbreak.cloud.aws.view.AwsNetworkView;
import com.sequenceiq.cloudbreak.cloud.aws.view.AwsRdsDbSubnetGroupView;
import com.sequenceiq.cloudbreak.cloud.aws.view.AwsRdsInstanceView;
import com.sequenceiq.cloudbreak.cloud.aws.view.AwsRdsVpcSecurityGroupView;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.exception.CloudConnectorException;
import com.sequenceiq.cloudbreak.cloud.model.CloudStack;
import com.sequenceiq.cloudbreak.cloud.model.DatabaseStack;
import com.sequenceiq.common.api.type.InstanceGroupType;

@Component
public class AwsStackRequestHelper {

    // The number of chunks for large parameters
    @VisibleForTesting
    static final int CHUNK_COUNT = 4;

    // The size of each chunk for large parameters
    @VisibleForTesting
    static final int CHUNK_SIZE = 4096;

    @Inject
    private AwsTaggingService awsTaggingService;

    @Inject
    private AwsClient awsClient;

    public CreateStackRequest createCreateStackRequest(AuthenticatedContext ac, CloudStack stack, String cFStackName, String subnet, String cfTemplate) {
        return new CreateStackRequest()
                .withStackName(cFStackName)
                .withOnFailure(OnFailure.DO_NOTHING)
                .withTemplateBody(cfTemplate)
                .withTags(awsTaggingService.prepareCloudformationTags(ac, stack.getTags()))
                .withCapabilities(CAPABILITY_IAM)
                .withParameters(getStackParameters(ac, stack, cFStackName, subnet));
    }

    public CreateStackRequest createCreateStackRequest(AuthenticatedContext ac, DatabaseStack stack, String cFStackName, String cfTemplate) {
        return new CreateStackRequest()
                .withStackName(cFStackName)
                .withOnFailure(OnFailure.DO_NOTHING)
                .withTemplateBody(cfTemplate)
                .withTags(awsTaggingService.prepareCloudformationTags(ac, stack.getTags()))
                .withCapabilities(CAPABILITY_IAM)
                .withParameters(getStackParameters(ac, stack));
    }

    public UpdateStackRequest createUpdateStackRequest(AuthenticatedContext ac, CloudStack stack, String cFStackName, String cfTemplate) {
        return new UpdateStackRequest()
                .withStackName(cFStackName)
                .withParameters(getStackParameters(ac, stack, cFStackName, null))
                .withTemplateBody(cfTemplate);
    }

    public DeleteStackRequest createDeleteStackRequest(String cFStackName) {
        return new DeleteStackRequest()
                .withStackName(cFStackName);
    }

    private Collection<Parameter> getStackParameters(AuthenticatedContext ac, CloudStack stack, String stackName, String newSubnetCidr) {
        AwsNetworkView awsNetworkView = new AwsNetworkView(stack.getNetwork());
        AwsInstanceProfileView awsInstanceProfileView = new AwsInstanceProfileView(stack);
        String keyPairName = awsClient.getKeyPairName(ac);
        if (awsClient.existingKeyPairNameSpecified(stack.getInstanceAuthentication())) {
            keyPairName = awsClient.getExistingKeyPairName(stack.getInstanceAuthentication());
        }

        Collection<Parameter> parameters = new ArrayList<>();
        if (stack.getImage().getUserDataByType(InstanceGroupType.CORE) != null) {
            addParameterChunks(parameters, "CBUserData", stack.getImage().getUserDataByType(InstanceGroupType.CORE), CHUNK_COUNT);
        }
        addParameterChunks(parameters, "CBGateWayUserData", stack.getImage().getUserDataByType(InstanceGroupType.GATEWAY), CHUNK_COUNT);
        parameters.addAll(asList(
                new Parameter().withParameterKey("StackName").withParameterValue(stackName),
                new Parameter().withParameterKey("KeyName").withParameterValue(keyPairName),
                new Parameter().withParameterKey("AMI").withParameterValue(stack.getImage().getImageName()),
                new Parameter().withParameterKey("RootDeviceName").withParameterValue(getRootDeviceName(ac, stack))
        ));
        if (awsInstanceProfileView.isInstanceProfileAvailable()) {
            parameters.add(new Parameter().withParameterKey("InstanceProfile").withParameterValue(awsInstanceProfileView.getInstanceProfile()));
        }
        if (ac.getCloudContext().getLocation().getAvailabilityZone() != null
                && ac.getCloudContext().getLocation().getAvailabilityZone().value() != null) {
            parameters.add(new Parameter().withParameterKey("AvailabilitySet")
                    .withParameterValue(ac.getCloudContext().getLocation().getAvailabilityZone().value()));
        }
        if (awsNetworkView.isExistingVPC()) {
            parameters.add(new Parameter().withParameterKey("VPCId").withParameterValue(awsNetworkView.getExistingVpc()));
            if (awsNetworkView.isExistingIGW()) {
                parameters.add(new Parameter().withParameterKey("InternetGatewayId").withParameterValue(awsNetworkView.getExistingIgw()));
            }
            if (awsNetworkView.isExistingSubnet()) {
                parameters.add(new Parameter().withParameterKey("SubnetId").withParameterValue(awsNetworkView.getExistingSubnet()));
            } else {
                parameters.add(new Parameter().withParameterKey("SubnetCIDR").withParameterValue(newSubnetCidr));
            }
        }
        return parameters;
    }

    private Parameter getStackOwnerFromStack(AuthenticatedContext ac, CloudStack stack, String key, String referenceName) {
        return getStackOwner(ac, stack.getTags().get(key), referenceName);
    }

    private Parameter getStackOwnerFromDatabase(AuthenticatedContext ac, DatabaseStack stack, String key, String referenceName) {
        return getStackOwner(ac, stack.getTags().get(key), referenceName);
    }

    private Parameter getStackOwner(AuthenticatedContext ac, String tagValue, String referenceName) {
        if (Strings.isNullOrEmpty(tagValue)) {
            return new Parameter()
                .withParameterKey(referenceName)
                .withParameterValue(String.valueOf(ac.getCloudContext().getUserName()));
        } else {
            return new Parameter()
                .withParameterKey(referenceName)
                .withParameterValue(tagValue);
        }
    }

    @VisibleForTesting
    void addParameterChunks(Collection<Parameter> parameters, String baseParameterKey, String parameterValue, int chunkCount) {
        int chunk = 0;
        String parameterKey = baseParameterKey;
        if (parameterValue == null) {
            parameters.add(new Parameter().withParameterKey(parameterKey).withParameterValue(null));
        } else {
            int len = parameterValue.length();
            int offset = 0;
            int limit = CHUNK_SIZE;
            // Add full chunks
            while ((chunk < chunkCount) && (len > limit)) {
                parameters.add(new Parameter().withParameterKey(parameterKey).withParameterValue(parameterValue.substring(offset, limit)));
                chunk++;
                parameterKey = baseParameterKey + chunk;
                offset = limit;
                limit += CHUNK_SIZE;
            }
            // Add the final partial chunk
            parameters.add(new Parameter().withParameterKey(parameterKey).withParameterValue(parameterValue.substring(offset, len)));
        }
        // Pad with empty chunks if necessary
        while (++chunk < chunkCount) {
            parameterKey = baseParameterKey + chunk;
            parameters.add(new Parameter().withParameterKey(parameterKey).withParameterValue(""));
        }
    }

    private String getRootDeviceName(AuthenticatedContext ac, CloudStack cloudStack) {
        AmazonEC2Client ec2Client = new AuthenticatedContextView(ac).getAmazonEC2Client();
        DescribeImagesResult images = ec2Client.describeImages(new DescribeImagesRequest().withImageIds(cloudStack.getImage().getImageName()));
        if (images.getImages().isEmpty()) {
            throw new CloudConnectorException(String.format("AMI is not available: '%s'.", cloudStack.getImage().getImageName()));
        }
        Image image = images.getImages().get(0);
        if (image == null) {
            throw new CloudConnectorException(String.format("Couldn't describe AMI '%s'.", cloudStack.getImage().getImageName()));
        }
        return image.getRootDeviceName();
    }

    @VisibleForTesting
    Collection<Parameter> getStackParameters(AuthenticatedContext ac, DatabaseStack stack) {
        AwsNetworkView awsNetworkView = new AwsNetworkView(stack.getNetwork());
        AwsRdsInstanceView awsRdsInstanceView = new AwsRdsInstanceView(stack.getDatabaseServer());
        AwsRdsDbSubnetGroupView awsRdsDbSubnetGroupView = new AwsRdsDbSubnetGroupView(stack.getDatabaseServer());
        AwsRdsVpcSecurityGroupView awsRdsVpcSecurityGroupView = new AwsRdsVpcSecurityGroupView(stack.getDatabaseServer());
        List<Parameter> parameters = new ArrayList<>(asList(
                new Parameter().withParameterKey("DBInstanceClassParameter").withParameterValue(awsRdsInstanceView.getDBInstanceClass()),
                new Parameter().withParameterKey("DBInstanceIdentifierParameter").withParameterValue(awsRdsInstanceView.getDBInstanceIdentifier()),
                new Parameter().withParameterKey("DBSubnetGroupNameParameter").withParameterValue(awsRdsDbSubnetGroupView.getDBSubnetGroupName()),
                new Parameter().withParameterKey("DBSubnetGroupSubnetIdsParameter").withParameterValue(String.join(",", awsNetworkView.getSubnetList())),
                new Parameter().withParameterKey("EngineParameter").withParameterValue(awsRdsInstanceView.getEngine()),
                new Parameter().withParameterKey("MasterUsernameParameter").withParameterValue(awsRdsInstanceView.getMasterUsername()),
                new Parameter().withParameterKey("MasterUserPasswordParameter").withParameterValue(awsRdsInstanceView.getMasterUserPassword()))
        );

        addParameterIfNotNull(parameters, "AllocatedStorageParameter", awsRdsInstanceView.getAllocatedStorage());
        addParameterIfNotNull(parameters, "BackupRetentionPeriodParameter", awsRdsInstanceView.getBackupRetentionPeriod());
        addParameterIfNotNull(parameters, "EngineVersionParameter", awsRdsInstanceView.getEngineVersion());
        addParameterIfNotNull(parameters, "MultiAZParameter", awsRdsInstanceView.getMultiAZ());
        addParameterIfNotNull(parameters, "StorageTypeParameter", awsRdsInstanceView.getStorageType());
        addParameterIfNotNull(parameters, "PortParameter", stack.getDatabaseServer().getPort());

        if (awsRdsInstanceView.getVPCSecurityGroups().isEmpty()) {
            // VPC-id and VPC cidr should be filled in
            parameters.addAll(
                    asList(
                            new Parameter().withParameterKey("VPCIdParameter").withParameterValue(String.valueOf(awsNetworkView.getExistingVpc())),
                            new Parameter().withParameterKey("VPCCidrParameter").withParameterValue(String.valueOf(awsNetworkView.getExistingVpcCidr())),
                            new Parameter().withParameterKey("DBSecurityGroupNameParameter")
                                    .withParameterValue(awsRdsVpcSecurityGroupView.getDBSecurityGroupName())
                    )
            );
        } else {
            parameters.add(
                    new Parameter().withParameterKey("VPCSecurityGroupsParameter")
                            .withParameterValue(String.join(",", awsRdsInstanceView.getVPCSecurityGroups()))
            );
        }

        return parameters;
    }

    private void addParameterIfNotNull(List<Parameter> parameters, String key, Object value) {
        if (value != null) {
            parameters.add(new Parameter().withParameterKey(key).withParameterValue(Objects.toString(value)));
        }
    }
}
