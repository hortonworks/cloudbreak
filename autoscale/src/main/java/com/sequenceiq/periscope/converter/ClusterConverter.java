package com.sequenceiq.periscope.converter;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.service.secret.SecretService;
import com.sequenceiq.periscope.api.model.AutoscaleClusterResponse;
import com.sequenceiq.periscope.api.model.MetricAlertResponse;
import com.sequenceiq.periscope.api.model.PrometheusAlertResponse;
import com.sequenceiq.periscope.api.model.ScalingConfigurationRequest;
import com.sequenceiq.periscope.api.model.TimeAlertResponse;
import com.sequenceiq.periscope.domain.Cluster;

@Component
public class ClusterConverter extends AbstractConverter<AutoscaleClusterResponse, Cluster> {

    @Inject
    private MetricAlertResponseConverter metricAlertResponseConverter;

    @Inject
    private TimeAlertResponseConverter timeAlertResponseConverter;

    @Inject
    private PrometheusAlertResponseConverter prometheusAlertResponseConverter;

    @Inject
    private SecretService secretService;

    @Override
    public AutoscaleClusterResponse convert(Cluster source) {
        AutoscaleClusterResponse json = new AutoscaleClusterResponse(
                source.getHost(),
                source.getPort(),
                secretService.get(source.getAmbariUser()),
                source.getStackId(),
                source.isAutoscalingEnabled(),
                source.getId(),
                source.getState().name());

        if (!source.getMetricAlerts().isEmpty()) {
            List<MetricAlertResponse> metricAlerts =
                    metricAlertResponseConverter.convertAllToJson(new ArrayList<>(source.getMetricAlerts()));
            json.setMetricAlerts(metricAlerts);
        }

        if (!source.getTimeAlerts().isEmpty()) {
            List<TimeAlertResponse> timeAlertRequests =
                    timeAlertResponseConverter.convertAllToJson(new ArrayList<>(source.getTimeAlerts()));
            json.setTimeAlerts(timeAlertRequests);
        }

        if (!source.getPrometheusAlerts().isEmpty()) {
            List<PrometheusAlertResponse> prometheusAlertRequests =
                    prometheusAlertResponseConverter.convertAllToJson(new ArrayList<>(source.getPrometheusAlerts()));
            json.setPrometheusAlerts(prometheusAlertRequests);
        }

        ScalingConfigurationRequest scalingConfig = new ScalingConfigurationRequest(source.getMinSize(), source.getMaxSize(), source.getCoolDown());
        json.setScalingConfiguration(scalingConfig);

        return json;
    }

}
