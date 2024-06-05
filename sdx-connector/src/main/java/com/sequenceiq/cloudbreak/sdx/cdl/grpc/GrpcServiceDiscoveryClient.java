package com.sequenceiq.cloudbreak.sdx.cdl.grpc;

import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import jakarta.inject.Inject;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import com.cloudera.api.swagger.model.ApiRemoteDataContext;
import com.cloudera.cdp.servicediscovery.ServiceDiscoveryProto;
import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.annotation.Nulls;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Preconditions;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.util.JsonFormat;
import com.sequenceiq.cloudbreak.auth.crn.RegionAwareInternalCrnGeneratorFactory;
import com.sequenceiq.cloudbreak.grpc.ManagedChannelWrapper;
import com.sequenceiq.cloudbreak.sdx.cdl.config.ServiceDiscoveryChannelConfig;

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

    public Map<String, String> getServiceConfiguration(String sdxCrn, String name) {
        ServiceDiscoveryClient serviceDiscoveryClient = makeClient();
        ServiceDiscoveryProto.ApiRemoteDataContext remoteDataContext = serviceDiscoveryClient.getRemoteDataContext(sdxCrn);
        if (Objects.nonNull(remoteDataContext)) {
            Optional<ServiceDiscoveryProto.ApiEndPoint> apiEndpoint = remoteDataContext
                    .getEndPointsList()
                    .stream()
                    .filter(endpoint -> endpoint.getName().equalsIgnoreCase(name))
                    .findFirst();
            if (apiEndpoint.isEmpty()) {
                return Collections.emptyMap();
            }
            return apiEndpoint
                    .map(apiEndPoint -> apiEndPoint.getServiceConfigsList()
                            .stream()
                            .collect(Collectors.toMap(ServiceDiscoveryProto.ApiMapEntry::getKey, ServiceDiscoveryProto.ApiMapEntry::getValue)))
                    .orElse(Collections.emptyMap());

        }
        return Collections.emptyMap();
    }

    public ServiceDiscoveryClient makeClient() {
        return new ServiceDiscoveryClient(channelWrapper.getChannel(), serviceDiscoveryChannelConfig, regionAwareInternalCrnGeneratorFactory);
    }
}
