package com.sequenceiq.cloudbreak.cloud.aws.connector.resource;

import static com.sequenceiq.cloudbreak.cloud.aws.scheduler.WaiterRunner.run;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.services.cloudformation.AmazonCloudFormationClient;
import com.amazonaws.services.cloudformation.model.DescribeStacksRequest;
import com.amazonaws.waiters.Waiter;
import com.sequenceiq.cloudbreak.cloud.aws.AwsClient;
import com.sequenceiq.cloudbreak.cloud.aws.AwsStackRequestHelper;
import com.sequenceiq.cloudbreak.cloud.aws.CloudFormationStackUtil;
import com.sequenceiq.cloudbreak.cloud.aws.CloudFormationTemplateBuilder;
import com.sequenceiq.cloudbreak.cloud.aws.scheduler.StackCancellationCheck;
import com.sequenceiq.cloudbreak.cloud.aws.view.AwsCredentialView;
import com.sequenceiq.cloudbreak.cloud.aws.view.AwsNetworkView;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.exception.CloudConnectorException;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.DatabaseStack;
import com.sequenceiq.cloudbreak.cloud.notification.PersistenceNotifier;
import com.sequenceiq.common.api.type.ResourceType;

public class AwsRdsUpdateService {
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
    private CloudFormationTemplateBuilder cloudFormationTemplateBuilder;

    @Inject
    private AwsStackRequestHelper awsStackRequestHelper;

    public void update(AuthenticatedContext ac, DatabaseStack stack, PersistenceNotifier resourceNotifier)
            throws Exception {
        String cFStackName = cfStackUtil.getCfStackName(ac);
        AwsCredentialView credentialView = new AwsCredentialView(ac.getCloudCredential());
        String regionName = ac.getCloudContext().getLocation().getRegion().value();
        AmazonCloudFormationClient cloudFormationClient = awsClient.createCloudFormationClient(credentialView, regionName);
        AwsNetworkView awsNetworkView = new AwsNetworkView(stack.getNetwork());
        DescribeStacksRequest describeStacksRequest = new DescribeStacksRequest().withStackName(cFStackName);
        try {
            cloudFormationClient.describeStacks(describeStacksRequest);
            LOGGER.debug("Stack exists: {}, will update it", cFStackName);
            // all subnets desired for DB subnet group are in the stack
            boolean existingSubnet = awsNetworkView.isExistingSubnet();
            if (!existingSubnet) {
                throw new CloudConnectorException("Can only create RDS instance with existing subnets");
            }
            CloudResource cloudFormationStack = new CloudResource.Builder().type(ResourceType.CLOUDFORMATION_STACK).name(cFStackName).build();
            resourceNotifier.notifyAllocation(cloudFormationStack, ac.getCloudContext());

            CloudFormationTemplateBuilder.RDSModelContext rdsModelContext = new CloudFormationTemplateBuilder.RDSModelContext()
                    .withTemplate(stack.getTemplate())
                    .withNetworkCidrs(awsNetworkView.getExistingVpcCidrs())
                    .withHasPort(stack.getDatabaseServer().getPort() != null)
                    .withHasSecurityGroup(!stack.getDatabaseServer().getSecurity().getCloudSecurityIds().isEmpty());
            String cfTemplate = cloudFormationTemplateBuilder.build(rdsModelContext);
            LOGGER.debug("CloudFormationTemplate: {}", cfTemplate);
            cloudFormationClient.updateStack(awsStackRequestHelper.createUpdateStackRequestDisablingDeleteProtection(ac, stack, cFStackName, cfTemplate));

        } catch (AmazonServiceException ignored) {
            throw new CloudConnectorException("Cloudformation stack not found");
        }
        LOGGER.debug("CloudFormation stack update request sent with stack name: '{}' for stack: '{}'", cFStackName, ac.getCloudContext().getId());

        AmazonCloudFormationClient cfClient = awsClient.createCloudFormationClient(credentialView, regionName);
        Waiter<DescribeStacksRequest> updateWaiter = cfClient.waiters().stackUpdateComplete();
        StackCancellationCheck stackCancellationCheck = new StackCancellationCheck(ac.getCloudContext().getId());
        run(updateWaiter, describeStacksRequest,
                stackCancellationCheck);
    }
}
