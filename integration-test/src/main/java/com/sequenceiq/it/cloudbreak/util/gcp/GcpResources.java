package com.sequenceiq.it.cloudbreak.util.gcp;

public enum GcpResources {

    GCP_INSTANCE("compute.googleapis.com/Instance"),
    GCP_DISK("compute.googleapis.com/Disk"),
    GCP_VOLUMESET("compute.googleapis.com/Disk"),
    GCP_ATTACHED_DISK("compute.googleapis.com/Disk"),
    GCP_ATTACHED_DISKSET("compute.googleapis.com/Disk"),
    GCP_INSTANCE_GROUP("compute.googleapis.com/InstanceGroup"),

    GCP_NETWORK("compute.googleapis.com/Network"),
    GCP_SECURITY_GROUP("compute.googleapis.com/Firewall"),
    GCP_SUBNET("compute.googleapis.com/Subnetwork"),
    GCP_RESERVED_IP("compute.googleapis.com/Address"),
    GCP_FORWARDING_RULE("compute.googleapis.com/ForwardingRule"),

    GCP_POSTGRESQL_INSTANCE("sqladmin.googleapis.com/Instance");

    private final String resourceType;

    GcpResources(String resourceType) {
        this.resourceType = resourceType;
    }

    public String getResourceType() {
        return resourceType;
    }
}