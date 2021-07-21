package com.sequenceiq.cloudbreak.cloud.aws.resource.instance;

import static java.util.Collections.singletonList;
import static org.slf4j.LoggerFactory.getLogger;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.springframework.stereotype.Service;

import com.amazonaws.services.ec2.model.AllocateAddressRequest;
import com.amazonaws.services.ec2.model.AllocateAddressResult;
import com.amazonaws.services.ec2.model.AssociateAddressResult;
import com.amazonaws.services.ec2.model.DisassociateAddressRequest;
import com.amazonaws.services.ec2.model.DomainType;
import com.amazonaws.services.ec2.model.ReleaseAddressRequest;
import com.amazonaws.services.ec2.model.ReleaseAddressResult;
import com.amazonaws.services.ec2.model.Tag;
import com.amazonaws.services.ec2.model.TagSpecification;
import com.sequenceiq.cloudbreak.cloud.aws.common.AwsTaggingService;
import com.sequenceiq.cloudbreak.cloud.aws.common.client.AmazonEc2Client;
import com.sequenceiq.cloudbreak.cloud.aws.common.connector.resource.AwsElasticIpService;
import com.sequenceiq.cloudbreak.cloud.aws.common.connector.resource.AwsNetworkService;
import com.sequenceiq.cloudbreak.cloud.aws.common.context.AwsContext;
import com.sequenceiq.cloudbreak.cloud.aws.common.util.AwsMethodExecutor;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.model.CloudInstance;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.CloudStack;
import com.sequenceiq.cloudbreak.cloud.model.EIpAttributes;
import com.sequenceiq.cloudbreak.cloud.model.Group;
import com.sequenceiq.cloudbreak.cloud.model.Image;
import com.sequenceiq.cloudbreak.cloud.notification.PersistenceRetriever;
import com.sequenceiq.common.api.type.CommonStatus;
import com.sequenceiq.common.api.type.InstanceGroupType;
import com.sequenceiq.common.api.type.ResourceType;

@Service
public class AwsNativeEIPResourceBuilder extends AbstractAwsNativeComputeBuilder {

    private static final Logger LOGGER = getLogger(AwsNativeEIPResourceBuilder.class);

    private static final int ORDER = 4;

    @Inject
    private AwsTaggingService awsTaggingService;

    @Inject
    private AwsMethodExecutor awsMethodExecutor;

    @Inject
    private AwsNetworkService awsNetworkService;

    @Inject
    private AwsElasticIpService awsElasticIpService;

    @Inject
    private PersistenceRetriever persistenceRetriever;

    @Override
    public List<CloudResource> create(AwsContext context, CloudInstance instance, long privateId, AuthenticatedContext auth, Group group, Image image) {
        List<CloudResource> resources = new ArrayList<>();
        if (InstanceGroupType.GATEWAY.equals(group.getType())) {
            CloudContext cloudContext = auth.getCloudContext();
            AmazonEc2Client amazonEC2Client = context.getAmazonEc2Client();

            boolean mapPublicIpOnLaunch = awsNetworkService.isMapPublicOnLaunch(List.of(instance.getSubnetId()), amazonEC2Client);
            if (mapPublicIpOnLaunch) {
                String resourceName = getResourceNameService().resourceName(resourceType(), cloudContext.getName(), group.getName(), privateId);
                return singletonList(CloudResource.builder()
                        .group(group.getName())
                        .type(resourceType())
                        .status(CommonStatus.CREATED)
                        .name(resourceName)
                        .persistent(true)
                        .reference(String.valueOf(privateId))
                        .build());
            } else {
                LOGGER.debug("EIp doesn't need because no public ip on launch");
            }
        }
        return resources;
    }

