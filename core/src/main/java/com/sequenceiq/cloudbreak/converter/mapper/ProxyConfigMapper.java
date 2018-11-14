package com.sequenceiq.cloudbreak.converter.mapper;

import java.util.Set;

import javax.inject.Inject;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Mappings;
import org.springframework.core.convert.ConversionService;

import com.sequenceiq.cloudbreak.api.model.SecretResponse;
import com.sequenceiq.cloudbreak.api.model.proxy.ProxyConfigRequest;
import com.sequenceiq.cloudbreak.api.model.proxy.ProxyConfigResponse;
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
            @Mapping(target = "owner", ignore = true),
            @Mapping(target = "account", ignore = true),
            @Mapping(target = "id", ignore = true),
            @Mapping(target = "workspace", ignore = true),
            @Mapping(target = "environments", ignore = true)
    })
    public abstract ProxyConfig mapRequestToEntity(ProxyConfigRequest proxyConfigRequest);

    @Mappings({
            @Mapping(target = "userName", expression = "java(getConversionService().convert(proxyConfig.getUserName(), getClazz()))"),
            @Mapping(target = "password", expression = "java(getConversionService().convert(proxyConfig.getPasswordSecret(), getClazz()))"),
            @Mapping(target = "environments", ignore = true)
    })
    public abstract ProxyConfigResponse mapEntityToResponse(ProxyConfig proxyConfig);

    public abstract Set<ProxyConfigResponse> mapEntityToResponse(Set<ProxyConfig> proxyConfigRequest);
}
