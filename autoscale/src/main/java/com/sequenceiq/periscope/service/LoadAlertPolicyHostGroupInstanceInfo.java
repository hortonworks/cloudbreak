package com.sequenceiq.periscope.service;

import java.util.List;
import java.util.Map;

import com.sequenceiq.periscope.domain.LoadAlert;

public class LoadAlertPolicyHostGroupInstanceInfo {

    private final LoadAlert loadAlert;

    private final String policyHostGroup;

    private final Map<String, String> hostFqdnsToInstanceId;

    private final List<String> servicesHealthyInstanceIds;

    private final List<String> stoppedHostInstanceIds;

    public LoadAlertPolicyHostGroupInstanceInfo(LoadAlert loadAlert, String policyHostGroup, Map<String, String> hostFqdnsToInstanceId,
            List<String> servicesHealthyInstanceIds, List<String> stoppedHostInstanceIds) {
        this.loadAlert = loadAlert;
        this.policyHostGroup = policyHostGroup;
        this.hostFqdnsToInstanceId = hostFqdnsToInstanceId;
        this.servicesHealthyInstanceIds = servicesHealthyInstanceIds;
        this.stoppedHostInstanceIds = stoppedHostInstanceIds;
    }

    public LoadAlert getLoadAlert() {
        return loadAlert;
    }

    public String getPolicyHostGroup() {
        return policyHostGroup;
    }

    public Map<String, String> getHostFqdnsToInstanceId() {
        return hostFqdnsToInstanceId;
    }

    public List<String> getServicesHealthyInstanceIds() {
        return servicesHealthyInstanceIds;
    }

    public List<String> getStoppedHostInstanceIds() {
        return stoppedHostInstanceIds;
    }
}
