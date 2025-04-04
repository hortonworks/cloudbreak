package com.sequenceiq.cloudbreak.sdx.common.grpc;

import java.util.Collections;
import java.util.Objects;

import jakarta.inject.Inject;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import com.cloudera.api.swagger.model.ApiRemoteDataContext;
import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.annotation.Nulls;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Preconditions;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.util.JsonFormat;
import com.sequenceiq.cloudbreak.auth.crn.RegionAwareInternalCrnGeneratorFactory;
import com.sequenceiq.cloudbreak.grpc.ManagedChannelWrapper;
import com.sequenceiq.cloudbreak.sdx.common.model.ServiceDiscoveryChannelConfig;

@Component
public class GrpcServiceDiscoveryClient {

    @Qualifier("discoveryManagedChannelWrapper")
    @Inject
    private ManagedChannelWrapper channelWrapper;

    @Inject
    private ServiceDiscoveryChannelConfig serviceDiscoveryChannelConfig;

    @Inject
    private RegionAwareInternalCrnGeneratorFactory regionAwareInternalCrnGeneratorFactory;

    public static GrpcServiceDiscoveryClient createClient(ManagedChannelWrapper channelWrapper, ServiceDiscoveryChannelConfig serviceDiscoveryChannelConfig) {
        GrpcServiceDiscoveryClient client = new GrpcServiceDiscoveryClient();
        client.channelWrapper = Preconditions.checkNotNull(channelWrapper, "channelWrapper should not be null.");
        client.serviceDiscoveryChannelConfig = Preconditions.checkNotNull(serviceDiscoveryChannelConfig,
                "serviceDiscoveryChannelConfig should not be null.");
        return client;
    }

    public String getRemoteDataContext(String sdxCrn) throws JsonProcessingException, InvalidProtocolBufferException {
        ServiceDiscoveryClient serviceDiscoveryClient = makeClient();
        String parsedJson = JsonFormat.printer().includingDefaultValueFields().print(serviceDiscoveryClient.getRemoteDataContext(sdxCrn));
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.setDefaultSetterInfo(JsonSetter.Value.construct(Nulls.AS_EMPTY, Nulls.AS_EMPTY));
        ApiRemoteDataContext apiRemoteDataContext = objectMapper.readValue(parsedJson, ApiRemoteDataContext.class);
        //Handle incorrectly set null values
        if (Objects.nonNull(apiRemoteDataContext)) {
            apiRemoteDataContext.getEndPoints()
                    .forEach(endpoint -> {
                        if (!CollectionUtils.isEmpty(endpoint.getEndPointHostList())) {
                            endpoint.getEndPointHostList()
                                    .forEach(apiEndPointHost -> {
                                        if (null == apiEndPointHost.getEndPointConfigs()) {
                                            apiEndPointHost.setEndPointConfigs(Collections.emptyList());
                                        }
                                    });
                        }
                    });
        }
        return objectMapper.writeValueAsString(apiRemoteDataContext);
    }

    public ServiceDiscoveryClient makeClient() {
        return new ServiceDiscoveryClient(channelWrapper.getChannel(), serviceDiscoveryChannelConfig, regionAwareInternalCrnGeneratorFactory);
    }
}
