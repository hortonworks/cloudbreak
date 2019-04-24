package com.sequenceiq.redbeams.logger;

import com.google.common.base.Strings;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.sift.Discriminator;

public class UniqueIdDiscriminator implements Discriminator<ILoggingEvent> {

    private static final String KEY = "nodeid";

    private final String value;

    private boolean started;

    public UniqueIdDiscriminator() {
        String nodeUuidEnvVar = System.getenv("REDBEAMS_INSTANCE_NODE_ID");
        String nodeUuidEnvProperty = System.getProperty("redbeams.instance.node.id");
        if (!Strings.isNullOrEmpty(nodeUuidEnvVar)) {
            value = nodeUuidEnvVar;
        } else if (!Strings.isNullOrEmpty(nodeUuidEnvProperty)) {
            value = nodeUuidEnvProperty;
        } else {
            value = "default";
        }
    }

    @Override
    public String getDiscriminatingValue(ILoggingEvent iLoggingEvent) {
        return value;
    }

    @Override
    public String getKey() {
        return KEY;
    }

    @Override
    public void start() {
        started = true;
    }

    @Override
    public void stop() {
        started = false;
    }

    @Override
    public boolean isStarted() {
        return started;
    }
}
