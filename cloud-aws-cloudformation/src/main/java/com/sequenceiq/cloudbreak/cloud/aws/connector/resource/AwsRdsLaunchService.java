package com.sequenceiq.cloudbreak.cloud.aws.connector.resource;

import static com.sequenceiq.cloudbreak.cloud.aws.common.AwsSdkErrorCodes.INSUFFICIENT_DB_INSTANCE_CAPACITY;
import static com.sequenceiq.cloudbreak.cloud.aws.scheduler.WaiterRunner.handleWaiterError;
import static software.amazon.awssdk.services.cloudformation.model.ResourceStatus.CREATE_FAILED;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import com.google.common.annotations.VisibleForTesting;
import com.sequenceiq.cloudbreak.cloud.aws.AwsCloudFormationClient;
import com.sequenceiq.cloudbreak.cloud.aws.AwsStackRequestHelper;
import com.sequenceiq.cloudbreak.cloud.aws.CloudFormationStackUtil;
import com.sequenceiq.cloudbreak.cloud.aws.CloudFormationTemplateBuilder;
import com.sequenceiq.cloudbreak.cloud.aws.CloudFormationTemplateBuilder.RDSModelContext;
import com.sequenceiq.cloudbreak.cloud.aws.client.AmazonCloudFormationClient;
import com.sequenceiq.cloudbreak.cloud.aws.common.view.AwsCredentialView;
import com.sequenceiq.cloudbreak.cloud.aws.common.view.AwsNetworkView;
import com.sequenceiq.cloudbreak.cloud.aws.scheduler.CancellableWaiterConfiguration;
import com.sequenceiq.cloudbreak.cloud.aws.scheduler.StackCancellationCheck;
import com.sequenceiq.cloudbreak.cloud.aws.util.AwsCloudFormationErrorMessageProvider;
import com.sequenceiq.cloudbreak.cloud.aws.view.AwsRdsInstanceView;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.exception.CloudConnectorException;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.CloudResourceStatus;
import com.sequenceiq.cloudbreak.cloud.model.DatabaseServer;
import com.sequenceiq.cloudbreak.cloud.model.DatabaseStack;
import com.sequenceiq.cloudbreak.cloud.model.ResourceStatus;
import com.sequenceiq.cloudbreak.cloud.notification.PersistenceNotifier;
import com.sequenceiq.common.api.type.ResourceType;

import software.amazon.awssdk.awscore.exception.AwsServiceException;
import software.amazon.awssdk.services.cloudformation.model.DescribeStacksRequest;
import software.amazon.awssdk.services.cloudformation.model.DescribeStacksResponse;
import software.amazon.awssdk.services.cloudformation.model.StackStatus;
import software.amazon.awssdk.services.cloudformation.waiters.CloudFormationWaiter;

@Service
public class AwsRdsLaunchService {

    @VisibleForTesting
    static final String HOSTNAME = "Hostname";

    @VisibleForTesting
    static final String PORT = "Port";

    @VisibleForTesting
    static final String CREATED_DB_INSTANCE = "CreatedDBInstance";

    @VisibleForTesting
    static final String CREATED_DB_SUBNET_GROUP = "CreatedDBSubnetGroup";

    @VisibleForTesting
    static final String CREATED_DB_PARAMETER_GROUP = "CreatedDBParameterGroup";

    private static final Logger LOGGER = LoggerFactory.getLogger(AwsRdsLaunchService.class);

    private static final String INSUFFICIENT_DB_INSTANCE_CAPACITY_LOWER = INSUFFICIENT_DB_INSTANCE_CAPACITY.toLowerCase(Locale.ROOT);

    private static final Set<StackStatus> FAILED_STACK_STATUSES = Set.of(
            StackStatus.CREATE_FAILED, StackStatus.ROLLBACK_COMPLETE, StackStatus.ROLLBACK_FAILED);

    @Inject
    private CloudFormationStackUtil cfStackUtil;

    @Inject
    private AwsCloudFormationClient awsClient;

    @Inject
    private CloudFormationTemplateBuilder cloudFormationTemplateBuilder;

    @Inject
    private AwsStackRequestHelper awsStackRequestHelper;

    @Inject
    private AwsCloudFormationErrorMessageProvider awsCloudFormationErrorMessageProvider;

