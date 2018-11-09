package com.sequenceiq.cloudbreak.converter.mapper;

import com.sequenceiq.cloudbreak.api.model.proxy.ProxyConfigRequest;
import com.sequenceiq.cloudbreak.api.model.proxy.ProxyConfigResponse;
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
public class ProxyConfigMapperImpl implements ProxyConfigMapper {

    @Override
    public ProxyConfig mapRequestToEntity(ProxyConfigRequest proxyConfigRequest) {
        if ( proxyConfigRequest == null ) {
            return null;
        }

        ProxyConfig proxyConfig = new ProxyConfig();

        proxyConfig.setName( proxyConfigRequest.getName() );
        proxyConfig.setServerHost( proxyConfigRequest.getServerHost() );
        proxyConfig.setServerPort( proxyConfigRequest.getServerPort() );
        proxyConfig.setProtocol( proxyConfigRequest.getProtocol() );
        proxyConfig.setDescription( proxyConfigRequest.getDescription() );

        proxyConfig.setPassword( new com.sequenceiq.cloudbreak.domain.Secret(proxyConfigRequest.getPassword()) );
        proxyConfig.setUserName( new com.sequenceiq.cloudbreak.domain.Secret(proxyConfigRequest.getUserName()) );

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
        proxyConfigResponse.setDescription( proxyConfigRequest.getDescription() );
        proxyConfigResponse.setId( proxyConfigRequest.getId() );
        proxyConfigResponse.setWorkspace( workspaceToWorkspaceResourceResponse( proxyConfigRequest.getWorkspace() ) );

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
