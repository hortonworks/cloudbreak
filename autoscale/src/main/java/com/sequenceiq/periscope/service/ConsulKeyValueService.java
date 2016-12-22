package com.sequenceiq.periscope.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class ConsulKeyValueService {
    private static final String DEFAULT_ALERTING_CONSUL_KEY_PATH = "rules/alerting/";

    @Value("${periscope.alerts.consul.key.path:"+ DEFAULT_ALERTING_CONSUL_KEY_PATH +"}")
    private String alertsKeyPath;


}