    public List<CloudResourceStatus> launch(AuthenticatedContext ac, DatabaseStack stack, PersistenceNotifier resourceNotifier) {
        String cFStackName = cfStackUtil.getCfStackName(ac);
        AwsCredentialView credentialView = new AwsCredentialView(ac.getCloudCredential());
        String regionName = ac.getCloudContext().getLocation().getRegion().value();
        AmazonCloudFormationClient cfClient = awsClient.createCloudFormationClient(credentialView, regionName);
        AwsNetworkView awsNetworkView = new AwsNetworkView(stack.getNetwork());
        DescribeStacksRequest describeStacksRequest = DescribeStacksRequest.builder().stackName(cFStackName).build();
        DatabaseServer databaseServer = stack.getDatabaseServer();
        boolean useSslEnforcement = databaseServer.isUseSslEnforcement();

        DescribeStacksResponse existingStack = null;
        AwsServiceException describeException = null;
        try {
            existingStack = cfClient.describeStacks(describeStacksRequest);
        } catch (AwsServiceException exception) {
            describeException = exception;
        }

        if (describeException != null) {
            LOGGER.warn("API call failed with this error:", describeException);
            // all subnets desired for DB subnet group are in the stack
            if (!awsNetworkView.isExistingSubnet()) {
                throw new CloudConnectorException("Can only create RDS instance with existing subnets", describeException);
            }
            CloudResource cloudFormationStack = CloudResource.builder()
                    .withType(ResourceType.CLOUDFORMATION_STACK)
                    .withName(cFStackName)
                    .withAvailabilityZone(ac.getCloudContext().getLocation().getAvailabilityZone().value())
                    .build();
            resourceNotifier.notifyAllocation(cloudFormationStack, ac.getCloudContext());
            buildTemplateAndCreateStack(ac, stack, awsNetworkView, databaseServer, cFStackName, useSslEnforcement, cfClient);
        } else if (isFailedStack(existingStack)) {
            LOGGER.info("Existing RDS CloudFormation stack {} is in failed state '{}' from a previous attempt, deleting it before re-provisioning",
                    cFStackName, existingStack.stacks().get(0).stackStatusAsString());
            deleteRolledBackStack(ac, cFStackName, cfClient);
            buildTemplateAndCreateStack(ac, stack, awsNetworkView, databaseServer, cFStackName, useSslEnforcement, cfClient);
        } else {
            LOGGER.debug("Stack already exists: {}", cFStackName);
            waitForStackCreateComplete(ac, cFStackName, cfClient);
        }

        List<CloudResource> databaseResources = getCreatedOutputs(ac, stack, cFStackName, cfClient, resourceNotifier, useSslEnforcement);
        databaseResources.forEach(dbr -> resourceNotifier.notifyAllocation(dbr, ac.getCloudContext()));
        // FIXME: For now, just return everything wrapped in a status object
        return databaseResources.stream()
                .map(resource -> new CloudResourceStatus(resource, ResourceStatus.CREATED))
                .collect(Collectors.toList());
    }

    private void buildTemplateAndCreateStack(AuthenticatedContext ac, DatabaseStack stack, AwsNetworkView awsNetworkView, DatabaseServer databaseServer,
            String cFStackName, boolean useSslEnforcement, AmazonCloudFormationClient cfClient) {
        RDSModelContext rdsModelContext = new RDSModelContext()
                .withTemplate(stack.getTemplate())
                .withNetworkCidrs(awsNetworkView.getExistingVpcCidrs())
                .withHasPort(databaseServer.getPort() != null)
                .withUseSslEnforcement(useSslEnforcement)
                .withSslCertificateIdentifierDefined(new AwsRdsInstanceView(databaseServer).isSslCertificateIdentifierDefined())
                .withHasSecurityGroup(!databaseServer.getSecurity().getCloudSecurityIds().isEmpty())
                .withIsKmsCustom(databaseServer.getParameters().containsKey("key"))
                .withGetKmsKey(getKmsKey(stack));

        String cfTemplate = cloudFormationTemplateBuilder.build(rdsModelContext);
        LOGGER.debug("CloudFormationTemplate: {}", cfTemplate);
        createStackWithInstanceTypeFallback(ac, stack, cFStackName, cfTemplate, cfClient);
    }