    @Override
    public List<CloudResource> build(AwsContext context, CloudInstance cloudInstance, long privateId, AuthenticatedContext ac,
            Group group, List<CloudResource> buildableResource, CloudStack cloudStack) throws Exception {
        List<CloudResource> ret = new ArrayList<>();
        if (!buildableResource.isEmpty()) {
            LOGGER.info("Trying to create EIp for {}, resource name: {}", cloudInstance.getInstanceId(), buildableResource.get(0).getName());
            AmazonEc2Client amazonEC2Client = context.getAmazonEc2Client();
            TagSpecification tagSpecification = awsTaggingService.prepareEc2TagSpecification(
                    cloudStack.getTags(),
                    com.amazonaws.services.ec2.model.ResourceType.ElasticIp);
            tagSpecification.getTags().add(new Tag().withKey("Name").withValue(buildableResource.get(0).getName()));

            AllocateAddressRequest allocateAddressRequest = new AllocateAddressRequest()
                    .withTagSpecifications(tagSpecification)
                    .withDomain(DomainType.Vpc);
            AllocateAddressResult allocateAddressResult = amazonEC2Client
                    .allocateAddress(allocateAddressRequest);

            Optional<CloudResource> instanceResourceOpt = persistenceRetriever.notifyRetrieve(ac.getCloudContext().getId(), String.valueOf(privateId),
                    CommonStatus.CREATED, ResourceType.AWS_INSTANCE);
            CloudResource instanceResource = instanceResourceOpt.orElseThrow();
            List<String> eips = List.of(allocateAddressResult.getAllocationId());
            List<AssociateAddressResult> associateAddressResults = awsElasticIpService.associateElasticIpsToInstances(
                    amazonEC2Client,
                    eips,
                    List.of(instanceResource.getInstanceId()));

            CloudResource cloudResource = CloudResource.builder()
                    .cloudResource(buildableResource.get(0))
                    .persistent(true)
                    .instanceId(instanceResource.getInstanceId())
                    .reference(allocateAddressResult.getAllocationId())
                    .params(Map.of(CloudResource.ATTRIBUTES, EIpAttributes.EIpAttributesBuilder.builder()
                            .withAllocateId(allocateAddressResult.getAllocationId())
                            .withAssociationId(associateAddressResults.get(0).getAssociationId())
                            .build()))
                    .build();
            ret.add(cloudResource);
            LOGGER.info("EIp created for {}: association id: {}, allocationId: {}",
                    cloudInstance.getInstanceId(), associateAddressResults.get(0).getAssociationId(), allocateAddressResult.getAllocationId());
        } else {
            LOGGER.debug("No buildable EIp for {}", cloudInstance.getInstanceId());
        }
        return ret;
    }

    @Override
    protected boolean isFinished(AwsContext context, AuthenticatedContext auth, CloudResource resource) {
        return true;
    }

    @Override
    public CloudResource delete(AwsContext context, AuthenticatedContext auth, CloudResource resource) throws Exception {
        LOGGER.info("Terminate EIP with id: {}", resource.getInstanceId());
        EIpAttributes eipAttributes = resource.getParameter(CloudResource.ATTRIBUTES, EIpAttributes.class);
        CloudResource ret = null;
        if (eipAttributes != null) {
            DisassociateAddressRequest disassociateAddressRequest = new DisassociateAddressRequest()
                    .withAssociationId(eipAttributes.getAssociationId());
            awsMethodExecutor.execute(() -> context.getAmazonEc2Client().disassociateAddress(disassociateAddressRequest), null);
            ReleaseAddressRequest request = new ReleaseAddressRequest()
                    .withAllocationId(eipAttributes.getAllocateId());
            ReleaseAddressResult releaseAddressResult = awsMethodExecutor.execute(() -> context.getAmazonEc2Client().releaseAddress(request), null);
            ret = releaseAddressResult == null ? null : resource;
        } else {
            LOGGER.debug("Cannot find attribute for {}, release and disassociate skipped", resource.getName());
        }
        return ret;
    }

    @Override
    public ResourceType resourceType() {
        return ResourceType.AWS_RESERVED_IP;
    }

    @Override
    public int order() {
        return ORDER;
    }
}
