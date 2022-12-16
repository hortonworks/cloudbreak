package com.sequenceiq.cloudbreak.cloud.aws;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.aws.common.CommonAwsClient;
import com.sequenceiq.cloudbreak.cloud.aws.common.client.AmazonElasticLoadBalancingClient;
import com.sequenceiq.cloudbreak.cloud.aws.common.loadbalancer.LoadBalancerService;
import com.sequenceiq.cloudbreak.cloud.aws.common.view.AwsCredentialView;
import com.sequenceiq.cloudbreak.cloud.aws.common.view.AwsNetworkView;
import com.sequenceiq.cloudbreak.cloud.aws.resource.loadbalancer.AwsNativeLoadBalancerLaunchService;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.exception.QuotaExceededException;
import com.sequenceiq.cloudbreak.cloud.exception.TemplatingNotSupportedException;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.CloudInstance;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.CloudResourceStatus;
import com.sequenceiq.cloudbreak.cloud.model.CloudStack;
import com.sequenceiq.cloudbreak.cloud.model.Network;
import com.sequenceiq.cloudbreak.cloud.model.TlsInfo;
import com.sequenceiq.cloudbreak.cloud.notification.PersistenceNotifier;
import com.sequenceiq.cloudbreak.cloud.service.ResourceRetriever;
import com.sequenceiq.cloudbreak.cloud.template.AbstractResourceConnector;
import com.sequenceiq.common.api.adjustment.AdjustmentTypeWithThreshold;
import com.sequenceiq.common.api.type.CommonStatus;
import com.sequenceiq.common.api.type.InstanceGroupType;
import com.sequenceiq.common.api.type.ResourceType;

@Component
public class AwsNativeResourceConnector extends AbstractResourceConnector {

    private static final Logger LOGGER = LoggerFactory.getLogger(AwsNativeResourceConnector.class);

    @Value("${cb.aws.vpc:}")
    private String cloudbreakVpc;

    @Inject
    private CommonAwsClient commonAwsClient;

    @Inject
    private AwsNativeLoadBalancerLaunchService loadBalancerLaunchService;

    @Inject
    private PersistenceNotifier persistenceNotifier;

    @Inject
    private LoadBalancerService loadBalancerService;

    @Inject
    private ResourceRetriever resourceRetriever;

    @Override
    public List<CloudResourceStatus> launchLoadBalancers(AuthenticatedContext authenticatedContext, CloudStack stack, PersistenceNotifier persistenceNotifier)
            throws Exception {
        LOGGER.info("Launching elastic load balancers");
        CloudCredential cloudCredential = authenticatedContext.getCloudCredential();
        String region = authenticatedContext.getCloudContext().getLocation().getRegion().value();
        AwsCredentialView awsCredentialView = new AwsCredentialView(cloudCredential);
        AmazonElasticLoadBalancingClient elasticLoadBalancingClient = commonAwsClient.createElasticLoadBalancingClient(awsCredentialView, region);
        return loadBalancerLaunchService.launchLoadBalancerResources(authenticatedContext, stack, persistenceNotifier, elasticLoadBalancingClient, true);
    }

    @Override
    public void updateUserData(AuthenticatedContext authenticatedContext, CloudStack stack, List<CloudResource> resources,
            Map<InstanceGroupType, String> userData) {
        LOGGER.info("Update userdata is not implemented on AWS Native!");
    }

    @Override
    public TlsInfo getTlsInfo(AuthenticatedContext authenticatedContext, CloudStack cloudStack) {
        Network network = cloudStack.getNetwork();
        AwsNetworkView networkView = new AwsNetworkView(network);
        boolean existingVPC = networkView.isExistingVPC();
        boolean sameVPC = StringUtils.isNotEmpty(cloudbreakVpc) && existingVPC && networkView.getExistingVpc().equals(cloudbreakVpc);
        return new TlsInfo(sameVPC);
    }

    @Override
    public String getStackTemplate() throws TemplatingNotSupportedException {
        throw new TemplatingNotSupportedException();
    }

    @Override
    public String getDBStackTemplate() throws TemplatingNotSupportedException {
        throw new TemplatingNotSupportedException();
    }

    @Override
    protected List<CloudResource> collectProviderSpecificResources(List<CloudResource> resources, List<CloudInstance> vms) {
        return List.of();
    }

    @Override
    protected boolean isCloudResourceAndCloudInstanceEquals(CloudInstance instance, CloudResource resource) {
        return instance.getInstanceId().equals(resource.getReference()) || instance.getInstanceId().equals(resource.getInstanceId());
    }

    @Override
    protected ResourceType getDiskResourceType() {
        return ResourceType.AWS_VOLUMESET;
    }

    @Override
    public List<CloudResourceStatus> upscale(AuthenticatedContext auth, CloudStack stack, List<CloudResource> resources,
            AdjustmentTypeWithThreshold adjustmentTypeWithThreshold) throws QuotaExceededException {
        List<CloudResourceStatus> upscale = super.upscale(auth, stack, resources, adjustmentTypeWithThreshold);
        LOGGER.info("Launching elastic load balancers");
        CloudCredential cloudCredential = auth.getCloudCredential();
        String region = auth.getCloudContext().getLocation().getRegion().value();
        AwsCredentialView awsCredentialView = new AwsCredentialView(cloudCredential);
        AmazonElasticLoadBalancingClient elasticLoadBalancingClient = commonAwsClient.createElasticLoadBalancingClient(awsCredentialView, region);
        loadBalancerLaunchService.launchLoadBalancerResources(auth, stack, persistenceNotifier, elasticLoadBalancingClient, true);
        return upscale;
    }

    @Override
    public List<CloudResourceStatus> downscale(AuthenticatedContext auth, CloudStack stack, List<CloudResource> resources, List<CloudInstance> vms,
            List<CloudResource> resourcesToRemove) {
        List<CloudResourceStatus> downscale = super.downscale(auth, stack, resources, vms, resourcesToRemove);

        List<String> targetGroupArns = resourceRetriever
                .findAllByStatusAndTypeAndStack(CommonStatus.CREATED, ResourceType.ELASTIC_LOAD_BALANCER_TARGET_GROUP, auth.getCloudContext().getId())
                .stream().map(CloudResource::getReference).collect(Collectors.toList());
        loadBalancerService.removeLoadBalancerTargets(auth, targetGroupArns, resourcesToRemove);
        return downscale;
    }
}
