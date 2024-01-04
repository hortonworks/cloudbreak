package com.sequenceiq.cloudbreak.common.metrics.status;

public interface StackCountByStatusView {

    String getStatus();

    int getCount();
}
