package com.sequenceiq.cloudbreak.cloud.aws;

import static com.amazonaws.services.cloudformation.model.StackStatus.CREATE_COMPLETE;
import static com.amazonaws.services.cloudformation.model.StackStatus.CREATE_FAILED;
import static com.amazonaws.services.cloudformation.model.StackStatus.DELETE_COMPLETE;
import static com.amazonaws.services.cloudformation.model.StackStatus.DELETE_FAILED;
import static com.amazonaws.services.cloudformation.model.StackStatus.ROLLBACK_COMPLETE;
import static com.amazonaws.services.cloudformation.model.StackStatus.ROLLBACK_FAILED;
import static com.amazonaws.services.cloudformation.model.StackStatus.ROLLBACK_IN_PROGRESS;
import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.net.util.SubnetUtils;
import org.apache.commons.net.util.SubnetUtils.SubnetInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.services.autoscaling.AmazonAutoScalingClient;
import com.amazonaws.services.autoscaling.model.AutoScalingGroup;
import com.amazonaws.services.autoscaling.model.DescribeAutoScalingGroupsRequest;
import com.amazonaws.services.autoscaling.model.DetachInstancesRequest;
import com.amazonaws.services.autoscaling.model.ResumeProcessesRequest;
import com.amazonaws.services.autoscaling.model.SuspendProcessesRequest;
import com.amazonaws.services.autoscaling.model.UpdateAutoScalingGroupRequest;
import com.amazonaws.services.cloudformation.AmazonCloudFormationClient;
import com.amazonaws.services.cloudformation.model.CreateStackRequest;
import com.amazonaws.services.cloudformation.model.DeleteStackRequest;
import com.amazonaws.services.cloudformation.model.DescribeStacksRequest;
import com.amazonaws.services.cloudformation.model.OnFailure;
import com.amazonaws.services.cloudformation.model.Output;
import com.amazonaws.services.cloudformation.model.Parameter;
import com.amazonaws.services.cloudformation.model.StackStatus;
import com.amazonaws.services.ec2.AmazonEC2Client;
import com.amazonaws.services.ec2.model.Address;
import com.amazonaws.services.ec2.model.AssociateAddressRequest;
import com.amazonaws.services.ec2.model.DeleteKeyPairRequest;
import com.amazonaws.services.ec2.model.DescribeAddressesRequest;
import com.amazonaws.services.ec2.model.DescribeAddressesResult;
import com.amazonaws.services.ec2.model.DescribeImagesRequest;
import com.amazonaws.services.ec2.model.DescribeImagesResult;
import com.amazonaws.services.ec2.model.DescribeKeyPairsRequest;
import com.amazonaws.services.ec2.model.DescribeSubnetsRequest;
import com.amazonaws.services.ec2.model.DescribeSubnetsResult;
import com.amazonaws.services.ec2.model.DescribeVpcsRequest;
import com.amazonaws.services.ec2.model.DisassociateAddressRequest;
import com.amazonaws.services.ec2.model.Filter;
import com.amazonaws.services.ec2.model.Image;
import com.amazonaws.services.ec2.model.ImportKeyPairRequest;
import com.amazonaws.services.ec2.model.ReleaseAddressRequest;
import com.amazonaws.services.ec2.model.Subnet;
import com.amazonaws.services.ec2.model.TerminateInstancesRequest;
import com.amazonaws.services.ec2.model.Vpc;
import com.google.common.collect.Lists;
import com.google.common.net.InetAddresses;
import com.sequenceiq.cloudbreak.api.model.AdjustmentType;
import com.sequenceiq.cloudbreak.api.model.InstanceGroupType;
import com.sequenceiq.cloudbreak.cloud.ResourceConnector;
import com.sequenceiq.cloudbreak.cloud.aws.CloudFormationTemplateBuilder.ModelContext;
import com.sequenceiq.cloudbreak.cloud.aws.task.AwsPollTaskFactory;
import com.sequenceiq.cloudbreak.cloud.aws.view.AwsCredentialView;
import com.sequenceiq.cloudbreak.cloud.aws.view.AwsInstanceProfileView;
import com.sequenceiq.cloudbreak.cloud.aws.view.AwsNetworkView;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.exception.CloudConnectorException;
import com.sequenceiq.cloudbreak.cloud.exception.TemplatingDoesNotSupportedException;
import com.sequenceiq.cloudbreak.cloud.model.CloudInstance;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource.Builder;
import com.sequenceiq.cloudbreak.cloud.model.CloudResourceStatus;
import com.sequenceiq.cloudbreak.cloud.model.CloudStack;
import com.sequenceiq.cloudbreak.cloud.model.Group;
import com.sequenceiq.cloudbreak.cloud.model.InstanceStatus;
import com.sequenceiq.cloudbreak.cloud.model.Network;
import com.sequenceiq.cloudbreak.cloud.model.ResourceStatus;
import com.sequenceiq.cloudbreak.cloud.model.TlsInfo;
import com.sequenceiq.cloudbreak.cloud.notification.PersistenceNotifier;
import com.sequenceiq.cloudbreak.cloud.scheduler.SyncPollingScheduler;
import com.sequenceiq.cloudbreak.cloud.task.PollTask;
import com.sequenceiq.cloudbreak.common.type.ResourceType;
import com.sequenceiq.cloudbreak.service.Retry;
import com.sequenceiq.cloudbreak.service.Retry.ActionWentFail;

import freemarker.template.Configuration;

@Service
public class AwsResourceConnector implements ResourceConnector<Object> {
    private static final Logger LOGGER = LoggerFactory.getLogger(AwsResourceConnector.class);

    private static final List<String> CAPABILITY_IAM = singletonList("CAPABILITY_IAM");

    private static final int INCREMENT_HOST_NUM = 256;

    private static final int CIDR_PREFIX = 24;

    private static final List<String> SUSPENDED_PROCESSES = asList("Launch", "HealthCheck", "ReplaceUnhealthy", "AZRebalance", "AlarmNotification",
            "ScheduledActions", "AddToLoadBalancer", "RemoveFromLoadBalancerLowPriority");

    private static final List<StackStatus> ERROR_STATUSES = asList(CREATE_FAILED, ROLLBACK_IN_PROGRESS, ROLLBACK_FAILED, ROLLBACK_COMPLETE);

