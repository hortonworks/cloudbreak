package com.sequenceiq.environment.configuration.registry;

public interface ServiceAddressResolver {

    String resolveUrl(String serverUrl, String protocol, String serviceId);

    String resolveHostPort(String host, String port, String serviceId);

}