    private void createStackWithInstanceTypeFallback(AuthenticatedContext ac, DatabaseStack stack, String cFStackName, String cfTemplate,
            AmazonCloudFormationClient cfClient) {
        List<String> instanceTypes = instanceTypeCandidates(stack.getDatabaseServer());
        for (int i = 0; i < instanceTypes.size(); i++) {
            String instanceType = instanceTypes.get(i);
            boolean lastCandidate = i == instanceTypes.size() - 1;
            DatabaseStack attemptStack = withInstanceType(stack, instanceType);
            LOGGER.debug("Sending RDS CloudFormation stack creation request '{}' for stack '{}' with instance type '{}' (attempt {}/{})",
                    cFStackName, ac.getCloudContext().getId(), instanceType, i + 1, instanceTypes.size());
            cfClient.createStack(awsStackRequestHelper.createCreateStackRequest(ac, attemptStack, cFStackName, cfTemplate));
            try {
                waitForStackCreationToComplete(ac, cFStackName, cfClient);
                LOGGER.info("RDS CloudFormation stack {} created with instance type '{}'", cFStackName, instanceType);
                return;
            } catch (Exception e) {
                Supplier<String> reasonSupplier = () -> awsCloudFormationErrorMessageProvider.getErrorReason(ac, cFStackName, CREATE_FAILED);
                String reason = resolveErrorReasonForClassification(cFStackName, reasonSupplier);
                if (!lastCandidate && isCapacityFailure(reason)) {
                    LOGGER.warn("RDS instance type '{}' could not be provisioned due to capacity shortage, falling back to '{}'. Reason: {}",
                            instanceType, instanceTypes.get(i + 1), reason);
                    deleteRolledBackStack(ac, cFStackName, cfClient);
                } else {
                    handleWaiterError(String.format("RDS CloudFormation stack %s creation failed", cFStackName), reasonSupplier, e);
                }
            }
        }
    }

    private void waitForStackCreateComplete(AuthenticatedContext ac, String cFStackName, AmazonCloudFormationClient cfClient) {
        try {
            waitForStackCreationToComplete(ac, cFStackName, cfClient);
        } catch (Exception e) {
            handleWaiterError(String.format("RDS CloudFormation stack %s creation failed", cFStackName),
                    () -> awsCloudFormationErrorMessageProvider.getErrorReason(ac, cFStackName, CREATE_FAILED), e);
        }
    }

    private void waitForStackCreationToComplete(AuthenticatedContext ac, String cFStackName, AmazonCloudFormationClient cfClient) {
        DescribeStacksRequest request = DescribeStacksRequest.builder().stackName(cFStackName).build();
        StackCancellationCheck cancellationCheck = new StackCancellationCheck(ac.getCloudContext().getId());
        try (CloudFormationWaiter waiter = cfClient.waiters()) {
            LOGGER.debug("Waiting for RDS CloudFormation stack {} creation", cFStackName);
            waiter.waitUntilStackCreateComplete(request, CancellableWaiterConfiguration.cancellableWaiterConfiguration(cancellationCheck));
        }
    }

    private boolean isFailedStack(DescribeStacksResponse describeStacksResponse) {
        return describeStacksResponse.hasStacks() && !describeStacksResponse.stacks().isEmpty()
                && FAILED_STACK_STATUSES.contains(describeStacksResponse.stacks().get(0).stackStatus());
    }

    private void deleteRolledBackStack(AuthenticatedContext ac, String cFStackName, AmazonCloudFormationClient cfClient) {
        LOGGER.debug("Deleting rolled-back RDS CloudFormation stack {} before instance type fallback", cFStackName);
        cfClient.deleteStack(awsStackRequestHelper.createDeleteStackRequest(cFStackName));
        DescribeStacksRequest request = DescribeStacksRequest.builder().stackName(cFStackName).build();
        StackCancellationCheck cancellationCheck = new StackCancellationCheck(ac.getCloudContext().getId());
        try (CloudFormationWaiter waiter = cfClient.waiters()) {
            waiter.waitUntilStackDeleteComplete(request, CancellableWaiterConfiguration.cancellableWaiterConfiguration(cancellationCheck));
        } catch (Exception e) {
            handleWaiterError(String.format("Deletion of failed RDS CloudFormation stack %s before instance type fallback failed", cFStackName), e);
        }
    }

    private List<String> instanceTypeCandidates(DatabaseServer databaseServer) {
        List<String> instanceTypes = new ArrayList<>();
        instanceTypes.add(databaseServer.getFlavor());
        for (String fallback : databaseServer.getFallbackInstanceTypes()) {
            if (StringUtils.hasText(fallback) && !instanceTypes.contains(fallback)) {
                instanceTypes.add(fallback);
            }
        }
        return instanceTypes;
    }

    private DatabaseStack withInstanceType(DatabaseStack stack, String instanceType) {
        DatabaseServer databaseServer = stack.getDatabaseServer();
        if (Objects.equals(databaseServer.getFlavor(), instanceType)) {
            return stack;
        }
        DatabaseServer patchedServer = DatabaseServer.builder(databaseServer).withFlavor(instanceType).build();
        return new DatabaseStack(stack.getNetwork(), patchedServer, stack.getTags(), stack.getTemplate(), stack.getDeploymentType());
    }

