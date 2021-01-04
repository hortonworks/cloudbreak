package com.sequenceiq.cloudbreak.cm.config;

import static com.sequenceiq.cloudbreak.template.VolumeUtils.buildSingleVolumePath;

import java.util.Arrays;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cluster.model.ServiceLocation;
import com.sequenceiq.cloudbreak.cluster.model.ServiceLocationMap;

@Component
public class CmMgmtVolumePathBuilder {

    public ServiceLocationMap buildServiceLocationMap() {
        ServiceLocationMap serviceLocations = new ServiceLocationMap();
        Arrays.stream(MgmtServices.values()).forEach(service ->
                serviceLocations.add(new ServiceLocation(service.name(), buildSingleVolumePath(1, service.getDirectory()))));
        return serviceLocations;
    }
}
