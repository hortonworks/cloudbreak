package com.sequenceiq.cloudbreak.cloud.aws.connector.resource;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.services.cloudformation.AmazonCloudFormationClient;
import com.amazonaws.services.cloudformation.model.DescribeStacksRequest;
import com.sequenceiq.cloudbreak.cloud.aws.AwsClient;
import com.sequenceiq.cloudbreak.cloud.aws.AwsStackRequestHelper;
import com.sequenceiq.cloudbreak.cloud.aws.CloudFormationStackUtil;
import com.sequenceiq.cloudbreak.cloud.aws.CloudFormationTemplateBuilder;
import com.sequenceiq.cloudbreak.cloud.aws.CloudFormationTemplateBuilder.RDSModelContext;
import com.sequenceiq.cloudbreak.cloud.aws.client.AmazonCloudFormationRetryClient;
import com.sequenceiq.cloudbreak.cloud.aws.scheduler.AwsBackoffSyncPollingScheduler;
import com.sequenceiq.cloudbreak.cloud.aws.task.AwsPollTaskFactory;
import com.sequenceiq.cloudbreak.cloud.aws.view.AwsCredentialView;
import com.sequenceiq.cloudbreak.cloud.aws.view.AwsNetworkView;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.exception.CloudConnectorException;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource.Builder;
import com.sequenceiq.cloudbreak.cloud.model.CloudResourceStatus;
import com.sequenceiq.cloudbreak.cloud.model.DatabaseStack;
import com.sequenceiq.cloudbreak.cloud.model.ResourceStatus;
import com.sequenceiq.cloudbreak.cloud.notification.PersistenceNotifier;
import com.sequenceiq.cloudbreak.cloud.task.PollTask;
import com.sequenceiq.common.api.type.ResourceType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.amazonaws.services.cloudformation.model.StackStatus.CREATE_COMPLETE;
import static com.amazonaws.services.cloudformation.model.StackStatus.CREATE_FAILED;
import static com.sequenceiq.cloudbreak.cloud.aws.connector.resource.AwsResourceConstants.ERROR_STATUSES;

@Service
public class AwsRdsLaunchService {

    private static final Logger LOGGER = LoggerFactory.getLogger(AwsRdsLaunchService.class);

    private static final String HOSTNAME = "Hostname";

    private static final String PORT = "Port";

    private static final String CREATED_DB_INSTANCE = "CreatedDBInstance";

    private static final String CREATED_DB_SUBNET_GROUP = "CreatedDBSubnetGroup";

    @Inject
    private CloudFormationStackUtil cfStackUtil;

    @Inject
    private AwsClient awsClient;

    @Inject
    private AwsBackoffSyncPollingScheduler<Boolean> awsBackoffSyncPollingScheduler;

    @Inject
    private CloudFormationTemplateBuilder cloudFormationTemplateBuilder;

    @Inject
    private AwsPollTaskFactory awsPollTaskFactory;

    @Inject
    private AwsStackRequestHelper awsStackRequestHelper;

    public List<CloudResourceStatus> launch(AuthenticatedContext ac, DatabaseStack stack, PersistenceNotifier resourceNotifier)
            throws Exception {
        String cFStackName = cfStackUtil.getCfStackName(ac);
        AwsCredentialView credentialView = new AwsCredentialView(ac.getCloudCredential());
        String regionName = ac.getCloudContext().getLocation().getRegion().value();
        AmazonCloudFormationRetryClient cfRetryClient = awsClient.createCloudFormationRetryClient(credentialView, regionName);
        AwsNetworkView awsNetworkView = new AwsNetworkView(stack.getNetwork());
        try {
            cfRetryClient.describeStacks(new DescribeStacksRequest().withStackName(cFStackName));
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
                    .withHasPort(stack.getDatabaseServer().getPort() != null)
                    .withHasSecurityGroup(!stack.getDatabaseServer().getSecurity().getCloudSecurityIds().isEmpty());
            String cfTemplate = cloudFormationTemplateBuilder.build(rdsModelContext);
            LOGGER.debug("CloudFormationTemplate: {}", cfTemplate);
            cfRetryClient.createStack(awsStackRequestHelper.createCreateStackRequest(ac, stack, cFStackName, cfTemplate));
        }
        LOGGER.debug("CloudFormation stack creation request sent with stack name: '{}' for stack: '{}'", cFStackName, ac.getCloudContext().getId());

        AmazonCloudFormationClient cfClient = awsClient.createCloudFormationClient(credentialView, regionName);
        // autoscaling client is unused by task!
        PollTask<Boolean> task = awsPollTaskFactory.newAwsCreateStackStatusCheckerTask(ac, cfClient, null, CREATE_COMPLETE, CREATE_FAILED, ERROR_STATUSES,
                cFStackName);
        try {
            awsBackoffSyncPollingScheduler.schedule(task);
        } catch (RuntimeException e) {
            throw new CloudConnectorException(e.getMessage(), e);
        }

        List<CloudResource> databaseResources = getCreatedOutputs(ac, stack, cFStackName, cfRetryClient, resourceNotifier);
        databaseResources.forEach(dbr -> resourceNotifier.notifyAllocation(dbr, ac.getCloudContext()));
        // FIXME: For now, just return everything wrapped in a status object
        return databaseResources.stream()
                .map(resource -> new CloudResourceStatus(resource, ResourceStatus.CREATED))
                .collect(Collectors.toList());
        // FIXME check does nothing?!
        //return awsResourceConnector.check(ac, databaseResources);
    }

    private List<CloudResource> getCreatedOutputs(AuthenticatedContext ac, DatabaseStack stack, String cFStackName, AmazonCloudFormationRetryClient client,
            PersistenceNotifier resourceNotifier) {
        List<CloudResource> resources = new ArrayList<>();

        Map<String, String> outputs = getCfStackOutputs(cFStackName, client);

        resources.add(new Builder().type(ResourceType.RDS_HOSTNAME).name(getHostname(outputs, cFStackName)).build());
        resources.add(new Builder().type(ResourceType.RDS_PORT).name(getPort(outputs, cFStackName)).build());
        resources.add(new Builder().type(ResourceType.RDS_INSTANCE).name(getCreatedDBInstance(outputs, cFStackName)).build());
        resources.add(new Builder().type(ResourceType.RDS_DB_SUBNET_GROUP).name(getCreatedDBSubnetGroup(outputs, cFStackName)).build());
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

    private String getOutput(Map<String, String> outputs, String key, String friendlyName, String cFStackName) {
        if (outputs.containsKey(key)) {
            return outputs.get(key);
        } else {
            String outputKeyNotFound = String.format(friendlyName + " could not be found in the Cloudformation stack('%s') output.", cFStackName);
            throw new CloudConnectorException(outputKeyNotFound);
        }
    }

    private Map<String, String> getCfStackOutputs(String cFStackName, AmazonCloudFormationRetryClient client) {
        return cfStackUtil.getOutputs(cFStackName, client);
    }
}