    private static final String CFS_OUTPUT_EIPALLOCATION_ID = "EIPAllocationID";

    private static final String S3_ACCESS_ROLE = "S3AccessRole";

    private static final String CREATED_VPC = "CreatedVpc";

    private static final String CREATED_SUBNET = "CreatedSubnet";

    @Inject
    private Configuration freemarkerConfiguration;

    @Inject
    private AwsClient awsClient;

    @Inject
    private CloudFormationStackUtil cfStackUtil;

    @Inject
    private SyncPollingScheduler<Boolean> syncPollingScheduler;

    @Inject
    private CloudFormationTemplateBuilder cloudFormationTemplateBuilder;

    @Inject
    private AwsPollTaskFactory awsPollTaskFactory;

    @Inject
    private CloudFormationStackUtil cloudFormationStackUtil;

    @Inject
    private AwsTagPreparationService awsTagPreparationService;

    @Inject
    private EncryptedSnapshotPreparator encryptedSnapshotPreparator;

    @Value("${cb.publicip:}")
    private String cloudbreakPublicIp;

    @Value("${cb.aws.default.inbound.security.group:}")
    private String defaultInboundSecurityGroup;

    @Value("${cb.aws.vpc:}")
    private String cloudbreakVpc;

    @Value("${cb.nginx.port:9443}")
    private int gatewayPort;

    @Value("${cb.knox.port:8443}")
    private int knoxPort;

    @Value("${cb.aws.cf.template.new.path:}")
    private String awsCloudformationTemplatePath;

    @Value("${cb.default.gateway.cidr:}")
    private String defaultGatewayCidr;

    @Inject
    @Qualifier("DefaultRetryService")
    private Retry retryService;

    @Override
    public List<CloudResourceStatus> launch(AuthenticatedContext ac, CloudStack stack, PersistenceNotifier resourceNotifier,
            AdjustmentType adjustmentType, Long threshold) throws Exception {
        createKeyPair(ac, stack);
        String cFStackName = cfStackUtil.getCfStackName(ac);
        AwsCredentialView credentialView = new AwsCredentialView(ac.getCloudCredential());
        String regionName = ac.getCloudContext().getLocation().getRegion().value();
        AmazonCloudFormationClient cfClient = awsClient.createCloudFormationClient(credentialView, regionName);
        AmazonEC2Client amazonEC2Client = awsClient.createAccess(credentialView, regionName);
        AwsNetworkView awsNetworkView = new AwsNetworkView(stack.getNetwork());
        boolean existingVPC = awsNetworkView.isExistingVPC();
        boolean existingSubnet = awsNetworkView.isExistingSubnet();
        boolean mapPublicIpOnLaunch = isMapPublicOnLaunch(awsNetworkView, amazonEC2Client);
        try {
            cfClient.describeStacks(new DescribeStacksRequest().withStackName(cFStackName));
            LOGGER.info("Stack already exists: {}", cFStackName);
        } catch (AmazonServiceException ignored) {
            CloudResource cloudFormationStack = new Builder().type(ResourceType.CLOUDFORMATION_STACK).name(cFStackName).build();
            resourceNotifier.notifyAllocation(cloudFormationStack, ac.getCloudContext());

            String cidr = stack.getNetwork().getSubnet().getCidr();
            String subnet = isNoCIDRProvided(existingVPC, existingSubnet, cidr) ? findNonOverLappingCIDR(ac, stack) : cidr;
            String inboundSecurityGroup = deployingToSameVPC(awsNetworkView, existingVPC)
                    ? defaultInboundSecurityGroup : "";
            AwsInstanceProfileView awsInstanceProfileView = new AwsInstanceProfileView(stack);
            ModelContext modelContext = new ModelContext()
                    .withAuthenticatedContext(ac)
                    .withStack(stack)
                    .withExistingVpc(existingVPC)
                    .withSnapshotId(getEbsSnapshotIdIfNeeded(ac, stack))
                    .withExistingIGW(awsNetworkView.isExistingIGW())
                    .withExistingSubnetCidr(existingSubnet ? getExistingSubnetCidr(ac, stack) : null)
                    .withExistingSubnetIds(existingSubnet ? awsNetworkView.getSubnetList() : null)
                    .mapPublicIpOnLaunch(mapPublicIpOnLaunch)
                    .withEnableInstanceProfile(awsInstanceProfileView.isEnableInstanceProfileStrategy())
                    .withInstanceProfileAvailable(awsInstanceProfileView.isInstanceProfileAvailable())
                    .withTemplate(stack.getTemplate())
                    .withDefaultSubnet(subnet)
                    .withCloudbreakPublicIp(cloudbreakPublicIp)
                    .withDefaultInboundSecurityGroup(inboundSecurityGroup)
                    .withGatewayPort(gatewayPort)
                    .withKnoxPort(knoxPort)
                    .withDefaultGatewayCidr(defaultGatewayCidr);
            String cfTemplate = cloudFormationTemplateBuilder.build(modelContext);
            LOGGER.debug("CloudFormationTemplate: {}", cfTemplate);
            cfClient.createStack(createCreateStackRequest(ac, stack, cFStackName, subnet, cfTemplate));
        }
        LOGGER.info("CloudFormation stack creation request sent with stack name: '{}' for stack: '{}'", cFStackName, ac.getCloudContext().getId());
        AmazonAutoScalingClient asClient = awsClient.createAutoScalingClient(credentialView, regionName);
        PollTask<Boolean> task = awsPollTaskFactory.newAwsCreateStackStatusCheckerTask(ac, cfClient, asClient, CREATE_COMPLETE, CREATE_FAILED, ERROR_STATUSES,
                cFStackName);
        try {
            Boolean statePollerResult = task.call();
            if (!task.completed(statePollerResult)) {
                syncPollingScheduler.schedule(task);
            }
        } catch (RuntimeException e) {
            throw new CloudConnectorException(e.getMessage(), e);
        }

        AmazonAutoScalingClient amazonASClient = awsClient.createAutoScalingClient(credentialView, regionName);
        saveS3AccessRoleArn(ac, stack, cFStackName, cfClient, resourceNotifier);
        saveGeneratedSubnet(ac, stack, cFStackName, cfClient, resourceNotifier);
        List<CloudResource> cloudResources = getCloudResources(ac, stack, cFStackName, cfClient, amazonEC2Client, amazonASClient, mapPublicIpOnLaunch);
        return check(ac, cloudResources);
    }

