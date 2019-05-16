package com.sequenceiq.environment.env.service;

import org.springframework.stereotype.Service;

import com.sequenceiq.environment.network.NetworkDto;

@Service
public class EnvironmentService {

    public EnvironmentDto getById(Long id) {
        return EnvironmentDto.EnvironmentDtoBuilder.anEnvironmentDto()
                .withId(id)
                .withName("helloka")
                .withCloudPlatform("AWS")
                .withStatus(EnvironmentStatus.CREATION_INITIATED)
                .withVpcDto(NetworkDto.NetworkDtoBuilder.aVpcDto()
                        .withId(1L)
                        .withEnvironmentId(id)
                        .withStatus("ok")
                        .build())
                .build();
    }
}
