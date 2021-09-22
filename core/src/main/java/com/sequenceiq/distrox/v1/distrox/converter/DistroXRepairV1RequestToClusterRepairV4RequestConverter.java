package com.sequenceiq.distrox.v1.distrox.converter;

import static com.sequenceiq.cloudbreak.util.NullUtil.getIfNotNull;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.ClusterRepairV4Request;
import com.sequenceiq.distrox.api.v1.distrox.model.DistroXRepairV1Request;

@Component
public class DistroXRepairV1RequestToClusterRepairV4RequestConverter {

    @Inject
    private DistroXRepairNodesV1RequestToClusterRepairNodesV4Request distroXRepairNodesV1RequestToClusterRepairNodesV4Request;

    public ClusterRepairV4Request convert(DistroXRepairV1Request source) {
        ClusterRepairV4Request clusterRepairV4Request = new ClusterRepairV4Request();
        clusterRepairV4Request.setNodes(getIfNotNull(source.getNodes(), distroXRepairNodesV1RequestToClusterRepairNodesV4Request::convert));
        clusterRepairV4Request.setHostGroups(source.getHostGroups());
        clusterRepairV4Request.setRemoveOnly(source.isRemoveOnly());
        return clusterRepairV4Request;
    }
}