    private void createKeyPair(AuthenticatedContext ac, CloudStack stack) {
        if (!awsClient.existingKeyPairNameSpecified(stack.getInstanceAuthentication())) {
            AwsCredentialView awsCredential = new AwsCredentialView(ac.getCloudCredential());
            try {
                String region = ac.getCloudContext().getLocation().getRegion().value();
                LOGGER.info(String.format("Importing public key to %s region on AWS", region));
                AmazonEC2Client client = awsClient.createAccess(awsCredential, region);
                String keyPairName = awsClient.getKeyPairName(ac);
                ImportKeyPairRequest importKeyPairRequest = new ImportKeyPairRequest(keyPairName, stack.getInstanceAuthentication().getPublicKey());
                try {
                    client.describeKeyPairs(new DescribeKeyPairsRequest().withKeyNames(keyPairName));
                    LOGGER.info("Key-pair already exists: {}", keyPairName);
                } catch (AmazonServiceException e) {
                    client.importKeyPair(importKeyPairRequest);
                }
            } catch (Exception e) {
                String errorMessage = String.format("Failed to import public key [roleArn:'%s'], detailed message: %s", awsCredential.getRoleArn(),
                        e.getMessage());
                LOGGER.error(errorMessage, e);
                throw new CloudConnectorException(e.getMessage(), e);
            }
        }
    }

    private boolean isNoCIDRProvided(boolean existingVPC, boolean existingSubnet, String cidr) {
        return existingVPC && !existingSubnet && cidr == null;
    }

    private boolean deployingToSameVPC(AwsNetworkView awsNetworkView, boolean existingVPC) {
        return StringUtils.isNoneEmpty(cloudbreakVpc) && existingVPC && awsNetworkView.getExistingVPC().equals(cloudbreakVpc);
    }

    private CreateStackRequest createCreateStackRequest(AuthenticatedContext ac, CloudStack stack, String cFStackName, String subnet, String cfTemplate) {
        return new CreateStackRequest()
                .withStackName(cFStackName)
                .withOnFailure(OnFailure.DO_NOTHING)
                .withTemplateBody(cfTemplate)
                .withTags(awsTagPreparationService.prepareTags(ac, stack.getTags()))
                .withCapabilities(CAPABILITY_IAM)
                .withParameters(getStackParameters(ac, stack, cFStackName, subnet));
    }

    private List<CloudResource> getCloudResources(AuthenticatedContext ac, CloudStack stack, String cFStackName, AmazonCloudFormationClient client,
            AmazonEC2Client amazonEC2Client, AmazonAutoScalingClient amazonASClient, boolean mapPublicIpOnLaunch) {
        List<CloudResource> cloudResources = new ArrayList<>();
        AmazonCloudFormationClient cloudFormationClient = awsClient.createCloudFormationClient(new AwsCredentialView(ac.getCloudCredential()),
                ac.getCloudContext().getLocation().getRegion().value());
        scheduleStatusChecks(stack, ac, cloudFormationClient);
        suspendAutoScaling(ac, stack);
        if (mapPublicIpOnLaunch) {
            Map<String, String> eipAllocationIds = getElasticIpAllocationIds(cFStackName, client);
            List<Group> gateways = getGatewayGroups(stack.getGroups());
            for (Group gateway : gateways) {
                List<String> eips = getEipsForGatewayGroup(eipAllocationIds, gateway);
                List<String> instanceIds = getInstancesForGroup(ac, amazonASClient, client, gateway);
                associateElasticIpsToInstances(amazonEC2Client, eips, instanceIds);
            }
        }
        return cloudResources;
    }

    private void saveS3AccessRoleArn(AuthenticatedContext ac, CloudStack stack, String cFStackName, AmazonCloudFormationClient client,
            PersistenceNotifier resourceNotifier) {
        AwsInstanceProfileView awsInstanceProfileView = new AwsInstanceProfileView(stack);
        if (awsInstanceProfileView.isEnableInstanceProfileStrategy() && !awsInstanceProfileView.isInstanceProfileAvailable()) {
            String s3AccessRoleArn = getCreatedS3AccessRoleArn(cFStackName, client);
            CloudResource s3AccessRoleArnCloudResource = new Builder().type(ResourceType.S3_ACCESS_ROLE_ARN).name(s3AccessRoleArn).build();
            resourceNotifier.notifyAllocation(s3AccessRoleArnCloudResource, ac.getCloudContext());
        }
    }

    private void saveGeneratedSubnet(AuthenticatedContext ac, CloudStack stack, String cFStackName, AmazonCloudFormationClient client,
            PersistenceNotifier resourceNotifier) {
        AwsNetworkView awsNetworkView = new AwsNetworkView(stack.getNetwork());
        if (awsNetworkView.isExistingVPC()) {
            String vpcId = awsNetworkView.getExistingVPC();
            CloudResource vpc = new Builder().type(ResourceType.AWS_VPC).name(vpcId).build();
            resourceNotifier.notifyAllocation(vpc, ac.getCloudContext());
        } else {
            String vpcId = getCreatedVpc(cFStackName, client);
            CloudResource vpc = new Builder().type(ResourceType.AWS_VPC).name(vpcId).build();
            resourceNotifier.notifyAllocation(vpc, ac.getCloudContext());
        }

        if (awsNetworkView.isExistingSubnet()) {
            String subnetId = awsNetworkView.getExistingSubnet();
            CloudResource subnet = new Builder().type(ResourceType.AWS_SUBNET).name(subnetId).build();
            resourceNotifier.notifyAllocation(subnet, ac.getCloudContext());
        } else {
            String subnetId = getCreatedSubnet(cFStackName, client);
            CloudResource subnet = new Builder().type(ResourceType.AWS_SUBNET).name(subnetId).build();
            resourceNotifier.notifyAllocation(subnet, ac.getCloudContext());
        }
    }

    private String getCreatedVpc(String cFStackName, AmazonCloudFormationClient client) {
        Map<String, String> outputs = getOutputs(cFStackName, client);
        if (outputs.containsKey(CREATED_VPC)) {
            return outputs.get(CREATED_VPC);
        } else {
            String outputKeyNotFound = String.format("Vpc could not be found in the Cloudformation stack('%s') output.", cFStackName);
            throw new CloudConnectorException(outputKeyNotFound);
        }
    }

