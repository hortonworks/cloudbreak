package com.sequenceiq.cloudbreak.quartz.metric.statusmetric;

public interface StackCountByStatusView {

    String getStatus();

    int getCount();
}
