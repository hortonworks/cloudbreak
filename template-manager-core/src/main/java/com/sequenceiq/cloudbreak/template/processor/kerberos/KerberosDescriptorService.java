package com.sequenceiq.cloudbreak.template.processor.kerberos;

import java.util.HashMap;
import java.util.Map;

public class KerberosDescriptorService {

    private String name;

    private Map<String, KerberosServiceConfiguration> configurations;

    public KerberosDescriptorService(String name) {
        this(name, new HashMap<>());
    }

    public KerberosDescriptorService(String name, Map<String, KerberosServiceConfiguration> configurations) {
        this.name = name;
        this.configurations = configurations;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Map<String, KerberosServiceConfiguration> getConfigurations() {
        return configurations;
    }

    public void setConfigurations(Map<String, KerberosServiceConfiguration> configurations) {
        this.configurations = configurations;
    }
}
