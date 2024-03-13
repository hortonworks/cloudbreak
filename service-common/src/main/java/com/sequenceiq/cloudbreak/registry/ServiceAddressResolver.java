package com.sequenceiq.cloudbreak.registry;

public interface ServiceAddressResolver {

    String resolveUrl(String serverUrl, String protocol, String serviceId);

    String resolveHostPort(String host, String port, String serviceId);

}