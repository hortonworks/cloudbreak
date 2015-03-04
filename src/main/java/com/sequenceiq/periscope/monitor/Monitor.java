package com.sequenceiq.periscope.monitor;

import java.util.Map;

import org.quartz.Job;

import com.sequenceiq.periscope.domain.Cluster;

public interface Monitor extends Job {

    String getIdentifier();

    String getTriggerExpression();

    Class getRequestType();

    Map<String, Object> getRequestContext(Cluster cluster);

}
