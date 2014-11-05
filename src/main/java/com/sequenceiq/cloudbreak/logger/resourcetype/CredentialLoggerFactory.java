package com.sequenceiq.cloudbreak.logger.resourcetype;

import org.slf4j.MDC;

import com.sequenceiq.cloudbreak.domain.Credential;
import com.sequenceiq.cloudbreak.logger.LoggerContextKey;
import com.sequenceiq.cloudbreak.logger.LoggerResourceType;

public class CredentialLoggerFactory {

    private CredentialLoggerFactory() {

    }

    public static void buildMdvContext(Credential credential) {
        if (credential.getOwner() != null) {
            MDC.put(LoggerContextKey.OWNER_ID.toString(), credential.getOwner());
        }
        MDC.put(LoggerContextKey.RESOURCE_TYPE.toString(), LoggerResourceType.CREDENTIAL.toString());
        if (credential.getId() == null) {
            MDC.put(LoggerContextKey.RESOURCE_NAME.toString(), credential.getName().toString());
        } else {
            MDC.put(LoggerContextKey.RESOURCE_ID.toString(), credential.getId().toString());
        }
    }
}
