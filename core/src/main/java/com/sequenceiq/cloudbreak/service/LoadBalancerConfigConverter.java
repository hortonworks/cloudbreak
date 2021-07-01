package com.sequenceiq.cloudbreak.service;

import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.cloud.model.TargetGroupPortPair;
import com.sequenceiq.cloudbreak.cloud.model.loadbalancer.AwsLoadBalancerMetadata;
import com.sequenceiq.cloudbreak.cloud.model.loadbalancer.AwsTargetGroupMetadata;
import com.sequenceiq.cloudbreak.cloud.model.loadbalancer.LoadBalancerMetadataBase;
import com.sequenceiq.cloudbreak.domain.stack.loadbalancer.aws.AwsLoadBalancerConfigDb;
import com.sequenceiq.cloudbreak.domain.stack.loadbalancer.aws.AwsTargetGroupArnsDb;
import com.sequenceiq.cloudbreak.domain.stack.loadbalancer.aws.AwsTargetGroupConfigDb;
import com.sequenceiq.cloudbreak.domain.stack.loadbalancer.LoadBalancerConfigDbWrapper;
import com.sequenceiq.cloudbreak.domain.stack.loadbalancer.TargetGroupConfigDbWrapper;
import com.sequenceiq.cloudbreak.domain.stack.loadbalancer.TargetGroup;

import static com.sequenceiq.cloudbreak.common.type.CloudConstants.AWS;

@Service
public class LoadBalancerConfigConverter {

    static final String MISSING_CLOUD_RESOURCE = "Could not find cloud resource corresponding to this traffic port.";

    @Inject
    private LoadBalancerConfigService loadBalancerConfigService;

    public LoadBalancerConfigDbWrapper convertLoadBalancer(String cloudPlatform, LoadBalancerMetadataBase cloudMetadata) {
        switch (cloudPlatform) {
            case AWS:
                return buildAwsConfig(cloudMetadata.getAwsMetadata());
            // TODO: AZURE, GCP
            default:
                return new LoadBalancerConfigDbWrapper();
        }
    }

    private LoadBalancerConfigDbWrapper buildAwsConfig(AwsLoadBalancerMetadata awsMetadata) {
        LoadBalancerConfigDbWrapper cloudLoadBalancerConfigDbWrapper = new LoadBalancerConfigDbWrapper();
        AwsLoadBalancerConfigDb awsLoadBalancerConfigDb = new AwsLoadBalancerConfigDb();
        awsLoadBalancerConfigDb.setArn(awsMetadata.getArn());
        cloudLoadBalancerConfigDbWrapper.setAwsConfig(awsLoadBalancerConfigDb);
        return cloudLoadBalancerConfigDbWrapper;
    }

    public TargetGroupConfigDbWrapper convertTargetGroup(String cloudPlatform, LoadBalancerMetadataBase cloudMetadata, TargetGroup targetGroup) {
        switch (cloudPlatform) {
            case AWS:
                return buildAwsConfig(cloudMetadata.getAwsMetadata(), targetGroup);
            // TODO: AZURE, GCP
            default:
                return new TargetGroupConfigDbWrapper();
        }
    }

    private TargetGroupConfigDbWrapper buildAwsConfig(AwsLoadBalancerMetadata awsMetadata, TargetGroup targetGroup) {
        Set<Integer> trafficPorts = loadBalancerConfigService.getTargetGroupPortPairs(targetGroup).stream()
            .map(TargetGroupPortPair::getTrafficPort)
            .collect(Collectors.toSet());

        TargetGroupConfigDbWrapper targetGroupConfigDbWrapper = new TargetGroupConfigDbWrapper();
        AwsTargetGroupConfigDb awsTargetGroupConfigDb = new AwsTargetGroupConfigDb();
        for (Integer port : trafficPorts) {
            Optional<AwsTargetGroupMetadata> targetGroupMetadata = awsMetadata.getTargetGroupMetadata().stream()
                .filter(tg -> tg.getPort() == port)
                .findFirst();
            AwsTargetGroupArnsDb targetGroupArns = new AwsTargetGroupArnsDb();
            if (targetGroupMetadata.isPresent()) {
                targetGroupArns.setListenerArn(targetGroupMetadata.get().getListenerArn());
                targetGroupArns.setTargetGroupArn(targetGroupMetadata.get().getTargetGroupArn());
            } else {
                targetGroupArns.setListenerArn(MISSING_CLOUD_RESOURCE);
                targetGroupArns.setTargetGroupArn(MISSING_CLOUD_RESOURCE);
            }
            awsTargetGroupConfigDb.addPortArnMapping(port, targetGroupArns);
        }
        targetGroupConfigDbWrapper.setAwsConfig(awsTargetGroupConfigDb);
        return targetGroupConfigDbWrapper;
    }
}
