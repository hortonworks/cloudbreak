package com.sequenceiq.environment.network;

import org.springframework.stereotype.Service;

import com.sequenceiq.environment.env.service.EnvironmentDto;

@Service
public class NetworkCreationService {

    public NetworkDto initVpc(EnvironmentDto environmentDto) {
        return NetworkDto.NetworkDtoBuilder.aVpcDto()
                .withEnvironmentId(environmentDto.getId())
                .withStatus("It's all right.")
                .build();
    }

    public NetworkDto createVpc(EnvironmentDto environmentDto) {
        return NetworkDto.NetworkDtoBuilder.aVpcDto()
                .withEnvironmentId(environmentDto.getId())
                .withStatus("It's all right.")
                .build();
    }
}
