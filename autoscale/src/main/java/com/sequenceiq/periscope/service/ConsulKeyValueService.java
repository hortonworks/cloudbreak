package com.sequenceiq.periscope.service;

import static com.sequenceiq.periscope.api.model.ClusterState.RUNNING;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.ecwid.consul.v1.ConsulClient;
import com.sequenceiq.periscope.domain.Cluster;
import com.sequenceiq.periscope.domain.ClusterManager;
import com.sequenceiq.periscope.domain.PrometheusAlert;
import com.sequenceiq.periscope.model.TlsConfiguration;
import com.sequenceiq.periscope.service.security.TlsSecurityService;
import com.sequenceiq.periscope.utils.ConsulUtils;

@Service
public class ConsulKeyValueService {
    private static final Logger LOGGER = LoggerFactory.getLogger(ConsulKeyValueService.class);

    private static final String DEFAULT_ALERTING_CONSUL_KEY_PATH = "rules/alerting/";

    @Inject
    private TlsSecurityService tlsSecurityService;

    public PrometheusAlert addAlert(Cluster cluster, PrometheusAlert alert) {
        ClusterManager ambari = cluster.getClusterManager();
        try {
            if (RUNNING.equals(cluster.getState())) {
                TlsConfiguration tlsConfig = tlsSecurityService.getTls(cluster.getId());
                ConsulClient consulClient = ConsulUtils.createClient(ambari.getHost(), cluster.getPort(), tlsConfig);
                String alertKey = getKeyNameForAlert(alert);
                consulClient.setKVValue(alertKey, alert.getAlertRule());
                LOGGER.debug("Alert has been added to Consul KV store with name: '{}' on host: '{}'.", alertKey, ambari.getHost());
            }
        } catch (Exception e) {
            LOGGER.error("Alert could not be added to Consul KV store: {}", e.getMessage());
        }
        return alert;
    }

    public void deleteAlert(Cluster cluster, PrometheusAlert alert) {
        ClusterManager ambari = cluster.getClusterManager();
        try {
            TlsConfiguration tlsConfig = tlsSecurityService.getTls(cluster.getId());
            ConsulClient consulClient = ConsulUtils.createClient(ambari.getHost(), cluster.getPort(), tlsConfig);
            String alertKey = getKeyNameForAlert(alert);
            consulClient.deleteKVValue(alertKey);
            LOGGER.debug("Alert has been removed from Consul KV store with name: '{}' on host: '{}'.", alertKey, ambari.getHost());
        } catch (Exception e) {
            LOGGER.error("Alert could not be deleted from Consul KV store: {}", e.getMessage());
        }
    }

    private String getKeyNameForAlert(PrometheusAlert alert) {
        return DEFAULT_ALERTING_CONSUL_KEY_PATH + alert.getName();
    }
}
