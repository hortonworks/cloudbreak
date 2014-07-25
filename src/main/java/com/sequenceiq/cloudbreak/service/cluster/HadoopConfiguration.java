package com.sequenceiq.cloudbreak.service.cluster;

import java.util.Arrays;
import java.util.List;

public enum HadoopConfiguration {

    YARN_SITE("yarn-site", HadoopProperty.AM_PORT_RANGE);

    private final String key;
    private final List<HadoopProperty> properties;

    private HadoopConfiguration(String key, HadoopProperty... properties) {
        this.properties = Arrays.asList(properties);
        this.key = key;
    }

    public String getKey() {
        return key;
    }

    public List<HadoopProperty> getProperties() {
        return properties;
    }

    public static enum HadoopProperty {

        AM_PORT_RANGE("yarn.app.mapreduce.am.job.client.port-range", "40000-40050");

        private final String key;
        private final String value;

        private HadoopProperty(String key, String value) {
            this.key = key;
            this.value = value;
        }

        public String getKey() {
            return key;
        }

        public String getValue() {
            return value;
        }
    }

}