    private String getCreatedSubnet(String cFStackName, AmazonCloudFormationClient client) {
        Map<String, String> outputs = getOutputs(cFStackName, client);
        if (outputs.containsKey(CREATED_SUBNET)) {
            return outputs.get(CREATED_SUBNET);
        } else {
            String outputKeyNotFound = String.format("Subnet could not be found in the Cloudformation stack('%s') output.", cFStackName);
            throw new CloudConnectorException(outputKeyNotFound);
        }
    }

    private String getCreatedS3AccessRoleArn(String cFStackName, AmazonCloudFormationClient client) {
        Map<String, String> outputs = getOutputs(cFStackName, client);
        if (outputs.containsKey(S3_ACCESS_ROLE)) {
            return outputs.get(S3_ACCESS_ROLE);
        } else {
            String outputKeyNotFound = String.format("S3AccessRole arn could not be found in the Cloudformation stack('%s') output.", cFStackName);
            throw new CloudConnectorException(outputKeyNotFound);
        }
    }

    private Map<String, String> getElasticIpAllocationIds(String cFStackName, AmazonCloudFormationClient client) {
        Map<String, String> outputs = getOutputs(cFStackName, client);
        Map<String, String> elasticIpIds = outputs.entrySet().stream().filter(e -> e.getKey().startsWith(CFS_OUTPUT_EIPALLOCATION_ID))
                .collect(Collectors.toMap(e -> e.getKey(), e -> e.getValue()));
        if (!elasticIpIds.isEmpty()) {
            return elasticIpIds;
        } else {
            String outputKeyNotFound = String.format("Allocation Id of Elastic IP could not be found in the Cloudformation stack('%s') output.", cFStackName);
            throw new CloudConnectorException(outputKeyNotFound);
        }
    }

    private Map<String, String> getOutputs(String cFStackName, AmazonCloudFormationClient client) {
        DescribeStacksRequest describeStacksRequest = new DescribeStacksRequest().withStackName(cFStackName);
        String outputNotFound = String.format("Couldn't get Cloudformation stack's('%s') output", cFStackName);
        List<Output> cfStackOutputs = client.describeStacks(describeStacksRequest).getStacks()
                .stream().findFirst().orElseThrow(getCloudConnectorExceptionSupplier(outputNotFound)).getOutputs();
        return cfStackOutputs.stream().collect(Collectors.toMap(Output::getOutputKey, Output::getOutputValue));
    }

    private void associateElasticIpsToInstances(AmazonEC2Client amazonEC2Client, List<String> eipAllocationIds, List<String> instanceIds) {
        if (eipAllocationIds.size() == instanceIds.size()) {
            for (int i = 0; i < eipAllocationIds.size(); i++) {
                associateElasticIpToInstance(amazonEC2Client, eipAllocationIds.get(i), instanceIds.get(i));
            }
        } else {
            LOGGER.warn("The number of elastic ips are not equals with the number of instances. EIP association will be skipped!");
        }
    }

    private void associateElasticIpToInstance(AmazonEC2Client amazonEC2Client, String eipAllocationId, String instanceId) {
        AssociateAddressRequest associateAddressRequest = new AssociateAddressRequest()
                .withAllocationId(eipAllocationId)
                .withInstanceId(instanceId);
        amazonEC2Client.associateAddress(associateAddressRequest);
    }

    private Supplier<CloudConnectorException> getCloudConnectorExceptionSupplier(String msg) {
        return () -> new CloudConnectorException(msg);
    }

    private void suspendAutoScaling(AuthenticatedContext ac, CloudStack stack) {
        AmazonAutoScalingClient amazonASClient = awsClient.createAutoScalingClient(new AwsCredentialView(ac.getCloudCredential()),
                ac.getCloudContext().getLocation().getRegion().value());
        for (Group group : stack.getGroups()) {
            String asGroupName = cfStackUtil.getAutoscalingGroupName(ac, group.getName(), ac.getCloudContext().getLocation().getRegion().value());
            amazonASClient.suspendProcesses(new SuspendProcessesRequest().withAutoScalingGroupName(asGroupName).withScalingProcesses(SUSPENDED_PROCESSES));
        }
    }

    private void resumeAutoScaling(AuthenticatedContext ac, CloudStack stack) {
        AmazonAutoScalingClient amazonASClient = awsClient.createAutoScalingClient(new AwsCredentialView(ac.getCloudCredential()),
                ac.getCloudContext().getLocation().getRegion().value());
        for (Group group : stack.getGroups()) {
            String asGroupName = cfStackUtil.getAutoscalingGroupName(ac, group.getName(), ac.getCloudContext().getLocation().getRegion().value());
            amazonASClient.resumeProcesses(new ResumeProcessesRequest().withAutoScalingGroupName(asGroupName).withScalingProcesses(SUSPENDED_PROCESSES));
        }
    }

    private List<Parameter> getStackParameters(AuthenticatedContext ac, CloudStack stack, String stackName, String newSubnetCidr) {
        AwsNetworkView awsNetworkView = new AwsNetworkView(stack.getNetwork());
        AwsInstanceProfileView awsInstanceProfileView = new AwsInstanceProfileView(stack);
        String keyPairName = awsClient.getKeyPairName(ac);
        if (awsClient.existingKeyPairNameSpecified(stack.getInstanceAuthentication())) {
            keyPairName = awsClient.getExistingKeyPairName(stack.getInstanceAuthentication());
        }

        List<Parameter> parameters = new ArrayList<>(asList(
                new Parameter().withParameterKey("CBUserData").withParameterValue(stack.getImage().getUserData(InstanceGroupType.CORE)),
                new Parameter().withParameterKey("CBGateWayUserData").withParameterValue(stack.getImage().getUserData(InstanceGroupType.GATEWAY)),
                new Parameter().withParameterKey("StackName").withParameterValue(stackName),
                new Parameter().withParameterKey("StackOwner").withParameterValue(ac.getCloudContext().getOwner()),
                new Parameter().withParameterKey("KeyName").withParameterValue(keyPairName),
                new Parameter().withParameterKey("AMI").withParameterValue(stack.getImage().getImageName()),
                new Parameter().withParameterKey("RootDeviceName").withParameterValue(getRootDeviceName(ac, stack))
        ));
        if (awsInstanceProfileView.isUseExistingInstanceProfile() && awsInstanceProfileView.isEnableInstanceProfileStrategy()) {
            parameters.add(new Parameter().withParameterKey("InstanceProfile").withParameterValue(awsInstanceProfileView.getInstanceProfile()));
        }
        if (ac.getCloudContext().getLocation().getAvailabilityZone().value() != null) {
            parameters.add(new Parameter().withParameterKey("AvailabilitySet")
                    .withParameterValue(ac.getCloudContext().getLocation().getAvailabilityZone().value()));
        }
        if (awsNetworkView.isExistingVPC()) {
            parameters.add(new Parameter().withParameterKey("VPCId").withParameterValue(awsNetworkView.getExistingVPC()));
            if (awsNetworkView.isExistingIGW()) {
                parameters.add(new Parameter().withParameterKey("InternetGatewayId").withParameterValue(awsNetworkView.getExistingIGW()));
            }
            if (awsNetworkView.isExistingSubnet()) {
                parameters.add(new Parameter().withParameterKey("SubnetId").withParameterValue(awsNetworkView.getExistingSubnet()));
            } else {
                parameters.add(new Parameter().withParameterKey("SubnetCIDR").withParameterValue(newSubnetCidr));
            }
        }
        return parameters;
    }

