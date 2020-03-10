package com.sequenceiq.periscope.converter;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.periscope.api.model.DistroXAutoscaleClusterResponse;
import com.sequenceiq.periscope.api.model.LoadAlertResponse;
import com.sequenceiq.periscope.api.model.TimeAlertResponse;
import com.sequenceiq.periscope.domain.Cluster;

@Component
public class DistroXAutoscaleClusterResponseConverter extends AbstractConverter<DistroXAutoscaleClusterResponse, Cluster> {

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

        List<TimeAlertResponse> timeAlertRequests =
                timeAlertResponseConverter.convertAllToJson(new ArrayList<>(source.getTimeAlerts()));
        json.setTimeAlerts(timeAlertRequests);

        List<LoadAlertResponse> loadAlertRequests =
                loadAlertResponseConverter.convertAllToJson(new ArrayList<>(source.getLoadAlerts()));
        json.setLoadAlerts(loadAlertRequests);

        return json;
    }
}
