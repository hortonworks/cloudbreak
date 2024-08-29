package com.sequenceiq.cloudbreak.cloud.aws.resource.loadbalancer;

import static com.sequenceiq.common.api.type.InstanceGroupType.GATEWAY;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import jakarta.inject.Inject;

import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import com.sequenceiq.cloudbreak.cloud.model.CloudStack;
import com.sequenceiq.cloudbreak.cloud.model.Group;
import com.sequenceiq.cloudbreak.cloud.service.ResourceRetriever;
import com.sequenceiq.common.api.type.CommonStatus;
import com.sequenceiq.common.api.type.ResourceType;

@Service
public class AwsNativeLoadBalancerSecurityGroupProvider {

    @Inject
    private ResourceRetriever resourceRetriever;

    public List<String> getSecurityGroups(Long stackId, CloudStack stack) {
        List<String> securityGroupIds = getGatewayGroups(stack)
                .map(g -> g.getSecurity().getCloudSecurityIds())
                .flatMap(Collection::stream)
                .distinct()
                .collect(Collectors.toList());
        List<String> gatewayGroupNames = getGatewayGroups(stack)
                .map(g -> g.getName())
                .distinct()
                .collect(Collectors.toList());
        if (CollectionUtils.isEmpty(securityGroupIds)) {
            return resourceRetriever
                    .findAllByStatusAndTypeAndStack(
                            CommonStatus.CREATED,
                            ResourceType.AWS_SECURITY_GROUP,
                            stackId)
                    .stream()
                    .filter(e -> gatewayGroupNames.contains(e.getGroup()))
                    .map(e -> e.getReference())
                    .collect(Collectors.toList());
        } else {
            return securityGroupIds;
        }
    }

    private Stream<Group> getGatewayGroups(CloudStack stack) {
        return stack.getGroups().stream()
                .filter(g -> GATEWAY.equals(g.getType()));
    }
}
