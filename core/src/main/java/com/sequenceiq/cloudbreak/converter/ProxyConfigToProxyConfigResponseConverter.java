package com.sequenceiq.cloudbreak.converter;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.model.proxy.ProxyConfigResponse;
import com.sequenceiq.cloudbreak.api.model.users.WorkspaceResourceResponse;
import com.sequenceiq.cloudbreak.domain.ProxyConfig;

@Component
public class ProxyConfigToProxyConfigResponseConverter extends AbstractConversionServiceAwareConverter<ProxyConfig, ProxyConfigResponse> {

    @Override
    public ProxyConfigResponse convert(ProxyConfig source) {
        ProxyConfigResponse response = new ProxyConfigResponse();
        response.setId(source.getId());
        response.setName(source.getName());
        response.setWorkspace(getConversionService().convert(source.getWorkspace(), WorkspaceResourceResponse.class));
        response.setDescription(source.getDescription());
        response.setProtocol(source.getProtocol());
        response.setServerHost(source.getServerHost());
        response.setServerPort(source.getServerPort());
        response.setUserName(source.getUserName());
        return response;
    }
}
