package com.sequenceiq.cloudbreak.logger;

public interface MdcContextInfoProvider {

    String getWorkspaceName();

    String getEnvironmentCrn();

    String getResourceCrn();

    String getResourceName();

    String getResourceType();

    String getTenantName();
}
