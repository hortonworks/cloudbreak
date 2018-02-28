package com.sequenceiq.cloudbreak.converter.mapper;

import java.util.Set;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Mappings;

import com.sequenceiq.cloudbreak.api.model.proxy.ProxyConfigRequest;
import com.sequenceiq.cloudbreak.api.model.proxy.ProxyConfigResponse;
import com.sequenceiq.cloudbreak.common.model.user.IdentityUser;
import com.sequenceiq.cloudbreak.domain.ProxyConfig;

@Mapper
public interface ProxyConfigMapper {

    @Mappings({
            @Mapping(source = "user.userId", target = "owner"),
            @Mapping(target = "id", ignore = true)
    })
    ProxyConfig mapRequestToEntity(ProxyConfigRequest proxyConfigRequest, IdentityUser user, boolean publicInAccount);

    ProxyConfigResponse mapEntityToResponse(ProxyConfig proxyConfigRequest);

    Set<ProxyConfigResponse> mapEntityToResponse(Set<ProxyConfig> proxyConfigRequest);
}
