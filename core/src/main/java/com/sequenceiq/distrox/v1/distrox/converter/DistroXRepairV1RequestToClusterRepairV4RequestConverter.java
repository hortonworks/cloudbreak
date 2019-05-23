package com.sequenceiq.distrox.v1.distrox.converter;

import org.springframework.stereotype.Component;

import com.sequenceiq.distrox.api.v1.distrox.model.DistroXRepairV1Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.ClusterRepairNodesV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.ClusterRepairV4Request;
import com.sequenceiq.cloudbreak.converter.AbstractConversionServiceAwareConverter;

@Component
public class DistroXRepairV1RequestToClusterRepairV4RequestConverter
        extends AbstractConversionServiceAwareConverter<DistroXRepairV1Request, ClusterRepairV4Request> {
    @Override
    public ClusterRepairV4Request convert(DistroXRepairV1Request source) {
        ClusterRepairV4Request clusterRepairV4Request = new ClusterRepairV4Request();
        clusterRepairV4Request.setNodes(getConversionService().convert(source.getNodes(), ClusterRepairNodesV4Request.class));
        clusterRepairV4Request.setHostGroups(source.getHostGroups());
        clusterRepairV4Request.setRemoveOnly(source.isRemoveOnly());
        return clusterRepairV4Request;
    }
}
