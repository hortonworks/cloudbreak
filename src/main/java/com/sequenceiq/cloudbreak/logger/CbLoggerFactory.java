package com.sequenceiq.cloudbreak.logger;

import com.sequenceiq.cloudbreak.domain.Blueprint;
import com.sequenceiq.cloudbreak.domain.Cluster;
import com.sequenceiq.cloudbreak.domain.Credential;
import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.domain.Template;
import com.sequenceiq.cloudbreak.logger.resourcetype.BlueprintLoggerFactory;
import com.sequenceiq.cloudbreak.logger.resourcetype.CloudBreakLoggerFactory;
import com.sequenceiq.cloudbreak.logger.resourcetype.ClusterLoggerFactory;
import com.sequenceiq.cloudbreak.logger.resourcetype.CredentialLoggerFactory;
import com.sequenceiq.cloudbreak.logger.resourcetype.StackLoggerFactory;
import com.sequenceiq.cloudbreak.logger.resourcetype.TemplateLoggerFactory;

public class CbLoggerFactory {

    private CbLoggerFactory() {

    }

    public static void buildMdvContext() {
        buildMdvContext(null);
    }

    public static void buildMdvContext(Object object) {
        if (object == null) {
            CloudBreakLoggerFactory.buildMdvContext();
            return;
        }
        if (object instanceof Credential) {
            CredentialLoggerFactory.buildMdvContext((Credential) object);
        } else if (object instanceof Stack) {
            StackLoggerFactory.buildMdvContext((Stack) object);
        } else if (object instanceof Cluster) {
            ClusterLoggerFactory.buildMdvContext((Cluster) object);
        } else if (object instanceof Template) {
            TemplateLoggerFactory.buildMdvContext((Template) object);
        } else if (object instanceof Blueprint) {
            BlueprintLoggerFactory.buildMdvContext((Blueprint) object);
        } else {
            throw new UnsupportedOperationException(String.format("%s class not supported for logging.", object.getClass()));
        }
    }
}