    private boolean isCapacityFailure(String reason) {
        if (!StringUtils.hasText(reason)) {
            return false;
        }
        return reason.toLowerCase(Locale.ROOT).contains(INSUFFICIENT_DB_INSTANCE_CAPACITY_LOWER);
    }

    private String resolveErrorReasonForClassification(String cFStackName, Supplier<String> reasonSupplier) {
        try {
            return reasonSupplier.get();
        } catch (Exception ex) {
            LOGGER.warn("Failed to resolve the failure reason for RDS CloudFormation stack {} while classifying capacity failure", cFStackName, ex);
            return "";
        }
    }

    private List<CloudResource> getCreatedOutputs(AuthenticatedContext ac, DatabaseStack stack, String cFStackName, AmazonCloudFormationClient client,
            PersistenceNotifier resourceNotifier, boolean useSslEnforcement) {
        List<CloudResource> resources = new ArrayList<>();

        Map<String, String> outputs = getCfStackOutputs(cFStackName, client);
        String availabilityZone = ac.getCloudContext().getLocation().getAvailabilityZone().value();

        resources.add(CloudResource.builder()
                .withType(ResourceType.RDS_HOSTNAME)
                .withName(getHostname(outputs, cFStackName))
                .withAvailabilityZone(availabilityZone)
                .build());
        resources.add(CloudResource.builder()
                .withType(ResourceType.RDS_PORT)
                .withName(getPort(outputs, cFStackName))
                .withAvailabilityZone(availabilityZone)
                .build());
        resources.add(CloudResource.builder()
                .withType(ResourceType.RDS_INSTANCE)
                .withName(getCreatedDBInstance(outputs, cFStackName))
                .withAvailabilityZone(availabilityZone)
                .build());
        resources.add(CloudResource.builder()
                .withType(ResourceType.RDS_DB_SUBNET_GROUP)
                .withName(getCreatedDBSubnetGroup(outputs, cFStackName))
                .withAvailabilityZone(availabilityZone)
                .build());
        if (useSslEnforcement) {
            resources.add(CloudResource.builder()
                    .withType(ResourceType.RDS_DB_PARAMETER_GROUP)
                    .withName(getCreatedDBParameterGroup(outputs, cFStackName))
                    .withAvailabilityZone(availabilityZone)
                    .build());
        }
        // The idea here is to record the CloudFormation stack name so that we can later manipulate it.
        // This may be unnecessary, but for now this is trivial to add.
        CloudResource cfNameResource = CloudResource.builder()
                .withType(ResourceType.CLOUDFORMATION_STACK)
                .withName(cFStackName)
                .withAvailabilityZone(availabilityZone)
                .build();
        resources.add(cfNameResource);

        return resources;
    }

    private String getHostname(Map<String, String> outputs, String cFStackName) {
        return getOutput(outputs, HOSTNAME, "DB hostname", cFStackName);
    }

    private String getPort(Map<String, String> outputs, String cFStackName) {
        return getOutput(outputs, PORT, "DB port", cFStackName);
    }

    private String getCreatedDBInstance(Map<String, String> outputs, String cFStackName) {
        return getOutput(outputs, CREATED_DB_INSTANCE, "DB instance", cFStackName);
    }

    private String getCreatedDBSubnetGroup(Map<String, String> outputs, String cFStackName) {
        return getOutput(outputs, CREATED_DB_SUBNET_GROUP, "DB subnet group", cFStackName);
    }

    private String getCreatedDBParameterGroup(Map<String, String> outputs, String cFStackName) {
        return getOutput(outputs, CREATED_DB_PARAMETER_GROUP, "DB parameter group", cFStackName);
    }

    private String getOutput(Map<String, String> outputs, String key, String friendlyName, String cFStackName) {
        if (outputs.containsKey(key)) {
            return outputs.get(key);
        } else {
            String outputKeyNotFound = String.format(friendlyName + " could not be found in the CloudFormation stack('%s') output.", cFStackName);
            throw new CloudConnectorException(outputKeyNotFound);
        }
    }

    private Map<String, String> getCfStackOutputs(String cFStackName, AmazonCloudFormationClient client) {
        return cfStackUtil.getOutputs(cFStackName, client);
    }

    private String getKmsKey(DatabaseStack stack) {
        if (stack.getDatabaseServer().getParameters().containsKey("key")) {
            return stack.getDatabaseServer().getParameters().get("key").toString();
        } else {
            return null;
        }
    }

}
