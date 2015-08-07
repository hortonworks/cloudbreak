package com.sequenceiq.cloudbreak.cloud;

public interface CloudPlatformAware {

    String platform();

    String sshUser();
}
