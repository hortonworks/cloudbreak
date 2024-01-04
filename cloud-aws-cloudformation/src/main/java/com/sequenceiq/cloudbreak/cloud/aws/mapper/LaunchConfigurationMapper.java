package com.sequenceiq.cloudbreak.cloud.aws.mapper;

import java.util.ArrayList;
import java.util.List;

import jakarta.inject.Inject;

import org.springframework.stereotype.Component;

import software.amazon.awssdk.services.autoscaling.model.BlockDeviceMapping;
import software.amazon.awssdk.services.autoscaling.model.CreateLaunchConfigurationRequest;
import software.amazon.awssdk.services.autoscaling.model.LaunchConfiguration;

@Component
public class LaunchConfigurationMapper {

    @Inject
    private EmptyToNullStringMapper emptyToNullStringMapper;

    public CreateLaunchConfigurationRequest.Builder mapExistingLaunchConfigToRequestBuilder(LaunchConfiguration launchConfiguration) {
        if (launchConfiguration == null) {
            return null;
        }

        CreateLaunchConfigurationRequest.Builder createLaunchConfigurationRequestBuilder = CreateLaunchConfigurationRequest.builder();

        createLaunchConfigurationRequestBuilder.launchConfigurationName(emptyToNullStringMapper.map(launchConfiguration.launchConfigurationName()));
        createLaunchConfigurationRequestBuilder.imageId(emptyToNullStringMapper.map(launchConfiguration.imageId()));
        createLaunchConfigurationRequestBuilder.keyName(emptyToNullStringMapper.map(launchConfiguration.keyName()));
        List<String> list = launchConfiguration.securityGroups();
        if (list != null) {
            createLaunchConfigurationRequestBuilder.securityGroups(new ArrayList<>(list));
        } else {
            createLaunchConfigurationRequestBuilder.securityGroups(List.of());
        }
        createLaunchConfigurationRequestBuilder.classicLinkVPCId(emptyToNullStringMapper.map(launchConfiguration.classicLinkVPCId()));
        List<String> list1 = launchConfiguration.classicLinkVPCSecurityGroups();
        if (list1 != null) {
            createLaunchConfigurationRequestBuilder.classicLinkVPCSecurityGroups(new ArrayList<>(list1));
        } else {
            createLaunchConfigurationRequestBuilder.classicLinkVPCSecurityGroups(List.of());
        }
        createLaunchConfigurationRequestBuilder.userData(emptyToNullStringMapper.map(launchConfiguration.userData()));
        createLaunchConfigurationRequestBuilder.instanceType(emptyToNullStringMapper.map(launchConfiguration.instanceType()));
        createLaunchConfigurationRequestBuilder.kernelId(emptyToNullStringMapper.map(launchConfiguration.kernelId()));
        createLaunchConfigurationRequestBuilder.ramdiskId(emptyToNullStringMapper.map(launchConfiguration.ramdiskId()));
        List<BlockDeviceMapping> list2 = launchConfiguration.blockDeviceMappings();
        if (list2 != null) {
            createLaunchConfigurationRequestBuilder.blockDeviceMappings(new ArrayList<>(list2));
        } else {
            createLaunchConfigurationRequestBuilder.blockDeviceMappings(List.of());
        }
        createLaunchConfigurationRequestBuilder.instanceMonitoring(launchConfiguration.instanceMonitoring());
        createLaunchConfigurationRequestBuilder.spotPrice(emptyToNullStringMapper.map(launchConfiguration.spotPrice()));
        createLaunchConfigurationRequestBuilder.iamInstanceProfile(emptyToNullStringMapper.map(launchConfiguration.iamInstanceProfile()));
        createLaunchConfigurationRequestBuilder.ebsOptimized(launchConfiguration.ebsOptimized());
        createLaunchConfigurationRequestBuilder.associatePublicIpAddress(launchConfiguration.associatePublicIpAddress());
        createLaunchConfigurationRequestBuilder.placementTenancy(emptyToNullStringMapper.map(launchConfiguration.placementTenancy()));

        return createLaunchConfigurationRequestBuilder;
    }
}
