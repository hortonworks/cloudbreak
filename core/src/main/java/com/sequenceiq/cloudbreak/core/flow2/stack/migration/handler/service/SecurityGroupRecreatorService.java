package com.sequenceiq.cloudbreak.core.flow2.stack.migration.handler.service;

import static com.sequenceiq.cloudbreak.common.exception.NotFoundException.notFound;
import static org.slf4j.LoggerFactory.getLogger;

import java.util.List;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.aws.common.context.AwsContext;
import com.sequenceiq.cloudbreak.cloud.aws.resource.group.AwsSecurityGroupResourceBuilder;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.event.resource.migration.aws.CreateResourcesRequest;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.CloudStack;
import com.sequenceiq.cloudbreak.cloud.model.Group;
import com.sequenceiq.cloudbreak.cloud.model.Network;
import com.sequenceiq.cloudbreak.cloud.notification.PersistenceNotifier;
import com.sequenceiq.cloudbreak.cloud.notification.model.ResourcePersisted;

@Component
public class SecurityGroupRecreatorService implements ResourceRecreator {

    private static final Logger LOGGER = getLogger(SecurityGroupRecreatorService.class);

    @Inject
    private AwsSecurityGroupResourceBuilder awsSecurityGroupResourceBuilder;

    @Inject
    private PersistenceNotifier persistenceNotifier;

    @Override
    public void recreate(CreateResourcesRequest request, AwsContext awsContext, AuthenticatedContext ac) throws Exception {
        CloudContext cloudContext = request.getCloudContext();
        CloudStack cloudStack = request.getCloudStack();
        Network network = cloudStack.getNetwork();
        String hostGroupName = request.getHostGroupName();
        Group group = getGroupByName(cloudStack.getGroups(), hostGroupName);
        CloudResource createdSecurityGroup = awsSecurityGroupResourceBuilder.create(awsContext, ac, group, network);
        if (createdSecurityGroup != null) {
            createdSecurityGroup = CloudResource.builder().cloudResource(createdSecurityGroup).withGroup(group.getName()).build();
            ResourcePersisted resourcePersisted = persistenceNotifier.notifyAllocation(createdSecurityGroup, cloudContext);
            if (resourcePersisted.getException() != null) {
                LOGGER.error("Cannot create security group resource in the db: {}", resourcePersisted.getStatusReason(), resourcePersisted.getException());
                throw resourcePersisted.getException();
            }
            CloudResource existedSecGroup = awsSecurityGroupResourceBuilder.build(awsContext, ac, group, network, group.getSecurity(), createdSecurityGroup);
            persistenceNotifier.notifyUpdate(existedSecGroup, cloudContext);
            LOGGER.info("AWS security group successfully created");
        } else {
            LOGGER.info("Security group already exists for group: '{}', no need for recreation", hostGroupName);
        }
    }

    private Group getGroupByName(List<Group> groups, String hostGroupName) {
        return groups.stream().filter(g -> hostGroupName.equals(g.getName())).findFirst().orElseThrow(notFound("hostGroup", hostGroupName));
    }
}