    private List<String> getExistingSubnetCidr(AuthenticatedContext ac, CloudStack stack) {
        AwsNetworkView awsNetworkView = new AwsNetworkView(stack.getNetwork());
        String region = ac.getCloudContext().getLocation().getRegion().value();
        AmazonEC2Client ec2Client = awsClient.createAccess(new AwsCredentialView(ac.getCloudCredential()), region);
        DescribeSubnetsRequest subnetsRequest = new DescribeSubnetsRequest().withSubnetIds(awsNetworkView.getSubnetList());
        List<Subnet> subnets = ec2Client.describeSubnets(subnetsRequest).getSubnets();
        if (subnets.isEmpty()) {
            throw new CloudConnectorException("The specified subnet does not exist (maybe it's in a different region).");
        }
        List<String> cidrs = Lists.newArrayList();
        for (Subnet subnet : subnets) {
            cidrs.add(subnet.getCidrBlock());
        }
        return cidrs;
    }

    private String getRootDeviceName(AuthenticatedContext ac, CloudStack cloudStack) {
        AmazonEC2Client ec2Client = awsClient.createAccess(new AwsCredentialView(ac.getCloudCredential()),
                ac.getCloudContext().getLocation().getRegion().value());
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

    @Override
    public List<CloudResourceStatus> check(AuthenticatedContext authenticatedContext, List<CloudResource> resources) {
        return new ArrayList<>();
    }

    @Override
    public List<CloudResourceStatus> terminate(AuthenticatedContext ac, CloudStack stack, List<CloudResource> resources) {
        LOGGER.info("Deleting stack: {}", ac.getCloudContext().getId());
        AwsCredentialView credentialView = new AwsCredentialView(ac.getCloudCredential());
        String regionName = ac.getCloudContext().getLocation().getRegion().value();
        if (resources != null && !resources.isEmpty()) {
            AmazonCloudFormationClient cfClient = awsClient.createCloudFormationClient(credentialView, regionName);
            CloudResource stackResource = getCloudFormationStackResource(resources);
            if (stackResource == null) {
                return Collections.emptyList();
            }
            String cFStackName = stackResource.getName();
            LOGGER.info("Deleting CloudFormation stack for stack: {} [cf stack id: {}]", cFStackName, ac.getCloudContext().getId());
            DescribeStacksRequest describeStacksRequest = new DescribeStacksRequest().withStackName(cFStackName);
            try {
                retryService.testWith2SecDelayMax5Times(() -> {
                    try {
                        cfClient.describeStacks(describeStacksRequest);
                    } catch (AmazonServiceException e) {
                        if (!e.getErrorMessage().contains(cFStackName + " does not exist")) {
                            throw e;
                        }
                        throw new ActionWentFail("Stack not exists");
                    }
                    return Boolean.TRUE;
                });
            } catch (ActionWentFail ignored) {
                LOGGER.info(String.format("Stack not found with name: %s", cFStackName));
                AmazonEC2Client amazonEC2Client = awsClient.createAccess(credentialView, regionName);
                releaseReservedIp(amazonEC2Client, resources);
                return Collections.emptyList();
            }
            resumeAutoScalingPolicies(ac, stack);
            DeleteStackRequest deleteStackRequest = new DeleteStackRequest().withStackName(cFStackName);
            cfClient.deleteStack(deleteStackRequest);
            PollTask<Boolean> task = awsPollTaskFactory.newAwsTerminateStackStatusCheckerTask(ac, cfClient, DELETE_COMPLETE, DELETE_FAILED, ERROR_STATUSES,
                    cFStackName);
            try {
                Boolean statePollerResult = task.call();
                if (!task.completed(statePollerResult)) {
                    syncPollingScheduler.schedule(task);
                }
            } catch (Exception e) {
                throw new CloudConnectorException(e.getMessage(), e);
            }
            AmazonEC2Client amazonEC2Client = awsClient.createAccess(credentialView, regionName);
            releaseReservedIp(amazonEC2Client, resources);
            deleteKeyPair(ac, stack);
        } else if (resources != null) {
            AmazonEC2Client amazonEC2Client = awsClient.createAccess(credentialView, regionName);
            releaseReservedIp(amazonEC2Client, resources);
            LOGGER.info("No CloudFormation stack saved for stack.");
        } else {
            LOGGER.info("No resources to release.");
        }
        return check(ac, resources);
    }

    private void deleteKeyPair(AuthenticatedContext ac, CloudStack stack) {
        AwsCredentialView awsCredential = new AwsCredentialView(ac.getCloudCredential());
        String region = ac.getCloudContext().getLocation().getRegion().value();
        if (!awsClient.existingKeyPairNameSpecified(stack.getInstanceAuthentication())) {
            try {
                AmazonEC2Client client = awsClient.createAccess(awsCredential, region);
                DeleteKeyPairRequest deleteKeyPairRequest = new DeleteKeyPairRequest(awsClient.getKeyPairName(ac));
                client.deleteKeyPair(deleteKeyPairRequest);
            } catch (Exception e) {
                String errorMessage = String.format("Failed to delete public key [roleArn:'%s', region: '%s'], detailed message: %s",
                        awsCredential.getRoleArn(), region, e.getMessage());
                LOGGER.error(errorMessage, e);
            }
        }
    }

    private void resumeAutoScalingPolicies(AuthenticatedContext ac, CloudStack stack) {
        for (Group instanceGroup : stack.getGroups()) {
            try {
                String asGroupName = cfStackUtil.getAutoscalingGroupName(ac, instanceGroup.getName(), ac.getCloudContext().getLocation().getRegion().value());
                if (asGroupName != null) {
                    AmazonAutoScalingClient amazonASClient = awsClient.createAutoScalingClient(new AwsCredentialView(ac.getCloudCredential()),
                            ac.getCloudContext().getLocation().getRegion().value());
                    List<AutoScalingGroup> asGroups = amazonASClient.describeAutoScalingGroups(new DescribeAutoScalingGroupsRequest()
                            .withAutoScalingGroupNames(asGroupName)).getAutoScalingGroups();
                    if (!asGroups.isEmpty()) {
                        if (!asGroups.get(0).getSuspendedProcesses().isEmpty()) {
                            amazonASClient.updateAutoScalingGroup(new UpdateAutoScalingGroupRequest()
                                    .withAutoScalingGroupName(asGroupName)
                                    .withMinSize(0)
                                    .withDesiredCapacity(0));
                            amazonASClient.resumeProcesses(new ResumeProcessesRequest().withAutoScalingGroupName(asGroupName));
                        }
                    }
                } else {
                    LOGGER.info("Autoscaling Group's physical id is null (the resource doesn't exist), it is not needed to resume scaling policies.");
                }
            } catch (AmazonServiceException e) {
                if (e.getErrorMessage().matches("Resource.*does not exist for stack.*") || e.getErrorMessage().matches("Stack '.*' does not exist.*")) {
                    LOGGER.info(e.getMessage());
                } else {
                    throw e;
                }
            }
        }
    }

    private void releaseReservedIp(AmazonEC2Client client, List<CloudResource> resources) {
        CloudResource elasticIpResource = getReservedIp(resources);
        if (elasticIpResource != null && elasticIpResource.getName() != null) {
            Address address;
            try {
                DescribeAddressesResult describeResult = client.describeAddresses(
                        new DescribeAddressesRequest().withAllocationIds(elasticIpResource.getName()));
                address = describeResult.getAddresses().get(0);
            } catch (AmazonServiceException e) {
                if (e.getErrorMessage().equals("The allocation ID '" + elasticIpResource.getName() + "' does not exist")) {
                    LOGGER.warn("Elastic IP with allocation ID '{}' not found. Ignoring IP release.", elasticIpResource.getName());
                    return;
                } else {
                    throw e;
                }
            }
            if (address.getAssociationId() != null) {
                client.disassociateAddress(new DisassociateAddressRequest().withAssociationId(elasticIpResource.getName()));
            }
            client.releaseAddress(new ReleaseAddressRequest().withAllocationId(elasticIpResource.getName()));
        }
    }

    @Override
    public List<CloudResourceStatus> update(AuthenticatedContext authenticatedContext, CloudStack stack, List<CloudResource> resources) {
        return new ArrayList<>();
    }

    @Override
    public List<CloudResourceStatus> upscale(AuthenticatedContext ac, CloudStack stack, List<CloudResource> resources) {
        resumeAutoScaling(ac, stack);

        AmazonAutoScalingClient amazonASClient = awsClient.createAutoScalingClient(new AwsCredentialView(ac.getCloudCredential()),
                ac.getCloudContext().getLocation().getRegion().value());
        AmazonCloudFormationClient cloudFormationClient = awsClient.createCloudFormationClient(new AwsCredentialView(ac.getCloudCredential()),
                ac.getCloudContext().getLocation().getRegion().value());
        AmazonEC2Client amazonEC2Client = awsClient.createAccess(new AwsCredentialView(ac.getCloudCredential()),
                ac.getCloudContext().getLocation().getRegion().value());

        List<Group> scaledGroups = getScaledGroups(stack);
        for (Group group : scaledGroups) {
            String asGroupName = cfStackUtil.getAutoscalingGroupName(ac, cloudFormationClient, group.getName());

            amazonASClient.updateAutoScalingGroup(new UpdateAutoScalingGroupRequest()
                    .withAutoScalingGroupName(asGroupName)
                    .withMaxSize(group.getInstancesSize())
                    .withDesiredCapacity(group.getInstancesSize()));
            LOGGER.info("Updated Auto Scaling group's desiredCapacity: [stack: '{}', to: '{}']", ac.getCloudContext().getId(),
                    resources.size());
        }
        scheduleStatusChecks(stack, ac, cloudFormationClient);
        suspendAutoScaling(ac, stack);

        boolean mapPublicIpOnLaunch = isMapPublicOnLaunch(new AwsNetworkView(stack.getNetwork()), amazonEC2Client);
        List<Group> gateways = getGatewayGroups(scaledGroups);
        if (mapPublicIpOnLaunch && !gateways.isEmpty()) {
            String cFStackName = getCloudFormationStackResource(resources).getName();
            Map<String, String> eipAllocationIds = getElasticIpAllocationIds(cFStackName, cloudFormationClient);
            for (Group gateway : gateways) {
                List<String> eips = getEipsForGatewayGroup(eipAllocationIds, gateway);
                List<String> freeEips = getFreeIps(eips, amazonEC2Client);
                List<String> instanceIds = getInstancesForGroup(ac, amazonASClient, cloudFormationClient, gateway);
                List<String> newInstances = instanceIds.stream().filter(
                        iid -> gateway.getInstances().stream().noneMatch(inst -> iid.equals(inst.getInstanceId()))).collect(Collectors.toList());
                associateElasticIpsToInstances(amazonEC2Client, freeEips, newInstances);
            }
        }

        return singletonList(new CloudResourceStatus(getCloudFormationStackResource(resources), ResourceStatus.UPDATED));
    }

    @Override
    public Object collectResourcesToRemove(AuthenticatedContext authenticatedContext, CloudStack stack,
            List<CloudResource> resources, List<CloudInstance> vms) {
        return null;
    }

    @Override
    public List<CloudResourceStatus> downscale(AuthenticatedContext auth, CloudStack stack, List<CloudResource> resources, List<CloudInstance> vms,
            Object resourcesToRemove) {
        List<String> instanceIds = new ArrayList<>();
        for (CloudInstance vm : vms) {
            instanceIds.add(vm.getInstanceId());
        }
        String asGroupName = cfStackUtil.getAutoscalingGroupName(auth, vms.get(0).getTemplate().getGroupName(),
                auth.getCloudContext().getLocation().getRegion().value());
        DetachInstancesRequest detachInstancesRequest = new DetachInstancesRequest().withAutoScalingGroupName(asGroupName).withInstanceIds(instanceIds)
                .withShouldDecrementDesiredCapacity(true);
        AmazonAutoScalingClient amazonASClient = awsClient.createAutoScalingClient(new AwsCredentialView(auth.getCloudCredential()),
                auth.getCloudContext().getLocation().getRegion().value());
        try {
            amazonASClient.detachInstances(detachInstancesRequest);
        } catch (AmazonServiceException e) {
            if (!"ValidationError".equals(e.getErrorCode())
                    || !e.getErrorMessage().contains("not part of Auto Scaling")
                    || instanceIds.stream().anyMatch(id -> !e.getErrorMessage().contains(id))) {
                throw e;
            }
            LOGGER.info(e.getErrorMessage());
        }
        AmazonEC2Client amazonEC2Client = awsClient.createAccess(new AwsCredentialView(auth.getCloudCredential()),
                auth.getCloudContext().getLocation().getRegion().value());
        try {
            amazonEC2Client.terminateInstances(new TerminateInstancesRequest().withInstanceIds(instanceIds));
        } catch (AmazonServiceException e) {
            if (!"InvalidInstanceID.NotFound".equals(e.getErrorCode())) {
                throw e;
            }
            LOGGER.info(e.getErrorMessage());
        }
        LOGGER.info("Terminated instances in stack '{}': '{}'", auth.getCloudContext().getId(), instanceIds);
        return check(auth, resources);
    }

    @Override
    public TlsInfo getTlsInfo(AuthenticatedContext authenticatedContext, CloudStack cloudStack) {
        Network network = cloudStack.getNetwork();
        AwsNetworkView networkView = new AwsNetworkView(network);
        boolean sameVPC = deployingToSameVPC(networkView, networkView.isExistingVPC());
        return new TlsInfo(sameVPC);
    }

    @Override
    public String getStackTemplate() throws TemplatingDoesNotSupportedException {
        try {
            return freemarkerConfiguration.getTemplate(awsCloudformationTemplatePath, "UTF-8").toString();
        } catch (IOException e) {
            throw new CloudConnectorException("can't get freemarker template", e);
        }
    }

    private List<Group> getScaledGroups(CloudStack stack) {
        return stack.getGroups().stream().filter(g -> g.getInstances().stream().anyMatch(
                inst -> InstanceStatus.CREATE_REQUESTED == inst.getTemplate().getStatus())).collect(Collectors.toList());
    }

    private List<String> getEipsForGatewayGroup(Map<String, String> eipAllocationIds, Group gateway) {
        return eipAllocationIds.entrySet().stream().filter(e -> e.getKey().contains(gateway.getName().replace("_", ""))).map(e -> e.getValue())
                .collect(Collectors.toList());
    }

    private List<String> getInstancesForGroup(AuthenticatedContext ac, AmazonAutoScalingClient amazonASClient, AmazonCloudFormationClient client, Group group) {
        return cfStackUtil.getInstanceIds(amazonASClient, cfStackUtil.getAutoscalingGroupName(ac, client, group.getName()));
    }

    private List<Group> getGatewayGroups(List<Group> groups) {
        return groups.stream().filter(group -> group.getType() == InstanceGroupType.GATEWAY).collect(Collectors.toList());
    }

    private List<String> getFreeIps(List<String> eips, AmazonEC2Client amazonEC2Client) {
        DescribeAddressesResult addresses = amazonEC2Client.describeAddresses(new DescribeAddressesRequest().withAllocationIds(eips));
        return addresses.getAddresses().stream().filter(address -> address.getInstanceId() == null)
                .map(Address::getAllocationId).collect(Collectors.toList());
    }

    private boolean isMapPublicOnLaunch(AwsNetworkView awsNetworkView, AmazonEC2Client amazonEC2Client) {
        boolean mapPublicIpOnLaunch = true;
        if (awsNetworkView.isExistingVPC() && awsNetworkView.isExistingSubnet()) {
            DescribeSubnetsRequest describeSubnetsRequest = new DescribeSubnetsRequest();
            describeSubnetsRequest.setSubnetIds(awsNetworkView.getSubnetList());
            DescribeSubnetsResult describeSubnetsResult = amazonEC2Client.describeSubnets(describeSubnetsRequest);
            if (!describeSubnetsResult.getSubnets().isEmpty()) {
                mapPublicIpOnLaunch = describeSubnetsResult.getSubnets().get(0).isMapPublicIpOnLaunch();
            }
        }
        return mapPublicIpOnLaunch;
    }

    private void scheduleStatusChecks(CloudStack stack, AuthenticatedContext ac, AmazonCloudFormationClient cloudFormationClient) {
        for (Group group : stack.getGroups()) {
            String asGroupName = cfStackUtil.getAutoscalingGroupName(ac, cloudFormationClient, group.getName());
            LOGGER.info("Polling Auto Scaling group until new instances are ready. [stack: {}, asGroup: {}]", ac.getCloudContext().getId(),
                    asGroupName);
            PollTask<Boolean> task = awsPollTaskFactory.newASGroupStatusCheckerTask(ac, asGroupName, group.getInstancesSize(), awsClient, cfStackUtil);
            try {
                Boolean statePollerResult = task.call();
                if (!task.completed(statePollerResult)) {
                    syncPollingScheduler.schedule(task);
                }
            } catch (Exception e) {
                throw new CloudConnectorException(e.getMessage(), e);
            }
        }
    }

    private CloudResource getCloudFormationStackResource(List<CloudResource> cloudResources) {
        for (CloudResource cloudResource : cloudResources) {
            if (cloudResource.getType().equals(ResourceType.CLOUDFORMATION_STACK)) {
                return cloudResource;
            }
        }
        return null;
    }

    private CloudResource getReservedIp(List<CloudResource> cloudResources) {
        for (CloudResource cloudResource : cloudResources) {
            if (cloudResource.getType().equals(ResourceType.AWS_RESERVED_IP)) {
                return cloudResource;
            }
        }
        return null;
    }

    protected String findNonOverLappingCIDR(AuthenticatedContext ac, CloudStack stack) {
        AwsNetworkView awsNetworkView = new AwsNetworkView(stack.getNetwork());
        String region = ac.getCloudContext().getLocation().getRegion().value();
        AmazonEC2Client ec2Client = awsClient.createAccess(new AwsCredentialView(ac.getCloudCredential()), region);

        DescribeVpcsRequest vpcRequest = new DescribeVpcsRequest().withVpcIds(awsNetworkView.getExistingVPC());
        Vpc vpc = ec2Client.describeVpcs(vpcRequest).getVpcs().get(0);
        String vpcCidr = vpc.getCidrBlock();
        LOGGER.info("Subnet cidr is empty, find a non-overlapping subnet for VPC cidr: {}", vpcCidr);

        DescribeSubnetsRequest request = new DescribeSubnetsRequest().withFilters(new Filter("vpc-id", singletonList(awsNetworkView.getExistingVPC())));
        List<Subnet> awsSubnets = ec2Client.describeSubnets(request).getSubnets();
        List<String> subnetCidrs = awsSubnets.stream().map(Subnet::getCidrBlock).collect(Collectors.toList());
        LOGGER.info("The selected VPCs: {}, has the following subnets: {}", vpc.getVpcId(), subnetCidrs.stream().collect(Collectors.joining(",")));

        return calculateSubnet(ac.getCloudContext().getName(), vpc, subnetCidrs);
    }

    private String calculateSubnet(String stackName, Vpc vpc, List<String> subnetCidrs) {
        SubnetInfo vpcInfo = new SubnetUtils(vpc.getCidrBlock()).getInfo();
        String[] cidrParts = vpcInfo.getCidrSignature().split("/");
        int netmask = Integer.parseInt(cidrParts[cidrParts.length - 1]);
        int netmaskBits = CIDR_PREFIX - netmask;
        if (netmaskBits <= 0) {
            throw new CloudConnectorException("The selected VPC has to be in a bigger CIDR range than /24");
        }
        int numberOfSubnets = Double.valueOf(Math.pow(2, netmaskBits)).intValue();
        int targetSubnet = 0;
        if (stackName != null) {
            byte[] b = stackName.getBytes(Charset.forName("UTF-8"));
            for (byte ascii : b) {
                targetSubnet += ascii;
            }
        }
        targetSubnet = Long.valueOf(targetSubnet % numberOfSubnets).intValue();
        String cidr = getSubnetCidrInRange(vpc, subnetCidrs, targetSubnet, numberOfSubnets);
        if (cidr == null) {
            cidr = getSubnetCidrInRange(vpc, subnetCidrs, 0, targetSubnet);
        }
        if (cidr == null) {
            throw new CloudConnectorException("Cannot find non-overlapping CIDR range");
        }
        return cidr;
    }

    private String getSubnetCidrInRange(Vpc vpc, List<String> subnetCidrs, int start, int end) {
        SubnetInfo vpcInfo = new SubnetUtils(vpc.getCidrBlock()).getInfo();
        String lowProbe = incrementIp(vpcInfo.getLowAddress());
        String highProbe = new SubnetUtils(toSubnetCidr(lowProbe)).getInfo().getHighAddress();
        // start from the target subnet
        for (int i = 0; i < start - 1; i++) {
            lowProbe = incrementIp(lowProbe);
            highProbe = incrementIp(highProbe);
        }
        boolean foundProbe = false;
        for (int i = start; i < end; i++) {
            boolean overlapping = false;
            for (String subnetCidr : subnetCidrs) {
                SubnetInfo subnetInfo = new SubnetUtils(subnetCidr).getInfo();
                if (isInRange(lowProbe, subnetInfo) || isInRange(highProbe, subnetInfo)) {
                    overlapping = true;
                    break;
                }
            }
            if (overlapping) {
                lowProbe = incrementIp(lowProbe);
                highProbe = incrementIp(highProbe);
            } else {
                foundProbe = true;
                break;
            }
        }
        if (foundProbe && isInRange(highProbe, vpcInfo)) {
            String subnet = toSubnetCidr(lowProbe);
            LOGGER.info("The following subnet cidr found: {} for VPC: {}", subnet, vpc.getVpcId());
            return subnet;
        } else {
            return null;
        }
    }

    private String toSubnetCidr(String ip) {
        int ipValue = InetAddresses.coerceToInteger(InetAddresses.forString(ip)) - 1;
        return InetAddresses.fromInteger(ipValue).getHostAddress() + "/24";
    }

    private String incrementIp(String ip) {
        int ipValue = InetAddresses.coerceToInteger(InetAddresses.forString(ip)) + INCREMENT_HOST_NUM;
        return InetAddresses.fromInteger(ipValue).getHostAddress();
    }

    private boolean isInRange(String address, SubnetInfo subnetInfo) {
        int low = InetAddresses.coerceToInteger(InetAddresses.forString(subnetInfo.getLowAddress()));
        int high = InetAddresses.coerceToInteger(InetAddresses.forString(subnetInfo.getHighAddress()));
        int currentAddress = InetAddresses.coerceToInteger(InetAddresses.forString(address));
        return low <= currentAddress && currentAddress <= high;
    }

    private Map<String, String> getEbsSnapshotIdIfNeeded(AuthenticatedContext ac, CloudStack cloudStack) {
        Map<String, String> snapshotIdMap = new HashMap<>();
        for (Group group : cloudStack.getGroups()) {
            if (encryptedSnapshotPreparator.isEncryptedVolumeRequested(group)) {
                Optional<String> snapshot = encryptedSnapshotPreparator.createSnapshotIfNeeded(ac, group);
                if (snapshot.isPresent()) {
                    snapshotIdMap.put(group.getName(), snapshot.orElse(null));
                } else {
                    throw new CloudConnectorException(String.format("Failed to create Ebs encrypted volume on stack: %s", ac.getCloudContext().getId()));
                }
            }
        }
        return snapshotIdMap;
    }


}
