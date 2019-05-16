package com.sequenceiq.environment.env.service;

import org.springframework.stereotype.Component;

import com.sequenceiq.environment.env.Environment;

@Component
public class EnvEntityConverter {

    public Environment dtoToEntity(EnvironmentDto environmentDto) {
        return Environment.EnvironmentBuilder.anEnvironment()
                .withName(environmentDto.getName())
                .withCloudPlatform(environmentDto.getCloudPlatform())
                .build();
    }
}
