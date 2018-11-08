package com.sequenceiq.cloudbreak.template.processor.kerberos;

import java.util.HashMap;
import java.util.Map;

public class KerberosServiceConfiguration {

    private Map<String, String> configurations;

    public KerberosServiceConfiguration() {
        configurations = new HashMap<>();
    }

    public KerberosServiceConfiguration(Map<String, String> configurations) {
        this.configurations = configurations;
    }

    public Map<String, String> getConfigurations() {
        return configurations;
    }

    public void setConfigurations(Map<String, String> configurations) {
        this.configurations = configurations;
    }
}
