package com.sequenceiq.cloudbreak.cloud.aws.connector.resource;

import static com.amazonaws.services.cloudformation.model.ResourceStatus.CREATE_FAILED;
import static com.sequenceiq.cloudbreak.cloud.aws.scheduler.WaiterRunner.run;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.services.cloudformation.AmazonCloudFormationClient;
import com.amazonaws.services.cloudformation.model.DescribeStacksRequest;
import com.amazonaws.waiters.Waiter;
import com.google.common.annotations.VisibleForTesting;
import com.sequenceiq.cloudbreak.cloud.aws.AwsClient;
import com.sequenceiq.cloudbreak.cloud.aws.AwsStackRequestHelper;
import com.sequenceiq.cloudbreak.cloud.aws.CloudFormationStackUtil;
import com.sequenceiq.cloudbreak.cloud.aws.CloudFormationTemplateBuilder;
import com.sequenceiq.cloudbreak.cloud.aws.CloudFormationTemplateBuilder.RDSModelContext;
import com.sequenceiq.cloudbreak.cloud.aws.client.AmazonCloudFormationRetryClient;
import com.sequenceiq.cloudbreak.cloud.aws.scheduler.StackCancellationCheck;
import com.sequenceiq.cloudbreak.cloud.aws.util.AwsCloudFormationErrorMessageProvider;
import com.sequenceiq.cloudbreak.cloud.aws.view.AwsCredentialView;
import com.sequenceiq.cloudbreak.cloud.aws.view.AwsNetworkView;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.exception.CloudConnectorException;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource.Builder;
import com.sequenceiq.cloudbreak.cloud.model.CloudResourceStatus;
import com.sequenceiq.cloudbreak.cloud.model.DatabaseServer;
import com.sequenceiq.cloudbreak.cloud.model.DatabaseStack;
import com.sequenceiq.cloudbreak.cloud.model.ResourceStatus;
import com.sequenceiq.cloudbreak.cloud.notification.PersistenceNotifier;
import com.sequenceiq.common.api.type.ResourceType;

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

    @Inject
    private CloudFormationStackUtil cfStackUtil;

    @Inject
    private AwsClient awsClient;

    @Inject
    private CloudFormationTemplateBuilder cloudFormationTemplateBuilder;

    @Inject
    private AwsStackRequestHelper awsStackRequestHelper;

    public List<CloudResourceStatus> launch(AuthenticatedContext ac, DatabaseStack stack, PersistenceNotifier resourceNotifier)
            throws Exception {
        String cFStackName = cfStackUtil.getCfStackName(ac);
        AwsCredentialView credentialView = new AwsCredentialView(ac.getCloudCredential());
        String regionName = ac.getCloudContext().getLocation().getRegion().value();
        AmazonCloudFormationRetryClient cfRetryClient = awsClient.createCloudFormationRetryClient(credentialView, regionName);
        AwsNetworkView awsNetworkView = new AwsNetworkView(stack.getNetwork());
        DescribeStacksRequest describeStacksRequest = new DescribeStacksRequest().withStackName(cFStackName);
        DatabaseServer databaseServer = stack.getDatabaseServer();
        boolean useSslEnforcement = databaseServer.isUseSslEnforcement();
        try {
            cfRetryClient.describeStacks(describeStacksRequest);
            LOGGER.debug("Stack already exists: {}", cFStackName);
        } catch (AmazonServiceException ignored) {
            // all subnets desired for DB subnet group are in the stack
            boolean existingSubnet = awsNetworkView.isExistingSubnet();
            if (!existingSubnet) {
                throw new CloudConnectorException("Can only create RDS instance with existing subnets");
            }
            CloudResource cloudFormationStack = new Builder().type(ResourceType.CLOUDFORMATION_STACK).name(cFStackName).build();
            resourceNotifier.notifyAllocation(cloudFormationStack, ac.getCloudContext());

            RDSModelContext rdsModelContext = new RDSModelContext()
                    .withTemplate(stack.getTemplate())
                    .withNetworkCidrs(awsNetworkView.getExistingVpcCidrs())
                    .withHasPort(databaseServer.getPort() != null)
                    .withUseSslEnforcement(useSslEnforcement)
                    .withHasSecurityGroup(!databaseServer.getSecurity().getCloudSecurityIds().isEmpty());
            String cfTemplate = cloudFormationTemplateBuilder.build(rdsModelContext);
            LOGGER.debug("CloudFormationTemplate: {}", cfTemplate);
            cfRetryClient.createStack(awsStackRequestHelper.createCreateStackRequest(ac, stack, cFStackName, cfTemplate));
        }
        LOGGER.debug("CloudFormation stack creation request sent with stack name: '{}' for stack: '{}'", cFStackName, ac.getCloudContext().getId());

        AmazonCloudFormationClient cfClient = awsClient.createCloudFormationClient(credentialView, regionName);
        Waiter<DescribeStacksRequest> creationWaiter = cfClient.waiters().stackCreateComplete();
        StackCancellationCheck stackCancellationCheck = new StackCancellationCheck(ac.getCloudContext().getId());
        run(creationWaiter, describeStacksRequest, stackCancellationCheck, String.format("RDS CloudFormation stack %s creation failed", cFStackName),
                () -> AwsCloudFormationErrorMessageProvider.getErrorReason(cfRetryClient, cFStackName, CREATE_FAILED));

        List<CloudResource> databaseResources = getCreatedOutputs(ac, stack, cFStackName, cfRetryClient, resourceNotifier, useSslEnforcement);
        databaseResources.forEach(dbr -> resourceNotifier.notifyAllocation(dbr, ac.getCloudContext()));
        // FIXME: For now, just return everything wrapped in a status object
        return databaseResources.stream()
                .map(resource -> new CloudResourceStatus(resource, ResourceStatus.CREATED))
                .collect(Collectors.toList());
        // FIXME check does nothing?!
        //return awsResourceConnector.check(ac, databaseResources);
    }

    private List<CloudResource> getCreatedOutputs(AuthenticatedContext ac, DatabaseStack stack, String cFStackName, AmazonCloudFormationRetryClient client,
            PersistenceNotifier resourceNotifier, boolean useSslEnforcement) {
        List<CloudResource> resources = new ArrayList<>();

        Map<String, String> outputs = getCfStackOutputs(cFStackName, client);

        resources.add(new Builder().type(ResourceType.RDS_HOSTNAME).name(getHostname(outputs, cFStackName)).build());
        resources.add(new Builder().type(ResourceType.RDS_PORT).name(getPort(outputs, cFStackName)).build());
        resources.add(new Builder().type(ResourceType.RDS_INSTANCE).name(getCreatedDBInstance(outputs, cFStackName)).build());
        resources.add(new Builder().type(ResourceType.RDS_DB_SUBNET_GROUP).name(getCreatedDBSubnetGroup(outputs, cFStackName)).build());
        if (useSslEnforcement) {
            resources.add(new Builder().type(ResourceType.RDS_DB_PARAMETER_GROUP).name(getCreatedDBParameterGroup(outputs, cFStackName)).build());
        }
        // The idea here is to record the CloudFormation stack name so that we can later manipulate it.
        // This may be unnecessary, but for now this is trivial to add.
        CloudResource cfNameResource = new Builder().type(ResourceType.CLOUDFORMATION_STACK).name(cFStackName).build();
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

    private Map<String, String> getCfStackOutputs(String cFStackName, AmazonCloudFormationRetryClient client) {
        return cfStackUtil.getOutputs(cFStackName, client);
    }

}
