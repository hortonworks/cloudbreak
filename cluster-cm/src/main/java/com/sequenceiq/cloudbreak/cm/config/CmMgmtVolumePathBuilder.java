package com.sequenceiq.cloudbreak.cm.config;

import static com.sequenceiq.cloudbreak.template.VolumeUtils.buildSingleVolumePath;

import java.util.Arrays;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cluster.model.ServiceLocation;
import com.sequenceiq.cloudbreak.cluster.model.ServiceLocationMap;

@Component
public class CmMgmtVolumePathBuilder {

    private static final int FIRST_VOLUME_INDEX = 1;

    public ServiceLocationMap buildServiceLocationMap() {
        return buildServiceLocationMap(FIRST_VOLUME_INDEX);
    }

    public ServiceLocationMap buildServiceLocationMap(int volumeIndex) {
        ServiceLocationMap serviceLocations = new ServiceLocationMap();
        Arrays.stream(MgmtServices.values()).forEach(service ->
                serviceLocations.add(new ServiceLocation(service.name(), buildSingleVolumePath(volumeIndex, volumeIndex, service.getDirectory()))));
        return serviceLocations;
    }
}
