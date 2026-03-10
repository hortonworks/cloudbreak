package com.sequenceiq.cloudbreak.cluster.model.resetjvmparams;

/**
 * Domain representation of a single config entry from a JVM parameter recalculation.
 * Mirrors the fields of {@code com.cloudera.api.swagger.model.ApiConfigRecord} that are
 * relevant to consumers, keeping CM-specific types confined to {@code cluster-cm}.
 */
public class JvmConfigRecord {

    /** Config parameter name. */
    private String name;

    /** Config parameter value. */
    private String value;

    /** Name of the role config group that owns this config. */
    private String roleConfigGroupName;

    /** Name of the cluster the owning service belongs to. */
    private String clusterName;

    /** Name of the service that owns this config. */
    private String serviceName;

    /** Indicates whether this config will be changed by the recalculation. */
    private JvmConfigApplicability applicability;

    public JvmConfigRecord() {
    }

    public JvmConfigRecord(String name, String value, String roleConfigGroupName,
            String clusterName, String serviceName, JvmConfigApplicability applicability) {
        this.name = name;
        this.value = value;
        this.roleConfigGroupName = roleConfigGroupName;
        this.clusterName = clusterName;
        this.serviceName = serviceName;
        this.applicability = applicability;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public String getRoleConfigGroupName() {
        return roleConfigGroupName;
    }

    public void setRoleConfigGroupName(String roleConfigGroupName) {
        this.roleConfigGroupName = roleConfigGroupName;
    }

    public String getClusterName() {
        return clusterName;
    }

    public void setClusterName(String clusterName) {
        this.clusterName = clusterName;
    }

    public String getServiceName() {
        return serviceName;
    }

    public void setServiceName(String serviceName) {
        this.serviceName = serviceName;
    }

    public JvmConfigApplicability getApplicability() {
        return applicability;
    }

    public void setApplicability(JvmConfigApplicability applicability) {
        this.applicability = applicability;
    }

    @Override
    public String toString() {
        return "JvmConfigRecord{" +
                "name='" + name + '\'' +
                ", value='" + value + '\'' +
                ", roleConfigGroupName='" + roleConfigGroupName + '\'' +
                ", clusterName='" + clusterName + '\'' +
                ", serviceName='" + serviceName + '\'' +
                ", applicability=" + applicability +
                '}';
    }
}
