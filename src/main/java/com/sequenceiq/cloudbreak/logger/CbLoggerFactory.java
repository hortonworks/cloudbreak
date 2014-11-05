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

    public static void buildMdcContext() {
        buildMdcContext(null);
    }

    public static void buildMdcContext(Object object) {
        if (object == null) {
            CloudBreakLoggerFactory.buildMdcContext();
            return;
        }
        if (object instanceof Credential) {
            CredentialLoggerFactory.buildMdcContext((Credential) object);
        } else if (object instanceof Stack) {
            StackLoggerFactory.buildMdcContext((Stack) object);
        } else if (object instanceof Cluster) {
            ClusterLoggerFactory.buildMdcContext((Cluster) object);
        } else if (object instanceof Template) {
            TemplateLoggerFactory.buildMdcContext((Template) object);
        } else if (object instanceof Blueprint) {
            BlueprintLoggerFactory.buildMdcContext((Blueprint) object);
        } else {
            throw new UnsupportedOperationException(String.format("%s class not supported for logging.", object.getClass()));
        }
    }
}
