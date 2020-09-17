package com.sequenceiq.environment.environment.validation.network;

import com.sequenceiq.environment.network.dto.NetworkDto;

public class TestNetworkBuilder {

    private NetworkDto.Builder networkDtoBuilder;

    public TestNetworkBuilder() {
        networkDtoBuilder = NetworkDto.builder()
                .withId(1L)
                .withName("networkName")
                .withResourceCrn("aResourceCRN");
    }

    public NetworkDto.Builder getBuilder() {
        return networkDtoBuilder;
    }

    public NetworkDto build() {
        return networkDtoBuilder.build();
    }
}
