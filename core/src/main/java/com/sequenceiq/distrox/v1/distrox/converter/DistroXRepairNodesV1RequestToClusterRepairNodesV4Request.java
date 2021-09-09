package com.sequenceiq.distrox.v1.distrox.converter;

import org.springframework.stereotype.Component;

import com.sequenceiq.distrox.api.v1.distrox.model.DistroXRepairNodesV1Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.ClusterRepairNodesV4Request;

@Component
public class DistroXRepairNodesV1RequestToClusterRepairNodesV4Request {

    public ClusterRepairNodesV4Request convert(DistroXRepairNodesV1Request source) {
        ClusterRepairNodesV4Request clusterRepairNodesV4Request = new ClusterRepairNodesV4Request();
        clusterRepairNodesV4Request.setIds(source.getIds());
        clusterRepairNodesV4Request.setDeleteVolumes(source.isDeleteVolumes());
        return clusterRepairNodesV4Request;
    }
}
