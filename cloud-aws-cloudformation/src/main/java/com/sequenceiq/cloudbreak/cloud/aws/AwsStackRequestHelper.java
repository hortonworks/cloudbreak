package com.sequenceiq.cloudbreak.cloud.aws;

import static java.util.Arrays.asList;
import static software.amazon.awssdk.services.cloudformation.model.Capability.CAPABILITY_IAM;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

import jakarta.inject.Inject;

import org.springframework.stereotype.Component;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Strings;
import com.sequenceiq.cloudbreak.cloud.aws.common.AwsTaggingService;
import com.sequenceiq.cloudbreak.cloud.aws.common.client.AmazonEc2Client;
import com.sequenceiq.cloudbreak.cloud.aws.common.view.AuthenticatedContextView;
import com.sequenceiq.cloudbreak.cloud.aws.common.view.AwsInstanceProfileView;
import com.sequenceiq.cloudbreak.cloud.aws.common.view.AwsNetworkView;
import com.sequenceiq.cloudbreak.cloud.aws.connector.resource.upgrade.operation.AwsRdsVersionOperations;
import com.sequenceiq.cloudbreak.cloud.aws.view.AwsRdsDbParameterGroupView;
import com.sequenceiq.cloudbreak.cloud.aws.view.AwsRdsDbSubnetGroupView;
import com.sequenceiq.cloudbreak.cloud.aws.view.AwsRdsInstanceView;
import com.sequenceiq.cloudbreak.cloud.aws.view.AwsRdsVpcSecurityGroupView;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.exception.CloudConnectorException;
import com.sequenceiq.cloudbreak.cloud.model.CloudStack;
import com.sequenceiq.cloudbreak.cloud.model.DatabaseServer;
import com.sequenceiq.cloudbreak.cloud.model.DatabaseStack;

