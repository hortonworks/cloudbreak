package com.sequenceiq.cloudbreak.converter;

import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.controller.json.AmbariStackDetailsJson;
import com.sequenceiq.cloudbreak.controller.json.ClusterResponse;
import com.sequenceiq.cloudbreak.controller.json.HostGroupJson;
import com.sequenceiq.cloudbreak.domain.AmbariStackDetails;
import com.sequenceiq.cloudbreak.domain.Cluster;
import com.sequenceiq.cloudbreak.domain.HostGroup;
import com.sequenceiq.cloudbreak.domain.Status;

@Component
public class ClusterToJsonConverter extends AbstractConversionServiceAwareConverter<Cluster, ClusterResponse> {

    private static final int SECONDS_PER_MINUTE = 60;
    private static final int MILLIS_PER_SECOND = 1000;

    @Override
    public ClusterResponse convert(Cluster source) {
        ClusterResponse clusterResponse = new ClusterResponse();
        clusterResponse.setId(source.getId());
        clusterResponse.setStatus(source.getStatus().name());
        clusterResponse.setStatusReason(source.getStatusReason());
        if (source.getBlueprint() != null) {
            clusterResponse.setBlueprintId(source.getBlueprint().getId());
        }
        if (source.getUpSince() != null && Status.AVAILABLE.equals(source.getStatus())) {
            long now = new Date().getTime();
            long uptime = now - source.getUpSince();
            int minutes = (int) ((uptime / (MILLIS_PER_SECOND * SECONDS_PER_MINUTE)) % SECONDS_PER_MINUTE);
            int hours = (int) (uptime / (MILLIS_PER_SECOND * SECONDS_PER_MINUTE * SECONDS_PER_MINUTE));
            clusterResponse.setHoursUp(hours);
            clusterResponse.setMinutesUp(minutes);
        } else {
            clusterResponse.setHoursUp(0);
            clusterResponse.setMinutesUp(0);
        }
        AmbariStackDetails ambariStackDetails = source.getAmbariStackDetails();
        if (ambariStackDetails != null) {
            clusterResponse.setAmbariStackDetails(getConversionService().convert(ambariStackDetails, AmbariStackDetailsJson.class));
        }
        clusterResponse.setAmbariServerIp(source.getAmbariIp());
        clusterResponse.setUserName(source.getUserName());
        clusterResponse.setPassword(source.getPassword());
        clusterResponse.setDescription(source.getDescription() == null ? "" : source.getDescription());
        clusterResponse.setHostGroups(convertHostGroupsToJson(source.getHostGroups()));
        return clusterResponse;
    }

    private Set<HostGroupJson> convertHostGroupsToJson(Set<HostGroup> hostGroups) {
        Set<HostGroupJson> jsons = new HashSet<>();
        for (HostGroup hostGroup : hostGroups) {
            jsons.add(getConversionService().convert(hostGroup, HostGroupJson.class));
        }
        return jsons;
    }
}
