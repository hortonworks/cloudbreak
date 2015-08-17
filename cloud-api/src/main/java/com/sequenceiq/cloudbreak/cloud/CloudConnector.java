package com.sequenceiq.cloudbreak.cloud;

public interface CloudConnector extends CloudPlatformAware {

    Authenticator authentication();

    Setup setup();

    CredentialConnector credentials();

    ResourceConnector resources();

    InstanceConnector instances();

    String sshUser();
}
