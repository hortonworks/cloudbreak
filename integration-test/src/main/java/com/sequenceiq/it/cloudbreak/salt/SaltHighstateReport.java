package com.sequenceiq.it.cloudbreak.salt;

import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class SaltHighstateReport {
    private String jid;

    private Map<String, List<SaltStateReport>> instances;

    public SaltHighstateReport(String jid, Map<String, List<SaltStateReport>> instances) {
        this.jid = jid;
        this.instances = instances;
    }

    public String getJid() {
        return jid;
    }

    public Map<String, List<SaltStateReport>> getInstances() {
        return instances;
    }

    @Override
    public String toString() {
        return "SaltHighstateReport{" +
                "jid='" + jid + '\'' +
                ", instances=" + instances +
                '}';
    }
}
