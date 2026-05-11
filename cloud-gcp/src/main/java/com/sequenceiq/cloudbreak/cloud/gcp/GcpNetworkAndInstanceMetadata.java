package com.sequenceiq.cloudbreak.cloud.gcp;


import com.google.api.services.compute.model.NetworkInterface;

public record GcpNetworkAndInstanceMetadata(NetworkInterface networkInterface, String instanceType) {
}
