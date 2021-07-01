package com.sequenceiq.cloudbreak.service;

import static com.sequenceiq.cloudbreak.common.type.CloudConstants.AWS;
import static com.sequenceiq.cloudbreak.common.type.CloudConstants.AZURE;
import static com.sequenceiq.cloudbreak.common.type.CloudConstants.GCP;

import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.cloud.aws.common.view.AwsLoadBalancerMetadataView;
import com.sequenceiq.cloudbreak.cloud.model.CloudLoadBalancerMetadata;
import com.sequenceiq.cloudbreak.cloud.model.TargetGroupPortPair;
import com.sequenceiq.cloudbreak.domain.stack.loadbalancer.aws.AwsLoadBalancerConfigDb;
import com.sequenceiq.cloudbreak.domain.stack.loadbalancer.aws.AwsTargetGroupArnsDb;
import com.sequenceiq.cloudbreak.domain.stack.loadbalancer.aws.AwsTargetGroupConfigDb;
import com.sequenceiq.cloudbreak.domain.stack.loadbalancer.LoadBalancerConfigDbWrapper;
import com.sequenceiq.cloudbreak.domain.stack.loadbalancer.TargetGroupConfigDbWrapper;
import com.sequenceiq.cloudbreak.domain.stack.loadbalancer.TargetGroup;

@Service
public class LoadBalancerConfigConverter {

    static final String MISSING_CLOUD_RESOURCE = "Could not find cloud resource corresponding to this traffic port.";

    @Inject
    private LoadBalancerConfigService loadBalancerConfigService;

    public LoadBalancerConfigDbWrapper convertLoadBalancer(String cloudPlatform, CloudLoadBalancerMetadata cloudLoadBalancerMetadata) {
        switch (cloudPlatform) {
            case AWS:
                return buildAwsConfig(new AwsLoadBalancerMetadataView(cloudLoadBalancerMetadata));
            // TODO: AZURE, GCP
            default:
                return new LoadBalancerConfigDbWrapper();
        }
    }

    private LoadBalancerConfigDbWrapper buildAwsConfig(AwsLoadBalancerMetadataView awsMetadata) {
        LoadBalancerConfigDbWrapper cloudLoadBalancerConfigDbWrapper = new LoadBalancerConfigDbWrapper();
        AwsLoadBalancerConfigDb awsLoadBalancerConfigDb = new AwsLoadBalancerConfigDb();
        awsLoadBalancerConfigDb.setArn(awsMetadata.getLoadbalancerArn());
        cloudLoadBalancerConfigDbWrapper.setAwsConfig(awsLoadBalancerConfigDb);
        return cloudLoadBalancerConfigDbWrapper;
    }

    public TargetGroupConfigDbWrapper convertTargetGroup(String cloudPlatform, CloudLoadBalancerMetadata cloudLoadBalancerMetadata, TargetGroup targetGroup) {
        switch (cloudPlatform) {
            case AWS:
                return buildAwsConfig(new AwsLoadBalancerMetadataView(cloudLoadBalancerMetadata), targetGroup);
            case AZURE:
            case GCP:
            default:
                return new TargetGroupConfigDbWrapper();
        }
    }

    private TargetGroupConfigDbWrapper buildAwsConfig(AwsLoadBalancerMetadataView awsMetadata, TargetGroup targetGroup) {
        Set<Integer> trafficPorts = loadBalancerConfigService.getTargetGroupPortPairs(targetGroup).stream()
            .map(TargetGroupPortPair::getTrafficPort)
            .collect(Collectors.toSet());

        TargetGroupConfigDbWrapper targetGroupConfigDbWrapper = new TargetGroupConfigDbWrapper();
        AwsTargetGroupConfigDb awsTargetGroupConfigDb = new AwsTargetGroupConfigDb();
        for (Integer port : trafficPorts) {
            AwsTargetGroupArnsDb targetGroupArns = new AwsTargetGroupArnsDb();
            String listenerArn = awsMetadata.getListenerArnByPort(port);
            String targetGroupArn = awsMetadata.getTargetGroupArnByPort(port);
            targetGroupArns.setListenerArn(StringUtils.isEmpty(listenerArn) ? MISSING_CLOUD_RESOURCE : listenerArn);
            targetGroupArns.setTargetGroupArn(StringUtils.isEmpty(targetGroupArn) ? MISSING_CLOUD_RESOURCE : targetGroupArn);
            awsTargetGroupConfigDb.addPortArnMapping(port, targetGroupArns);
        }
        targetGroupConfigDbWrapper.setAwsConfig(awsTargetGroupConfigDb);
        return targetGroupConfigDbWrapper;
    }
}
