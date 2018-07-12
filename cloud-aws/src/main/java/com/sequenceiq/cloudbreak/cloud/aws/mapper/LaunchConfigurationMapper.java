package com.sequenceiq.cloudbreak.cloud.aws.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Mappings;

import com.amazonaws.services.autoscaling.model.CreateLaunchConfigurationRequest;
import com.amazonaws.services.autoscaling.model.LaunchConfiguration;

@Mapper(componentModel = "spring", uses = EmptyToNullStringMapper.class)
public interface LaunchConfigurationMapper {

    @Mappings({
            @Mapping(target = "requestCredentials", ignore = true),
            @Mapping(target = "requestCredentialsProvider", ignore = true),
            @Mapping(target = "requestMetricCollector", ignore = true),
            @Mapping(target = "generalProgressListener", ignore = true),
            @Mapping(target = "sdkRequestTimeout", ignore = true),
            @Mapping(target = "sdkClientExecutionTimeout", ignore = true),
            @Mapping(target = "instanceId", ignore = true),
            @Mapping(target = "customRequestHeaders", ignore = true),
            @Mapping(target = "customQueryParameters", ignore = true)
    })
    CreateLaunchConfigurationRequest mapExistingLaunchConfigToRequest(LaunchConfiguration launchConfiguration);
}