import software.amazon.awssdk.services.cloudformation.model.CreateStackRequest;
import software.amazon.awssdk.services.cloudformation.model.DeleteStackRequest;
import software.amazon.awssdk.services.cloudformation.model.ListStackResourcesRequest;
import software.amazon.awssdk.services.cloudformation.model.OnFailure;
import software.amazon.awssdk.services.cloudformation.model.Parameter;
import software.amazon.awssdk.services.cloudformation.model.UpdateStackRequest;
import software.amazon.awssdk.services.cloudformation.model.ValidateTemplateRequest;
import software.amazon.awssdk.services.ec2.model.DescribeImagesRequest;
import software.amazon.awssdk.services.ec2.model.DescribeImagesResponse;
import software.amazon.awssdk.services.ec2.model.Image;

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
    private AwsCloudFormationClient awsClient;

    @Inject
    private AwsRdsVersionOperations awsRdsVersionOperations;

    public CreateStackRequest createCreateStackRequest(AuthenticatedContext ac, CloudStack stack, String cFStackName, String subnet, String cfTemplate) {
        return CreateStackRequest.builder()
                .stackName(cFStackName)
                .onFailure(OnFailure.DO_NOTHING)
                .templateBody(cfTemplate)
                .tags(awsTaggingService.prepareCloudformationTags(ac, stack.getTags()))
                .capabilities(CAPABILITY_IAM)
                .parameters(getStackParameters(ac, stack, cFStackName, subnet))
                .build();
    }

    public CreateStackRequest createCreateStackRequest(AuthenticatedContext ac, DatabaseStack stack, String cFStackName, String cfTemplate) {
        return CreateStackRequest.builder()
                .stackName(cFStackName)
                .onFailure(OnFailure.DO_NOTHING)
                .templateBody(cfTemplate)
                .tags(awsTaggingService.prepareCloudformationTags(ac, stack.getTags()))
                .capabilities(CAPABILITY_IAM)
                .parameters(getStackParameters(ac, stack, true))
                .build();
    }

    public ValidateTemplateRequest createValidateTemplateRequest(String cfTemplate) {
        return ValidateTemplateRequest.builder()
                .templateBody(cfTemplate)
                .build();
    }

    public UpdateStackRequest createUpdateStackRequest(AuthenticatedContext ac, CloudStack stack, String cFStackName, String cfTemplate) {
        return UpdateStackRequest.builder()
                .stackName(cFStackName)
                .parameters(getStackParameters(ac, stack, cFStackName, null))
                .templateBody(cfTemplate)
                .build();
    }

    public DeleteStackRequest createDeleteStackRequest(String cFStackName, String... retainResources) {
        return DeleteStackRequest.builder()
                .retainResources(retainResources)
                .stackName(cFStackName)
                .build();
    }

    public UpdateStackRequest createUpdateStackRequest(AuthenticatedContext ac, CloudStack stack, String cFStackName, String subnet, String cfTemplate) {
        return UpdateStackRequest.builder()
                .stackName(cFStackName)
                .parameters(getStackParameters(ac, stack, cFStackName, subnet))
                .templateBody(cfTemplate)
                .tags(awsTaggingService.prepareCloudformationTags(ac, stack.getTags()))
                .capabilities(CAPABILITY_IAM)
                .build();
    }

    public ListStackResourcesRequest createListStackResourcesRequest(String cFStackName) {
        return ListStackResourcesRequest.builder()
                .stackName(cFStackName)
                .build();
    }

    private Collection<Parameter> getStackParameters(AuthenticatedContext ac, CloudStack stack, String stackName, String newSubnetCidr) {
        AwsNetworkView awsNetworkView = new AwsNetworkView(stack.getNetwork());
        AwsInstanceProfileView awsInstanceProfileView = new AwsInstanceProfileView(stack);
        String keyPairName = awsClient.getKeyPairName(ac);
        if (awsClient.existingKeyPairNameSpecified(stack.getInstanceAuthentication())) {
            keyPairName = awsClient.getExistingKeyPairName(stack.getInstanceAuthentication());
        }

        Collection<Parameter> parameters = new ArrayList<>();
        if (stack.getCoreUserData() != null) {
            addParameterChunks(parameters, "CBUserData", stack.getCoreUserData(), CHUNK_COUNT);
        }
        addParameterChunks(parameters, "CBGateWayUserData", stack.getGatewayUserData(), CHUNK_COUNT);
        parameters.addAll(asList(
                Parameter.builder().parameterKey("StackName").parameterValue(stackName).build(),
                Parameter.builder().parameterKey("KeyName").parameterValue(keyPairName).build(),
                Parameter.builder().parameterKey("AMI").parameterValue(stack.getImage().getImageName()).build(),
                Parameter.builder().parameterKey("RootDeviceName").parameterValue(getRootDeviceName(ac, stack)).build()
        ));
        if (awsInstanceProfileView.isInstanceProfileAvailable()) {
            parameters.add(Parameter.builder().parameterKey("InstanceProfile").parameterValue(awsInstanceProfileView.getInstanceProfile()).build());
        }
        if (ac.getCloudContext().getLocation().getAvailabilityZone() != null
                && ac.getCloudContext().getLocation().getAvailabilityZone().value() != null) {
            parameters.add(Parameter.builder().parameterKey("AvailabilitySet")
                    .parameterValue(ac.getCloudContext().getLocation().getAvailabilityZone().value()).build());
        }
        if (awsNetworkView.isExistingVPC()) {
            parameters.add(Parameter.builder().parameterKey("VPCId").parameterValue(awsNetworkView.getExistingVpc()).build());
            if (awsNetworkView.isExistingIGW()) {
                parameters.add(Parameter.builder().parameterKey("InternetGatewayId").parameterValue(awsNetworkView.getExistingIgw()).build());
            }
            if (awsNetworkView.isExistingSubnet()) {
                parameters.add(Parameter.builder().parameterKey("SubnetId").parameterValue(awsNetworkView.getExistingSubnet()).build());
            } else {
                parameters.add(Parameter.builder().parameterKey("SubnetCIDR").parameterValue(newSubnetCidr).build());
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
            return Parameter.builder()
                    .parameterKey(referenceName)
                    .parameterValue(String.valueOf(ac.getCloudContext().getUserName()))
                    .build();
        } else {
            return Parameter.builder()
                    .parameterKey(referenceName)
                    .parameterValue(tagValue)
                    .build();
        }
    }

    @VisibleForTesting
    void addParameterChunks(Collection<Parameter> parameters, String baseParameterKey, String parameterValue, int chunkCount) {
        int chunk = 0;
        String parameterKey = baseParameterKey;
        if (parameterValue == null) {
            parameters.add(Parameter.builder().parameterKey(parameterKey).parameterValue(null).build());
        } else {
            int len = parameterValue.length();
            int offset = 0;
            int limit = CHUNK_SIZE + 1;
            // Add full chunks
            while (chunk < chunkCount && len > limit) {
                char c;
                char c2;
                do {
                    c = parameterValue.charAt(--limit);
                    c2 = parameterValue.charAt(limit - 1);
                } while ((Character.isWhitespace(c) || Character.isWhitespace(c2)) && offset <= limit - 2);

                String slice = parameterValue.substring(offset, limit);
                parameters.add(Parameter.builder().parameterKey(parameterKey).parameterValue(slice).build());
                chunk++;
                parameterKey = baseParameterKey + chunk;
                offset = limit;
                limit += CHUNK_SIZE + 1;
            }
            // Add the final partial chunk
            parameters.add(Parameter.builder().parameterKey(parameterKey).parameterValue(parameterValue.substring(offset, len)).build());
        }
        // Pad with empty chunks if necessary
        while (++chunk < chunkCount) {
            parameterKey = baseParameterKey + chunk;
            parameters.add(Parameter.builder().parameterKey(parameterKey).parameterValue("").build());
        }
    }

    private String getRootDeviceName(AuthenticatedContext ac, CloudStack cloudStack) {
        AmazonEc2Client ec2Client = new AuthenticatedContextView(ac).getAmazonEC2Client();
        DescribeImagesResponse images = ec2Client.describeImages(DescribeImagesRequest.builder().imageIds(cloudStack.getImage().getImageName()).build());
        if (images.images().isEmpty()) {
            throw new CloudConnectorException(String.format("AMI is not available: '%s'.", cloudStack.getImage().getImageName()));
        }
        Image image = images.images().get(0);
        if (image == null) {
            throw new CloudConnectorException(String.format("Couldn't describe AMI '%s'.", cloudStack.getImage().getImageName()));
        }
        return image.rootDeviceName();
    }

    @VisibleForTesting
    Collection<Parameter> getStackParameters(AuthenticatedContext ac, DatabaseStack stack, boolean deleteProtection) {
        AwsNetworkView awsNetworkView = new AwsNetworkView(stack.getNetwork());
        DatabaseServer databaseServer = stack.getDatabaseServer();
        AwsRdsInstanceView awsRdsInstanceView = new AwsRdsInstanceView(databaseServer);
        AwsRdsDbSubnetGroupView awsRdsDbSubnetGroupView = new AwsRdsDbSubnetGroupView(databaseServer);
        AwsRdsVpcSecurityGroupView awsRdsVpcSecurityGroupView = new AwsRdsVpcSecurityGroupView(databaseServer);
        AwsRdsDbParameterGroupView awsRdsDbParameterGroupView = new AwsRdsDbParameterGroupView(databaseServer, awsRdsVersionOperations);
        List<Parameter> parameters = new ArrayList<>(asList(
                Parameter.builder().parameterKey("DBInstanceClassParameter").parameterValue(awsRdsInstanceView.getDBInstanceClass()).build(),
                Parameter.builder().parameterKey("DBInstanceIdentifierParameter").parameterValue(awsRdsInstanceView.getDBInstanceIdentifier()).build(),
                Parameter.builder().parameterKey("DBSubnetGroupNameParameter").parameterValue(awsRdsDbSubnetGroupView.getDBSubnetGroupName()).build(),
                Parameter.builder().parameterKey("DBSubnetGroupSubnetIdsParameter").parameterValue(String.join(",", awsNetworkView.getSubnetList())).build(),
                Parameter.builder().parameterKey("EngineParameter").parameterValue(awsRdsInstanceView.getEngine()).build(),
                Parameter.builder().parameterKey("MasterUsernameParameter").parameterValue(awsRdsInstanceView.getMasterUsername()).build(),
                Parameter.builder().parameterKey("MasterUserPasswordParameter").parameterValue(awsRdsInstanceView.getMasterUserPassword()).build(),
                Parameter.builder().parameterKey("DeletionProtectionParameter").parameterValue(deleteProtection ? "true" : "false").build())
        );

        addParameterIfNotNull(parameters, "AllocatedStorageParameter", awsRdsInstanceView.getAllocatedStorage());
        addParameterIfNotNull(parameters, "BackupRetentionPeriodParameter", awsRdsInstanceView.getBackupRetentionPeriod());
        addParameterIfNotNull(parameters, "EngineVersionParameter", awsRdsInstanceView.getEngineVersion());
        addParameterIfNotNull(parameters, "MultiAZParameter", awsRdsInstanceView.getMultiAZ());
        addParameterIfNotNull(parameters, "StorageTypeParameter", awsRdsInstanceView.getStorageType());
        addParameterIfNotNull(parameters, "PortParameter", databaseServer.getPort());
        boolean useSslEnforcement = databaseServer.isUseSslEnforcement();
        addParameterIfNotNull(parameters, "DBParameterGroupNameParameter", useSslEnforcement ? awsRdsDbParameterGroupView.getDBParameterGroupName() : null);
        addParameterIfNotNull(parameters, "DBParameterGroupFamilyParameter",
                useSslEnforcement ? awsRdsDbParameterGroupView.getDBParameterGroupFamily() : null);
        addParameterIfNotNull(parameters, "SslCertificateIdentifierParameter",
                useSslEnforcement && awsRdsInstanceView.isSslCertificateIdentifierDefined() ? awsRdsInstanceView.getSslCertificateIdentifier() : null);

        if (awsRdsInstanceView.getVPCSecurityGroups().isEmpty()) {
            // VPC-id and VPC cidr should be filled in
            parameters.addAll(
                    asList(
                            Parameter.builder().parameterKey("VPCIdParameter").parameterValue(String.valueOf(awsNetworkView.getExistingVpc())).build(),
                            Parameter.builder().parameterKey("DBSecurityGroupNameParameter")
                                    .parameterValue(awsRdsVpcSecurityGroupView.getDBSecurityGroupName()).build()
                    )
            );
        } else {
            parameters.add(
                    Parameter.builder().parameterKey("VPCSecurityGroupsParameter")
                            .parameterValue(String.join(",", awsRdsInstanceView.getVPCSecurityGroups()))
                            .build()
            );
        }

        return parameters;
    }

    private void addParameterIfNotNull(List<Parameter> parameters, String key, Object value) {
        if (value != null) {
            parameters.add(Parameter.builder().parameterKey(key).parameterValue(Objects.toString(value)).build());
        }
    }
}
