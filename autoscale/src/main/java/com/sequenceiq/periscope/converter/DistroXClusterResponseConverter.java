package com.sequenceiq.periscope.converter;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.periscope.api.model.DistroXAutoscaleClusterResponse;
import com.sequenceiq.periscope.api.model.LoadAlertResponse;
import com.sequenceiq.periscope.api.model.ScalingConfigurationRequest;
import com.sequenceiq.periscope.api.model.TimeAlertResponse;
import com.sequenceiq.periscope.domain.Cluster;

@Component
public class DistroXClusterResponseConverter extends AbstractConverter<DistroXAutoscaleClusterResponse, Cluster> {

    @Inject
    private TimeAlertResponseConverter timeAlertResponseConverter;

    @Inject
    private LoadAlertResponseConverter loadAlertResponseConverter;

    @Override
    public DistroXAutoscaleClusterResponse convert(Cluster source) {
        DistroXAutoscaleClusterResponse json = new DistroXAutoscaleClusterResponse(
                source.getStackCrn(),
                source.getStackName(),
                source.isAutoscalingEnabled(),
                source.getId(),
                source.getState());

        json.setStackType(source.getStackType());
        json.setStackCrn(source.getStackCrn());
        json.setStackName(source.getStackName());
        json.setAutoScalingMode(source.getAutoscalingMode());

        if (!source.getTimeAlerts().isEmpty()) {
            List<TimeAlertResponse> timeAlertRequests =
                    timeAlertResponseConverter.convertAllToJson(new ArrayList<>(source.getTimeAlerts()));
            json.setTimeAlerts(timeAlertRequests);
        }

        if (!source.getLoadAlerts().isEmpty()) {
            List<LoadAlertResponse> loadAlertRequests =
                    loadAlertResponseConverter.convertAllToJson(new ArrayList<>(source.getLoadAlerts()));
            json.setLoadAlerts(loadAlertRequests);
        }
        ScalingConfigurationRequest scalingConfig = new ScalingConfigurationRequest(source.getMinSize(), source.getMaxSize(), source.getCoolDown());
        json.setScalingConfiguration(scalingConfig);

        return json;
    }
}
