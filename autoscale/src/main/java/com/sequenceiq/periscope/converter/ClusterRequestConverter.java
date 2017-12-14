package com.sequenceiq.periscope.converter;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.periscope.api.model.AutoscaleClusterRequest;
import com.sequenceiq.periscope.api.model.MetricAlertRequest;
import com.sequenceiq.periscope.api.model.PrometheusAlertRequest;
import com.sequenceiq.periscope.api.model.ScalingConfigurationRequest;
import com.sequenceiq.periscope.api.model.TimeAlertRequest;
import com.sequenceiq.periscope.domain.Cluster;
import com.sequenceiq.periscope.domain.MetricAlert;
import com.sequenceiq.periscope.domain.PrometheusAlert;
import com.sequenceiq.periscope.domain.TimeAlert;

@Component
public class ClusterRequestConverter extends AbstractConverter<AutoscaleClusterRequest, Cluster> {

    @Inject
    private MetricAlertRequestConverter metricAlertRequestConverter;

    @Inject
    private TimeAlertRequestConverter timeAlertRequestConverter;

    @Inject
    private PrometheusAlertRequestConverter prometheusAlertRequestConverter;

    @Override
    public Cluster convert(AutoscaleClusterRequest source) {
        Cluster cluster = new Cluster();
        cluster.setStackId(source.getStackId());
        cluster.setAutoscalingEnabled(source.enableAutoscaling());

        List<MetricAlertRequest> metricAlertResponses = source.getMetricAlerts();
        if (metricAlertResponses != null && !metricAlertResponses.isEmpty()) {
            Set<MetricAlert> alerts = metricAlertResponses.stream()
                    .map(metricAlertJson -> {
                        MetricAlert alert = metricAlertRequestConverter.convert(metricAlertJson);
                        alert.setCluster(cluster);
                        return alert;
                    }).collect(Collectors.toSet());
            cluster.setMetricAlerts(alerts);
        }

        List<TimeAlertRequest> timeAlertRequests = source.getTimeAlerts();
        if (timeAlertRequests != null && !timeAlertRequests.isEmpty()) {
            Set<TimeAlert> alerts = timeAlertRequests.stream()
                    .map(timeAlertJson -> {
                        TimeAlert alert = timeAlertRequestConverter.convert(timeAlertJson);
                        alert.setCluster(cluster);
                        return alert;
                    }).collect(Collectors.toSet());
            cluster.setTimeAlerts(alerts);
        }

        List<PrometheusAlertRequest> prometheusAlertRequests = source.getPrometheusAlerts();
        if (prometheusAlertRequests != null && !prometheusAlertRequests.isEmpty()) {
            Set<PrometheusAlert> alerts = prometheusAlertRequests.stream()
                    .map(prometheusAlertJson -> {
                        PrometheusAlert alert = prometheusAlertRequestConverter.convert(prometheusAlertJson);
                        alert.setCluster(cluster);
                        return alert;
                    }).collect(Collectors.toSet());
            cluster.setPrometheusAlerts(alerts);
        }

        ScalingConfigurationRequest scalingConfiguration = source.getScalingConfiguration();
        if (scalingConfiguration != null) {
            cluster.setMinSize(scalingConfiguration.getMinSize());
            cluster.setMaxSize(scalingConfiguration.getMaxSize());
            cluster.setCoolDown(scalingConfiguration.getCoolDown());
        }

        return cluster;
    }
}
