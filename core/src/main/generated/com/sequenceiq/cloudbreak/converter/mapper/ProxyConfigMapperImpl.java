package com.sequenceiq.cloudbreak.converter.mapper;

import com.sequenceiq.cloudbreak.api.endpoint.v4.proxies.requests.ProxyV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.proxies.responses.ProxyV4Response;
import com.sequenceiq.cloudbreak.api.model.users.WorkspaceResourceResponse;
import com.sequenceiq.cloudbreak.domain.ProxyConfig;
import com.sequenceiq.cloudbreak.domain.workspace.Workspace;
import java.util.HashSet;
import java.util.Set;
import javax.annotation.Generated;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor"
)
@Component
public class ProxyConfigMapperImpl extends ProxyConfigMapper {

    @Override
    public ProxyConfig mapRequestToEntity(ProxyV4Request proxyV4Request) {
        if ( proxyV4Request == null ) {
            return null;
        }

        ProxyConfig proxyConfig = new ProxyConfig();

        proxyConfig.setName( proxyV4Request.getName() );
        proxyConfig.setServerHost( proxyV4Request.getHost() );
        proxyConfig.setServerPort( proxyV4Request.getPort() );
        proxyConfig.setProtocol( proxyV4Request.getProtocol() );
        proxyConfig.setUserName( proxyV4Request.getUserName() );
        proxyConfig.setPassword( proxyV4Request.getPassword() );
        proxyConfig.setDescription( proxyV4Request.getDescription() );

        return proxyConfig;
    }

    @Override
    public ProxyV4Response mapEntityToResponse(ProxyConfig proxyConfig) {
        if ( proxyConfig == null ) {
            return null;
        }

        ProxyV4Response proxyV4Response = new ProxyV4Response();

        proxyV4Response.setName( proxyConfig.getName() );
        proxyV4Response.setHost( proxyConfig.getServerHost() );
        proxyV4Response.setPort( proxyConfig.getServerPort() );
        proxyV4Response.setProtocol( proxyConfig.getProtocol() );
        proxyV4Response.setDescription( proxyConfig.getDescription() );
        proxyV4Response.setId( proxyConfig.getId() );
        proxyV4Response.setWorkspace( workspaceToWorkspaceResourceResponse( proxyConfig.getWorkspace() ) );

        proxyV4Response.setPassword( getConversionService().convert(proxyConfig.getPasswordSecret(), getClazz()) );
        proxyV4Response.setUserName( getConversionService().convert(proxyConfig.getUserName(), getClazz()) );

        return proxyV4Response;
    }

    @Override
    public Set<ProxyV4Response> mapEntityToResponse(Set<ProxyConfig> proxyConfigRequest) {
        if ( proxyConfigRequest == null ) {
            return null;
        }

        Set<ProxyV4Response> set = new HashSet<ProxyV4Response>( Math.max( (int) ( proxyConfigRequest.size() / .75f ) + 1, 16 ) );
        for ( ProxyConfig proxyConfig : proxyConfigRequest ) {
            set.add( mapEntityToResponse( proxyConfig ) );
        }

        return set;
    }

    protected WorkspaceResourceResponse workspaceToWorkspaceResourceResponse(Workspace workspace) {
        if ( workspace == null ) {
            return null;
        }

        WorkspaceResourceResponse workspaceResourceResponse = new WorkspaceResourceResponse();

        workspaceResourceResponse.setId( workspace.getId() );
        workspaceResourceResponse.setName( workspace.getName() );

        return workspaceResourceResponse;
    }
}
