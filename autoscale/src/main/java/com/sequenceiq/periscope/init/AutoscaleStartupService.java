package com.sequenceiq.periscope.init;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import com.sequenceiq.ambari.client.AmbariClient;
import com.sequenceiq.periscope.domain.Cluster;
import com.sequenceiq.periscope.domain.MetricAlert;
import com.sequenceiq.periscope.repository.ClusterRepository;
import com.sequenceiq.periscope.service.AmbariClientProvider;

@Component
public class AutoscaleStartupService implements ApplicationListener<ContextRefreshedEvent> {
    @Inject
    private ClusterRepository clusterRepository;

    @Inject
    private AmbariClientProvider ambariClientProvider;

    @Override
    public void onApplicationEvent(ContextRefreshedEvent event) {
        Set<Cluster> clusters = clusterRepository.findClustersWhereMetricAlertLabelIsMissing();
        for (Cluster cluster : clusters) {
            Map<String, String> alertDefinitionMap = cluster.isRunning() ? getAlertDefinitionMap(cluster) : Collections.emptyMap();
            for (MetricAlert alert : cluster.getMetricAlerts()) {
                if (StringUtils.isEmpty(alert.getDefinitionLabel()) || alert.getDefinitionName().equals(alert.getDefinitionLabel())) {
                    alert.setDefinitionLabel(alertDefinitionMap.getOrDefault(alert.getDefinitionName(), alert.getDefinitionName()));
                }
            }
        }
        clusterRepository.saveAll(clusters);
    }

    private Map<String, String> getAlertDefinitionMap(Cluster cluster) {
        try {
            AmbariClient ambariClient = ambariClientProvider.createAmbariClient(cluster);
            List<Map<String, String>> alertDefinitions = ambariClient.getAlertDefinitions();
            return alertDefinitions.stream().collect(Collectors.toMap(e -> e.get("name"), e -> e.get("label")));
        } catch (Exception ex) {
            return Collections.emptyMap();
        }
    }

    private boolean isMetricAlertMigrationNeeded(Cluster cluster) {
        return cluster.getMetricAlerts().stream()
                .anyMatch(ma -> StringUtils.isEmpty(ma.getDefinitionLabel()) || ma.getDefinitionName().equals(ma.getDefinitionLabel()));
    }
}
