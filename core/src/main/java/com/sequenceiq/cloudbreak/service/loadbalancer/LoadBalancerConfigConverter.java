package com.sequenceiq.cloudbreak.service.loadbalancer;

import static com.sequenceiq.cloudbreak.common.type.CloudConstants.AWS;
import static com.sequenceiq.cloudbreak.common.type.CloudConstants.AZURE;
import static com.sequenceiq.cloudbreak.common.type.CloudConstants.GCP;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.cloud.aws.common.view.AwsLoadBalancerMetadataView;
import com.sequenceiq.cloudbreak.cloud.azure.view.AzureLoadBalancerMetadataView;
import com.sequenceiq.cloudbreak.cloud.gcp.view.GcpLoadBalancerMetadataView;
import com.sequenceiq.cloudbreak.cloud.model.CloudLoadBalancerMetadata;
import com.sequenceiq.cloudbreak.cloud.model.TargetGroupPortPair;
import com.sequenceiq.cloudbreak.domain.stack.loadbalancer.LoadBalancerConfigDbWrapper;
import com.sequenceiq.cloudbreak.domain.stack.loadbalancer.TargetGroup;
import com.sequenceiq.cloudbreak.domain.stack.loadbalancer.TargetGroupConfigDbWrapper;
import com.sequenceiq.cloudbreak.domain.stack.loadbalancer.aws.AwsLoadBalancerConfigDb;
import com.sequenceiq.cloudbreak.domain.stack.loadbalancer.aws.AwsTargetGroupArnsDb;
import com.sequenceiq.cloudbreak.domain.stack.loadbalancer.aws.AwsTargetGroupConfigDb;
import com.sequenceiq.cloudbreak.domain.stack.loadbalancer.azure.AzureLoadBalancerConfigDb;
import com.sequenceiq.cloudbreak.domain.stack.loadbalancer.azure.AzureTargetGroupConfigDb;
import com.sequenceiq.cloudbreak.domain.stack.loadbalancer.gcp.GcpLoadBalancerConfigDb;
import com.sequenceiq.cloudbreak.domain.stack.loadbalancer.gcp.GcpLoadBalancerNamesDb;
import com.sequenceiq.cloudbreak.domain.stack.loadbalancer.gcp.GcpTargetGroupConfigDb;
import com.sequenceiq.common.api.type.LoadBalancerSku;

@Service
public class LoadBalancerConfigConverter {

    static final String MISSING_CLOUD_RESOURCE = "Could not find cloud resource corresponding to this traffic port.";

    @Inject
    private TargetGroupPortProvider targetGroupPortProvider;

    public LoadBalancerConfigDbWrapper convertLoadBalancer(String cloudPlatform, CloudLoadBalancerMetadata cloudLoadBalancerMetadata, LoadBalancerSku sku) {
        return switch (cloudPlatform) {
            case AWS -> buildAwsConfig(new AwsLoadBalancerMetadataView(cloudLoadBalancerMetadata));
            case AZURE -> buildAzureConfig(new AzureLoadBalancerMetadataView(cloudLoadBalancerMetadata), sku);
            case GCP -> buildGcpConfig(new GcpLoadBalancerMetadataView(cloudLoadBalancerMetadata));
            default -> new LoadBalancerConfigDbWrapper();
        };
    }

    private LoadBalancerConfigDbWrapper buildAwsConfig(AwsLoadBalancerMetadataView awsMetadata) {
        LoadBalancerConfigDbWrapper cloudLoadBalancerConfigDbWrapper = new LoadBalancerConfigDbWrapper();
        AwsLoadBalancerConfigDb awsLoadBalancerConfigDb = new AwsLoadBalancerConfigDb();
        awsLoadBalancerConfigDb.setArn(awsMetadata.getLoadbalancerArn());
        cloudLoadBalancerConfigDbWrapper.setAwsConfig(awsLoadBalancerConfigDb);
        return cloudLoadBalancerConfigDbWrapper;
    }

    private LoadBalancerConfigDbWrapper buildAzureConfig(AzureLoadBalancerMetadataView azureMetadata, LoadBalancerSku sku) {
        LoadBalancerConfigDbWrapper cloudLoadBalancerConfigDbWrapper = new LoadBalancerConfigDbWrapper();
        AzureLoadBalancerConfigDb azureLoadBalancerConfigDb = new AzureLoadBalancerConfigDb();
        azureLoadBalancerConfigDb.setName(azureMetadata.getLoadbalancerName());
        azureLoadBalancerConfigDb.setSku(sku);
        cloudLoadBalancerConfigDbWrapper.setAzureConfig(azureLoadBalancerConfigDb);
        return cloudLoadBalancerConfigDbWrapper;
    }

