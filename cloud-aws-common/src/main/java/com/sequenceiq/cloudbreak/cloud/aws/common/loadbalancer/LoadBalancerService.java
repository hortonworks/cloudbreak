package com.sequenceiq.cloudbreak.cloud.aws.common.loadbalancer;

import static org.slf4j.LoggerFactory.getLogger;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.aws.common.CommonAwsClient;
import com.sequenceiq.cloudbreak.cloud.aws.common.client.AmazonElasticLoadBalancingClient;
import com.sequenceiq.cloudbreak.cloud.aws.common.view.AwsCredentialView;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;

import software.amazon.awssdk.services.elasticloadbalancingv2.model.DeregisterTargetsRequest;
import software.amazon.awssdk.services.elasticloadbalancingv2.model.InvalidTargetException;
import software.amazon.awssdk.services.elasticloadbalancingv2.model.TargetDescription;
import software.amazon.awssdk.services.elasticloadbalancingv2.model.TargetGroupNotFoundException;

@Component
public class LoadBalancerService {

    private static final Logger LOGGER = getLogger(LoadBalancerService.class);

    @Inject

    private CommonAwsClient awsClient;

    public void removeLoadBalancerTargets(AuthenticatedContext ac, List<String> targetGroupArns, List<CloudResource> resourcesToRemove) {
        if (targetGroupArns.isEmpty()) {
            LOGGER.info("Cannot find target group for the stack, skip the removing of the targets");
            return;
        }
        for (String targetGroupArn : targetGroupArns) {
            String region = ac.getCloudContext().getLocation().getRegion().value();
            AmazonElasticLoadBalancingClient amazonElbClient =
                    awsClient.createElasticLoadBalancingClient(new AwsCredentialView(ac.getCloudCredential()), region);

            LOGGER.debug("Get a list of the instance ids to remove");
            Set<String> instancesToRemove = getInstanceIdsForGroups(resourcesToRemove);

            LOGGER.debug("Deregister any instances that no longer exist");
            if (!instancesToRemove.isEmpty()) {
                try {
                    List<TargetDescription> targetsToRemove = instancesToRemove.stream()
                            .map(instanceId -> TargetDescription.builder().id(instanceId).build())
                            .collect(Collectors.toList());
                    amazonElbClient.deregisterTargets(DeregisterTargetsRequest.builder()
                            .targetGroupArn(targetGroupArn)
                            .targets(targetsToRemove)
                            .build());
                    LOGGER.debug("Targets deregistered: {}", targetsToRemove);
                } catch (TargetGroupNotFoundException targetGroupNotFoundException) {
                    LOGGER.debug("Couldn't find the specified target group with ARN {}", targetGroupArn, targetGroupNotFoundException);
                } catch (InvalidTargetException ignored) {
                    LOGGER.debug("no-op - we tried to remove a target that wasn't in the target group, which is fine");
                }
            }
        }
    }

    private Set<String> getInstanceIdsForGroups(List<CloudResource> resources) {
        return resources.stream()
                .filter(instance -> instance.getInstanceId() != null)
                .map(CloudResource::getInstanceId)
                .collect(Collectors.toSet());
    }
}
