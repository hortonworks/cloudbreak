package com.sequenceiq.cloudbreak.converter.mapper;

import java.util.Set;

import javax.inject.Inject;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Mappings;
import org.springframework.core.convert.ConversionService;

import com.sequenceiq.cloudbreak.api.model.SecretResponse;
import com.sequenceiq.cloudbreak.api.endpoint.v4.proxies.requests.ProxyV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.proxies.responses.ProxyV4Response;
import com.sequenceiq.cloudbreak.domain.ProxyConfig;

@Mapper(componentModel = "spring")
public abstract class ProxyConfigMapper {

    @Inject
    private ConversionService conversionService;

    protected ConversionService getConversionService() {
        return conversionService;
    }

    protected Class<SecretResponse> getClazz() {
        return SecretResponse.class;
    }

    @Mappings({
            @Mapping(target = "id", ignore = true),
            @Mapping(target = "workspace", ignore = true),
            @Mapping(target = "environments", ignore = true)
    })
    public abstract ProxyConfig mapRequestToEntity(ProxyV4Request proxyV4Request);

    @Mappings({
            @Mapping(target = "userName", expression = "java(getConversionService().convert(proxyConfig.getUserName(), getClazz()))"),
            @Mapping(target = "password", expression = "java(getConversionService().convert(proxyConfig.getPasswordSecret(), getClazz()))"),
            @Mapping(target = "environments", ignore = true)
    })
    public abstract ProxyV4Response mapEntityToResponse(ProxyConfig proxyConfig);

    public abstract Set<ProxyV4Response> mapEntityToResponse(Set<ProxyConfig> proxyConfigRequest);
}
