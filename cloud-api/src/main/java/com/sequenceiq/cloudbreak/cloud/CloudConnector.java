package com.sequenceiq.cloudbreak.cloud;

public interface CloudConnector extends CloudPlatformVariantAware {

    Authenticator authentication();

    Setup setup();

    CredentialConnector credentials();

    ResourceConnector resources();

    InstanceConnector instances();

    MetadataCollector metadata();

    PlatformParameters parameters();

}
