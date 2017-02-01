package com.sequenceiq.periscope.service;

import static com.sequenceiq.periscope.api.model.ClusterState.RUNNING;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.ecwid.consul.v1.ConsulClient;
import com.sequenceiq.periscope.domain.Ambari;
import com.sequenceiq.periscope.domain.Cluster;
import com.sequenceiq.periscope.domain.PrometheusAlert;
import com.sequenceiq.periscope.model.TlsConfiguration;
import com.sequenceiq.periscope.service.security.TlsSecurityService;
import com.sequenceiq.periscope.utils.ConsulUtils;

@Component
public class ConsulKeyValueService {
    private static final Logger LOGGER = LoggerFactory.getLogger(ConsulKeyValueService.class);

    private static final String DEFAULT_ALERTING_CONSUL_KEY_PATH = "rules/alerting/";

    @Value("${periscope.alerts.consul.key.path:" + DEFAULT_ALERTING_CONSUL_KEY_PATH + "}")
    private String alertsKeyPath;

    @Autowired
    private TlsSecurityService tlsSecurityService;

    public PrometheusAlert addAlert(Cluster cluster, PrometheusAlert alert) {
        Ambari ambari = cluster.getAmbari();
        try {
            if (RUNNING.equals(cluster.getState())) {
                TlsConfiguration tlsConfig = tlsSecurityService.getConfiguration(cluster);
                ConsulClient consulClient = ConsulUtils.createClient(ambari.getHost(), cluster.getPort(), tlsConfig);
                String alertKey = getKeyNameForAlert(alert);
                consulClient.setKVValue(alertKey, alert.getAlertRule());
                LOGGER.info("Alert has been added to Consul KV store with name: '{}' on host: '{}'.", alertKey, ambari.getHost());
            }
        } catch (Exception e) {
            LOGGER.warn("Alert could not be added to Consul KV store:", e);
        }
        return alert;
    }

    public PrometheusAlert deleteAlert(Cluster cluster, PrometheusAlert alert) {
        Ambari ambari = cluster.getAmbari();
        try {
            TlsConfiguration tlsConfig = tlsSecurityService.getConfiguration(cluster);
            ConsulClient consulClient = ConsulUtils.createClient(ambari.getHost(), cluster.getPort(), tlsConfig);
            String alertKey = getKeyNameForAlert(alert);
            consulClient.deleteKVValue(alertKey);
            LOGGER.info("Alert has been removed from Consul KV store with name: '{}' on host: '{}'.", alertKey, ambari.getHost());
        } catch (Exception e) {
            LOGGER.warn("Alert could not be deleted from Consul KV store:", e);
        }
        return alert;
    }

    private String getKeyNameForAlert(PrometheusAlert alert) {
        return DEFAULT_ALERTING_CONSUL_KEY_PATH + alert.getName();
    }
}
