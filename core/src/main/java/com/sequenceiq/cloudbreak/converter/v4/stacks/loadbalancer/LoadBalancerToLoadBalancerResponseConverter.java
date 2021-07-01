package com.sequenceiq.cloudbreak.converter.v4.stacks.loadbalancer;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.loadbalancer.AwsLoadBalancerResponse;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.loadbalancer.AwsTargetGroupResponse;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.loadbalancer.LoadBalancerResponse;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.loadbalancer.TargetGroupResponse;
import com.sequenceiq.cloudbreak.cloud.model.TargetGroupPortPair;
import com.sequenceiq.cloudbreak.converter.AbstractConversionServiceAwareConverter;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceGroup;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceMetaData;
import com.sequenceiq.cloudbreak.domain.stack.loadbalancer.aws.AwsLoadBalancerConfigDb;
import com.sequenceiq.cloudbreak.domain.stack.loadbalancer.aws.AwsTargetGroupArnsDb;
import com.sequenceiq.cloudbreak.domain.stack.loadbalancer.aws.AwsTargetGroupConfigDb;
import com.sequenceiq.cloudbreak.domain.stack.loadbalancer.LoadBalancer;
import com.sequenceiq.cloudbreak.domain.stack.loadbalancer.TargetGroupConfigDbWrapper;
import com.sequenceiq.cloudbreak.domain.stack.loadbalancer.TargetGroup;
import com.sequenceiq.cloudbreak.service.LoadBalancerConfigService;
import com.sequenceiq.cloudbreak.service.stack.InstanceGroupService;
import com.sequenceiq.cloudbreak.service.stack.InstanceMetaDataService;
import com.sequenceiq.cloudbreak.service.stack.TargetGroupPersistenceService;

@Component
public class LoadBalancerToLoadBalancerResponseConverter extends AbstractConversionServiceAwareConverter<LoadBalancer, LoadBalancerResponse> {

    @Inject
    private TargetGroupPersistenceService targetGroupService;

    @Inject
    private InstanceGroupService instanceGroupService;

    @Inject
    private InstanceMetaDataService instanceMetaDataService;

    @Inject
    private LoadBalancerConfigService loadBalancerConfigService;

    @Override
    public LoadBalancerResponse convert(LoadBalancer source) {
        LoadBalancerResponse response = new LoadBalancerResponse();
        response.setIp(source.getIp());
        response.setCloudDns(source.getDns());
        response.setFqdn(source.getFqdn());
        response.setType(source.getType());
        response.setTargets(convertTargetGroups(targetGroupService.findByLoadBalancerId(source.getId())));
        if (source.getProviderConfig() != null) {
            response.setAwsResourceId(convertAwsLoadBalancer(source.getProviderConfig().getAwsConfig()));
        }

        return response;
    }

    private AwsLoadBalancerResponse convertAwsLoadBalancer(AwsLoadBalancerConfigDb awsMetadata) {
        if (awsMetadata != null) {
            AwsLoadBalancerResponse awsSettings = new AwsLoadBalancerResponse();
            awsSettings.setArn(awsMetadata.getArn());
            return awsSettings;
        }
        return null;
    }

    public List<TargetGroupResponse> convertTargetGroups(Set<TargetGroup> targetGroups) {
        List<TargetGroupResponse> allResponses = new ArrayList<>();
        for (TargetGroup targetGroup : targetGroups) {
            allResponses.addAll(convertTargetGroup(targetGroup));
        }
        return allResponses;
    }

    public List<TargetGroupResponse> convertTargetGroup(TargetGroup targetGroup) {
        Set<InstanceGroup> instanceGroups = instanceGroupService.findByTargetGroupId(targetGroup.getId());
        Set<String> instanceIds = getInstanceMetadataForGroups(instanceGroups).stream()
            .map(InstanceMetaData::getInstanceId)
            .collect(Collectors.toSet());
        TargetGroupConfigDbWrapper targetGroupConfig = targetGroup.getProviderConfig();
        Set<TargetGroupPortPair> portPairs = loadBalancerConfigService.getTargetGroupPortPairs(targetGroup);

        return portPairs.stream()
            .map(portPair -> mapPortPairToTargetGroup(instanceIds, targetGroupConfig, portPair))
            .collect(Collectors.toList());
    }

    private TargetGroupResponse mapPortPairToTargetGroup(Set<String> instanceIds, TargetGroupConfigDbWrapper targetGroupConfig, TargetGroupPortPair portPair) {
        TargetGroupResponse response = new TargetGroupResponse();
        response.setPort(portPair.getTrafficPort());
        response.setTargetInstances(instanceIds);
        if (targetGroupConfig != null) {
            response.setAwsResourceIds(convertAwsTargetGroup(targetGroupConfig.getAwsConfig(), portPair.getTrafficPort()));
        }
        return response;
    }

    private AwsTargetGroupResponse convertAwsTargetGroup(AwsTargetGroupConfigDb awsConfig, Integer port) {
        if (awsConfig != null) {
            Optional<AwsTargetGroupArnsDb> arns = awsConfig.getPortArnMapping().entrySet().stream()
                .filter(entry -> entry.getKey().equals(port))
                .map(Map.Entry::getValue)
                .findFirst();
            if (arns.isPresent()) {
                AwsTargetGroupResponse awsSettings = new AwsTargetGroupResponse();
                awsSettings.setListenerArn(arns.get().getListenerArn());
                awsSettings.setTargetGroupArn(arns.get().getTargetGroupArn());
                return awsSettings;
            }
        }
        return null;
    }

    private List<InstanceMetaData> getInstanceMetadataForGroups(Set<InstanceGroup> instanceGroups) {
        return instanceGroups.stream()
            .map(ig -> instanceMetaDataService.findAliveInstancesInInstanceGroup(ig.getId()))
            .flatMap(List::stream)
            .collect(Collectors.toList());
    }
}
