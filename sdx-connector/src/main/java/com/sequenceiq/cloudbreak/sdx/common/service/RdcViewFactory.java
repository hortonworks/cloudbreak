package com.sequenceiq.cloudbreak.sdx.common.service;

import java.util.Collection;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.cloudera.api.swagger.model.ApiRemoteDataContext;
import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.annotation.Nulls;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sequenceiq.cloudbreak.common.exception.CloudbreakServiceException;
import com.sequenceiq.cloudbreak.sdx.RdcView;
import com.sequenceiq.cloudbreak.template.TemplateEndpoint;
import com.sequenceiq.cloudbreak.template.TemplateServiceConfig;

@Component
public class RdcViewFactory {

    private static final Logger LOGGER = LoggerFactory.getLogger(RdcViewFactory.class);

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    static {
        OBJECT_MAPPER.setDefaultSetterInfo(JsonSetter.Value.construct(Nulls.AS_EMPTY, Nulls.AS_EMPTY));
    }

    public RdcView create(String sdxCrn, Optional<String> remoteDataContext) {
        try {
            if (remoteDataContext.isPresent()) {
                ApiRemoteDataContext apiRemoteDataContext = OBJECT_MAPPER.readValue(remoteDataContext.get(), ApiRemoteDataContext.class);
                Set<TemplateEndpoint> endpoints = Stream.ofNullable(apiRemoteDataContext.getEndPoints())
                        .flatMap(Collection::stream)
                        .flatMap(apiEndPoint -> Stream.ofNullable(apiEndPoint.getEndPointHostList())
                                .flatMap(Collection::stream)
                                .map(host -> new TemplateEndpoint(apiEndPoint.getServiceType(), host.getType(), host.getUri())))
                        .collect(Collectors.toSet());
                Set<TemplateServiceConfig> serviceConfigs = Stream.ofNullable(apiRemoteDataContext.getEndPoints())
                        .flatMap(Collection::stream)
                        .flatMap(apiEndPoint -> Stream.ofNullable(apiEndPoint.getServiceConfigs())
                                .flatMap(Collection::stream)
                                .map(config -> new TemplateServiceConfig(apiEndPoint.getServiceType(), config.getKey(), config.getValue())))
                        .collect(Collectors.toSet());
                return new RdcView(sdxCrn, remoteDataContext.get(), endpoints, serviceConfigs, null);
            } else {
                return new RdcView(sdxCrn, null, null, null, null);
            }
        } catch (JsonProcessingException e) {
            String message = "Failed to process remote data context of " + sdxCrn;
            LOGGER.error(message, e);
            throw new CloudbreakServiceException(message);
        }
    }
}
