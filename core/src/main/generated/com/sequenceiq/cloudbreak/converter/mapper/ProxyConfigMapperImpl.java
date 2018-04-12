package com.sequenceiq.cloudbreak.converter.mapper;

import java.util.HashSet;
import java.util.Set;

import javax.annotation.Generated;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.model.proxy.ProxyConfigRequest;
import com.sequenceiq.cloudbreak.api.model.proxy.ProxyConfigResponse;
import com.sequenceiq.cloudbreak.domain.ProxyConfig;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor"
)
@Component
public class ProxyConfigMapperImpl implements ProxyConfigMapper {

    @Override
    public ProxyConfig mapRequestToEntity(ProxyConfigRequest proxyConfigRequest, boolean publicInAccount) {
        if ( proxyConfigRequest == null ) {
            return null;
        }

        ProxyConfig proxyConfig = new ProxyConfig();

        if ( proxyConfigRequest != null ) {
            proxyConfig.setName( proxyConfigRequest.getName() );
            proxyConfig.setServerHost( proxyConfigRequest.getServerHost() );
            proxyConfig.setServerPort( proxyConfigRequest.getServerPort() );
            proxyConfig.setProtocol( proxyConfigRequest.getProtocol() );
            proxyConfig.setUserName( proxyConfigRequest.getUserName() );
            proxyConfig.setPassword( proxyConfigRequest.getPassword() );
            proxyConfig.setDescription( proxyConfigRequest.getDescription() );
        }
        proxyConfig.setPublicInAccount( publicInAccount );

        return proxyConfig;
    }

    @Override
    public ProxyConfigResponse mapEntityToResponse(ProxyConfig proxyConfigRequest) {
        if ( proxyConfigRequest == null ) {
            return null;
        }

        ProxyConfigResponse proxyConfigResponse = new ProxyConfigResponse();

        proxyConfigResponse.setName( proxyConfigRequest.getName() );
        proxyConfigResponse.setServerHost( proxyConfigRequest.getServerHost() );
        proxyConfigResponse.setServerPort( proxyConfigRequest.getServerPort() );
        proxyConfigResponse.setProtocol( proxyConfigRequest.getProtocol() );
        proxyConfigResponse.setUserName( proxyConfigRequest.getUserName() );
        proxyConfigResponse.setDescription( proxyConfigRequest.getDescription() );
        proxyConfigResponse.setId( proxyConfigRequest.getId() );

        return proxyConfigResponse;
    }

    @Override
    public Set<ProxyConfigResponse> mapEntityToResponse(Set<ProxyConfig> proxyConfigRequest) {
        if ( proxyConfigRequest == null ) {
            return null;
        }

        Set<ProxyConfigResponse> set = new HashSet<ProxyConfigResponse>( Math.max( (int) ( proxyConfigRequest.size() / .75f ) + 1, 16 ) );
        for ( ProxyConfig proxyConfig : proxyConfigRequest ) {
            set.add( mapEntityToResponse( proxyConfig ) );
        }

        return set;
    }
}
