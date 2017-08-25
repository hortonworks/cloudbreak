package com.sequenceiq.periscope.rest.converter;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.periscope.api.model.ClusterJson;
import com.sequenceiq.periscope.api.model.MetricAlertJson;
import com.sequenceiq.periscope.api.model.PrometheusAlertJson;
import com.sequenceiq.periscope.api.model.ScalingConfigurationJson;
import com.sequenceiq.periscope.api.model.TimeAlertJson;
import com.sequenceiq.periscope.domain.Cluster;

@Component
public class ClusterConverter extends AbstractConverter<ClusterJson, Cluster> {

    @Inject
    private MetricAlertConverter metricAlertConverter;

    @Inject
    private TimeAlertConverter timeAlertConverter;

    @Inject
    private PrometheusAlertConverter prometheusAlertConverter;

    @Override
    public ClusterJson convert(Cluster source) {
        ClusterJson json = new ClusterJson(
                source.getHost(),
                source.getPort(),
                source.getAmbariUser(),
                source.getStackId(),
                source.isAutoscalingEnabled(),
                source.getId(),
                source.getState().name());

        if (!source.getMetricAlerts().isEmpty()) {
            List<MetricAlertJson> metricAlerts = metricAlertConverter.convertAllToJson(new ArrayList<>(source.getMetricAlerts()));
            json.setMetricAlerts(metricAlerts);
        }

        if (!source.getTimeAlerts().isEmpty()) {
            List<TimeAlertJson> timeAlertJsons = timeAlertConverter.convertAllToJson(new ArrayList<>(source.getTimeAlerts()));
            json.setTimeAlerts(timeAlertJsons);
        }

        if (!source.getPrometheusAlerts().isEmpty()) {
            List<PrometheusAlertJson> prometheusAlertJsons = prometheusAlertConverter.convertAllToJson(new ArrayList<>(source.getPrometheusAlerts()));
            json.setPrometheusAlerts(prometheusAlertJsons);
        }

        ScalingConfigurationJson scalingConfig = new ScalingConfigurationJson(source.getMinSize(), source.getMaxSize(), source.getCoolDown());
        json.setScalingConfiguration(scalingConfig);

        return json;
    }

}
