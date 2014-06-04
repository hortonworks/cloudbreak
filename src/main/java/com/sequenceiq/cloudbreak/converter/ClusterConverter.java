package com.sequenceiq.cloudbreak.converter;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.controller.json.ClusterRequest;
import com.sequenceiq.cloudbreak.domain.Blueprint;
import com.sequenceiq.cloudbreak.domain.Cluster;
import com.sequenceiq.cloudbreak.domain.Status;
import com.sequenceiq.cloudbreak.repository.BlueprintRepository;

@Component
public class ClusterConverter {

    @Autowired
    private BlueprintRepository blueprintRepository;

    public Cluster convert(ClusterRequest clusterRequest) {
        Cluster cluster = new Cluster();
        Blueprint blueprint = blueprintRepository.findOne(clusterRequest.getBlueprintId());
        cluster.setBlueprint(blueprint);
        cluster.setName(clusterRequest.getClusterName());
        cluster.setStatus(Status.REQUESTED);
        return cluster;
    }

}
