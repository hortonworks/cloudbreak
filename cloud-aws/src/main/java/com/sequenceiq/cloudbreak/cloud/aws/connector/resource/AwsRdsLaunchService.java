package com.sequenceiq.cloudbreak.cloud.aws.connector.resource;

import static com.amazonaws.services.cloudformation.model.StackStatus.CREATE_COMPLETE;
import static com.amazonaws.services.cloudformation.model.StackStatus.CREATE_FAILED;
import static com.sequenceiq.cloudbreak.cloud.aws.connector.resource.AwsResourceConstants.ERROR_STATUSES;

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

    @Inject
    private AwsResourceConnector awsResourceConnector;

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
                    .withHasPort(stack.getDatabaseServer().getPort() != null);
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

        String hostname = getHostname(cFStackName, client);
        CloudResource hostnameResource = new Builder().type(ResourceType.RDS_HOSTNAME).name(hostname).build();
        resourceNotifier.notifyAllocation(hostnameResource, ac.getCloudContext());
        resources.add(hostnameResource);

        String port = getPort(cFStackName, client);
        CloudResource portResource = new Builder().type(ResourceType.RDS_PORT).name(port).build();
        resourceNotifier.notifyAllocation(portResource, ac.getCloudContext());
        resources.add(portResource);

        String dbInstanceId = getCreatedDBInstance(cFStackName, client);
        CloudResource dbInstance = new Builder().type(ResourceType.RDS_INSTANCE).name(dbInstanceId).build();
        resourceNotifier.notifyAllocation(dbInstance, ac.getCloudContext());
        resources.add(dbInstance);

        String dbSubnetGroupId = getCreatedDBSubnetGroup(cFStackName, client);
        CloudResource dbSubnetGroup = new Builder().type(ResourceType.RDS_DB_SUBNET_GROUP).name(dbSubnetGroupId).build();
        resourceNotifier.notifyAllocation(dbSubnetGroup, ac.getCloudContext());
        resources.add(dbSubnetGroup);

        // The idea here is to record the CloudFormation stack name so that we can later manipulate it.
        // This may be unnecessary, but for now this is trivial to add.
        CloudResource cfNameResource = new Builder().type(ResourceType.CLOUDFORMATION_STACK).name(cFStackName).build();
        resources.add(cfNameResource);

        return resources;
    }

    private String getHostname(String cFStackName, AmazonCloudFormationRetryClient client) {
        return getOutput(cFStackName, HOSTNAME, client, "DB hostname");
    }

    private String getPort(String cFStackName, AmazonCloudFormationRetryClient client) {
        return getOutput(cFStackName, PORT, client, "DB port");
    }

    private String getCreatedDBInstance(String cFStackName, AmazonCloudFormationRetryClient client) {
        return getOutput(cFStackName, CREATED_DB_INSTANCE, client, "DB instance");
    }

    private String getCreatedDBSubnetGroup(String cFStackName, AmazonCloudFormationRetryClient client) {
        return getOutput(cFStackName, CREATED_DB_SUBNET_GROUP, client, "DB subnet group");
    }

    private String getOutput(String cFStackName, String key, AmazonCloudFormationRetryClient client, String friendlyName) {
        Map<String, String> outputs = cfStackUtil.getOutputs(cFStackName, client);
        if (outputs.containsKey(key)) {
            return outputs.get(key);
        } else {
            String outputKeyNotFound = String.format(friendlyName + " could not be found in the Cloudformation stack('%s') output.", cFStackName);
            throw new CloudConnectorException(outputKeyNotFound);
        }
    }
}