    private LoadBalancerConfigDbWrapper buildGcpConfig(GcpLoadBalancerMetadataView gcpMetadata) {
        LoadBalancerConfigDbWrapper cloudLoadBalancerConfigDbWrapper = new LoadBalancerConfigDbWrapper();
        GcpLoadBalancerConfigDb gcpLoadBalancerConfigDb = new GcpLoadBalancerConfigDb();
        gcpLoadBalancerConfigDb.setName(gcpMetadata.getLoadbalancerName());
        cloudLoadBalancerConfigDbWrapper.setGcpConfig(gcpLoadBalancerConfigDb);
        return cloudLoadBalancerConfigDbWrapper;
    }

    public TargetGroupConfigDbWrapper convertTargetGroup(String cloudPlatform, CloudLoadBalancerMetadata cloudLoadBalancerMetadata, TargetGroup targetGroup) {
        return switch (cloudPlatform) {
            case AWS -> buildAwsConfig(new AwsLoadBalancerMetadataView(cloudLoadBalancerMetadata), targetGroup);
            case AZURE -> buildAzureConfig(new AzureLoadBalancerMetadataView(cloudLoadBalancerMetadata), targetGroup);
            case GCP -> buildGcpConfig(new GcpLoadBalancerMetadataView(cloudLoadBalancerMetadata), targetGroup);
            default -> new TargetGroupConfigDbWrapper();
        };
    }

    private TargetGroupConfigDbWrapper buildAwsConfig(AwsLoadBalancerMetadataView awsMetadata, TargetGroup targetGroup) {
        Set<Integer> trafficPorts = getTrafficPorts(targetGroup);

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

    private TargetGroupConfigDbWrapper buildAzureConfig(AzureLoadBalancerMetadataView azureMetadata, TargetGroup targetGroup) {
        Set<Integer> trafficPorts = getTrafficPorts(targetGroup);

        TargetGroupConfigDbWrapper targetGroupConfigDbWrapper = new TargetGroupConfigDbWrapper();
        AzureTargetGroupConfigDb azureTargetGroupConfigDb = new AzureTargetGroupConfigDb();
        for (Integer port : trafficPorts) {
            String availabilitySetName = azureMetadata.getAvailabilitySetByPort(port);
            List<String> availabilitySetNames = List.of(StringUtils.isEmpty(availabilitySetName) ? MISSING_CLOUD_RESOURCE : availabilitySetName);
            azureTargetGroupConfigDb.addPortAvailabilitySetMapping(port, availabilitySetNames);
        }
        targetGroupConfigDbWrapper.setAzureConfig(azureTargetGroupConfigDb);
        return targetGroupConfigDbWrapper;
    }

    private TargetGroupConfigDbWrapper buildGcpConfig(GcpLoadBalancerMetadataView gcpMetadata, TargetGroup targetGroup) {
        Set<Integer> trafficPorts = getTrafficPorts(targetGroup);
        TargetGroupConfigDbWrapper targetGroupConfigDbWrapper = new TargetGroupConfigDbWrapper();
        GcpTargetGroupConfigDb gcpTargetGroupConfigDb = new GcpTargetGroupConfigDb();
        for (Integer port : trafficPorts) {
            GcpLoadBalancerNamesDb namesDb = new GcpLoadBalancerNamesDb();
            namesDb.setInstanceGroupName(gcpMetadata.getInstanceGroupByPort(port));
            namesDb.setBackendServiceName(gcpMetadata.getBackendServiceByPort(port));
            gcpTargetGroupConfigDb.addPortNameMapping(port, namesDb);
        }
        targetGroupConfigDbWrapper.setGcpConfig(gcpTargetGroupConfigDb);
        return targetGroupConfigDbWrapper;
    }

    private Set<Integer> getTrafficPorts(TargetGroup targetGroup) {
        return targetGroupPortProvider.getTargetGroupPortPairs(targetGroup).stream()
                .map(TargetGroupPortPair::getTrafficPort)
                .collect(Collectors.toSet());
    }
}