package com.sequenceiq.cloudbreak.cmtemplate.configproviders.kafka;

public class KafkaConfigs {

    public static final String RANGER_PLUGIN_KAFKA_SERVICE_NAME = "ranger_plugin_kafka_service_name";

    public static final String GENERATED_RANGER_SERVICE_NAME = "{{GENERATED_RANGER_SERVICE_NAME}}";

    static final String DEFAULT_REPLICATION_FACTOR = "default.replication.factor";

    static final String DELEGATION_TOKEN_ENABLE = "delegation.token.enable";

    static final String ENABLE_RACK_AWARENESS = "enable.rack.awareness";

    static final String KAFKA_DECOMMISSION_HOOK_ENABLED = "kafka.decommission.hook.enabled";

    static final String LDAP_AUTH_ENABLE = "ldap.auth.enable";

    static final String LDAP_AUTH_URL = "ldap.auth.url";

    static final String LDAP_AUTH_USER_DN_TEMPLATE = "ldap.auth.user.dn.template";

    static final String PRODUCER_METRICS_ENABLE = "producer.metrics.enable";

    static final String SASL_AUTH_METHOD = "sasl.plain.auth";

    private KafkaConfigs() { }
}
