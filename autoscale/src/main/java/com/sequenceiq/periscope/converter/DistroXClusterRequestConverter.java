package com.sequenceiq.periscope.converter;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.periscope.api.model.DistroXAutoscaleClusterRequest;
import com.sequenceiq.periscope.api.model.ScalingConfigurationRequest;
import com.sequenceiq.periscope.domain.Cluster;
import com.sequenceiq.periscope.domain.LoadAlert;
import com.sequenceiq.periscope.domain.TimeAlert;

@Component
public class DistroXClusterRequestConverter extends AbstractConverter<DistroXAutoscaleClusterRequest, Cluster> {

    @Inject
    private TimeAlertRequestConverter timeAlertRequestConverter;

    @Inject
    private LoadAlertRequestConverter loadAlertRequestConverter;

    @Override
    public Cluster convert(DistroXAutoscaleClusterRequest source) {
        Cluster cluster = new Cluster();
        cluster.setAutoscalingEnabled(source.getEnableAutoscaling());
        cluster.setAutoscalingMode(source.getAutoScalingMode());

        if (source.getTimeAlertRequests() != null) {
            List<TimeAlert> timeAlerts =
                    timeAlertRequestConverter.convertAllFromJson(new ArrayList(source.getTimeAlertRequests()));
            cluster.setTimeAlerts(new HashSet<>(timeAlerts));
        }

        if (source.getLoadAlertRequests() != null) {
            List<LoadAlert> loadAlerts =
                    loadAlertRequestConverter.convertAllFromJson(new ArrayList(source.getLoadAlertRequests()));
            cluster.setLoadAlerts(new HashSet<>(loadAlerts));
        }

        ScalingConfigurationRequest scalingConfig = source.getScalingConfiguration();
        if (scalingConfig != null) {
            cluster.setMinSize(source.getScalingConfiguration().getMinSize());
            cluster.setMaxSize(source.getScalingConfiguration().getMaxSize());
            cluster.setCoolDown(source.getScalingConfiguration().getCoolDown());
        }
        return cluster;
    }
}
