package com.sequenceiq.cloudbreak.converter.mapper;

import com.sequenceiq.cloudbreak.api.model.template.ClusterTemplateRequest;
import com.sequenceiq.cloudbreak.api.model.template.ClusterTemplateResponse;
import com.sequenceiq.cloudbreak.domain.stack.cluster.ClusterTemplate;
import java.util.HashSet;
import java.util.Set;
import javax.annotation.Generated;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor"
)
@Component
public class ClusterTemplateMapperImpl extends ClusterTemplateMapper {

    @Override
    public ClusterTemplate mapRequestToEntity(ClusterTemplateRequest clusterTemplateRequest) {
        if ( clusterTemplateRequest == null ) {
            return null;
        }

        ClusterTemplate clusterTemplate = new ClusterTemplate();

        clusterTemplate.setName( clusterTemplateRequest.getName() );
        clusterTemplate.setDescription( clusterTemplateRequest.getDescription() );
        clusterTemplate.setCloudPlatform( clusterTemplateRequest.getCloudPlatform() );

        clusterTemplate.setTemplate( mapStackV2RequestToJson(clusterTemplateRequest.getTemplate()) );
        clusterTemplate.setStatus( com.sequenceiq.cloudbreak.api.model.ResourceStatus.USER_MANAGED );

        return clusterTemplate;
    }

    @Override
    public ClusterTemplateResponse mapEntityToResponse(ClusterTemplate clusterTemplate) {
        if ( clusterTemplate == null ) {
            return null;
        }

        ClusterTemplateResponse clusterTemplateResponse = new ClusterTemplateResponse();

        clusterTemplateResponse.setName( clusterTemplate.getName() );
        clusterTemplateResponse.setDescription( clusterTemplate.getDescription() );
        clusterTemplateResponse.setTemplate( mapJsonToStackV2Request( clusterTemplate.getTemplate() ) );
        clusterTemplateResponse.setCloudPlatform( clusterTemplate.getCloudPlatform() );
        clusterTemplateResponse.setStatus( clusterTemplate.getStatus() );
        clusterTemplateResponse.setId( clusterTemplate.getId() );

        return clusterTemplateResponse;
    }

    @Override
    public Set<ClusterTemplateResponse> mapEntityToResponse(Set<ClusterTemplate> clusterTemplates) {
        if ( clusterTemplates == null ) {
            return null;
        }

        Set<ClusterTemplateResponse> set = new HashSet<ClusterTemplateResponse>( Math.max( (int) ( clusterTemplates.size() / .75f ) + 1, 16 ) );
        for ( ClusterTemplate clusterTemplate : clusterTemplates ) {
            set.add( mapEntityToResponse( clusterTemplate ) );
        }

        return set;
    }
}
