package com.sequenceiq.periscope.rest.converter;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.periscope.api.model.ClusterRequestJson;
import com.sequenceiq.periscope.api.model.MetricAlertJson;
import com.sequenceiq.periscope.api.model.PrometheusAlertJson;
import com.sequenceiq.periscope.api.model.ScalingConfigurationJson;
import com.sequenceiq.periscope.api.model.TimeAlertJson;
import com.sequenceiq.periscope.domain.Cluster;
import com.sequenceiq.periscope.domain.MetricAlert;
import com.sequenceiq.periscope.domain.PrometheusAlert;
import com.sequenceiq.periscope.domain.TimeAlert;

@Component
public class ClusterRequestConverter extends AbstractConverter<ClusterRequestJson, Cluster> {

    @Inject
    private MetricAlertConverter metricAlertConverter;

    @Inject
    private TimeAlertConverter timeAlertConverter;

    @Inject
    private PrometheusAlertConverter prometheusAlertConverter;

    @Override
    public Cluster convert(ClusterRequestJson source) {
        Cluster cluster = new Cluster();
        cluster.setStackId(source.getStackId());
        cluster.setAutoscalingEnabled(source.enableAutoscaling());

        List<MetricAlertJson> metricAlertJsons = source.getMetricAlerts();
        if (metricAlertJsons != null && !metricAlertJsons.isEmpty()) {
            Set<MetricAlert> alerts = metricAlertJsons.stream()
                    .map(metricAlertJson -> {
                        MetricAlert alert = metricAlertConverter.convert(metricAlertJson);
                        alert.setCluster(cluster);
                        return alert;
                    }).collect(Collectors.toSet());
            cluster.setMetricAlerts(alerts);
        }

        List<TimeAlertJson> timeAlertJsons = source.getTimeAlerts();
        if (timeAlertJsons != null && !timeAlertJsons.isEmpty()) {
            Set<TimeAlert> alerts = timeAlertJsons.stream()
                    .map(timeAlertJson -> {
                        TimeAlert alert = timeAlertConverter.convert(timeAlertJson);
                        alert.setCluster(cluster);
                        return alert;
                    }).collect(Collectors.toSet());
            cluster.setTimeAlerts(alerts);
        }

        List<PrometheusAlertJson> prometheusAlertJsons = source.getPrometheusAlerts();
        if (prometheusAlertJsons != null && !prometheusAlertJsons.isEmpty()) {
            Set<PrometheusAlert> alerts = prometheusAlertJsons.stream()
                    .map(prometheusAlertJson -> {
                        PrometheusAlert alert = prometheusAlertConverter.convert(prometheusAlertJson);
                        alert.setCluster(cluster);
                        return alert;
                    }).collect(Collectors.toSet());
            cluster.setPrometheusAlerts(alerts);
        }

        ScalingConfigurationJson scalingConfiguration = source.getScalingConfiguration();
        if (scalingConfiguration != null) {
            cluster.setMinSize(scalingConfiguration.getMinSize());
            cluster.setMaxSize(scalingConfiguration.getMaxSize());
            cluster.setCoolDown(scalingConfiguration.getCoolDown());
        }

        return cluster;
    }
}
