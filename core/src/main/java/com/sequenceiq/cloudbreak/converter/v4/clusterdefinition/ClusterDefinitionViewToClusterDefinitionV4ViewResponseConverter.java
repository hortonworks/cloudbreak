package com.sequenceiq.cloudbreak.converter.v4.clusterdefinition;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.clusterdefinition.responses.ClusterDefinitionV4ViewResponse;
import com.sequenceiq.cloudbreak.converter.CompactViewToCompactViewResponseConverter;
import com.sequenceiq.cloudbreak.domain.view.ClusterDefinitionView;

@Component
public class ClusterDefinitionViewToClusterDefinitionV4ViewResponseConverter
        extends CompactViewToCompactViewResponseConverter<ClusterDefinitionView, ClusterDefinitionV4ViewResponse> {
    @Override
    public ClusterDefinitionV4ViewResponse convert(ClusterDefinitionView entity) {
        ClusterDefinitionV4ViewResponse clusterDefinitionJson = super.convert(entity);
        clusterDefinitionJson.setStackType(entity.getStackType());
        clusterDefinitionJson.setStackVersion(entity.getStackVersion());
        clusterDefinitionJson.setHostGroupCount(entity.getHostGroupCount());
        clusterDefinitionJson.setStatus(entity.getStatus());
        clusterDefinitionJson.setTags(entity.getTags().getMap());
        return clusterDefinitionJson;
    }

    @Override
    protected ClusterDefinitionV4ViewResponse createTarget() {
        return new ClusterDefinitionV4ViewResponse();
    }
}
