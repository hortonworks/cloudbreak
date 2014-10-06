package com.sequenceiq.cloudbreak.converter;

import java.util.Date;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.controller.json.ClusterRequest;
import com.sequenceiq.cloudbreak.controller.json.ClusterResponse;
import com.sequenceiq.cloudbreak.controller.json.JsonHelper;
import com.sequenceiq.cloudbreak.domain.Cluster;
import com.sequenceiq.cloudbreak.domain.Status;
import com.sequenceiq.cloudbreak.repository.BlueprintRepository;

@Component
public class ClusterConverter {

    private static final int SECONDS_PER_MINUTE = 60;
    private static final int MILLIS_PER_SECOND = 1000;

    @Autowired
    private BlueprintRepository blueprintRepository;

    @Autowired
    private JsonHelper jsonHelper;

    public Cluster convert(ClusterRequest clusterRequest) {
        Cluster cluster = new Cluster();
        try {
            cluster.setBlueprint(blueprintRepository.findOne(clusterRequest.getBlueprintId()));
        } catch (AccessDeniedException e) {
            throw new AccessDeniedException(String.format("Access to blueprint '%s' is denied or blueprint doesn't exist.", clusterRequest.getBlueprintId()), e);
        }
        cluster.setName(clusterRequest.getName());
        cluster.setStatus(Status.REQUESTED);
        cluster.setDescription(clusterRequest.getDescription());
        cluster.setEmailNeeded(clusterRequest.getEmailNeeded());
        return cluster;
    }

    public ClusterResponse convert(Cluster cluster, String clusterJson) {
        ClusterResponse clusterResponse = new ClusterResponse();
        clusterResponse.setCluster(jsonHelper.createJsonFromString(clusterJson));
        clusterResponse.setId(cluster.getId());
        clusterResponse.setStatus(cluster.getStatus().name());
        if (cluster.getCreationFinished() != null) {
            long now = new Date().getTime();
            long createFinished = now - cluster.getCreationFinished();
            int minutes = (int) ((createFinished / (MILLIS_PER_SECOND * SECONDS_PER_MINUTE)) % SECONDS_PER_MINUTE);
            int hours = (int) (createFinished / (MILLIS_PER_SECOND * SECONDS_PER_MINUTE * SECONDS_PER_MINUTE));
            clusterResponse.setHoursUp(hours);
            clusterResponse.setMinutesUp(minutes);
        } else {
            clusterResponse.setHoursUp(0);
            clusterResponse.setMinutesUp(0);
        }
        clusterResponse.setStatusReason(cluster.getStatusReason());
        clusterResponse.setBlueprintId(cluster.getBlueprint().getId());
        clusterResponse.setDescription(cluster.getDescription() == null ? "" : cluster.getDescription());
        return clusterResponse;
    }

}
