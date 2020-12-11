package com.sequenceiq.it.cloudbreak.util.wait.service;

import java.util.Map;

public interface WaitObject {

    void fetchData();

    boolean isDeleteFailed();

    Map<String, String> actualStatuses();

    Map<String, String> actualStatusReason();

    Map<String, String> getDesiredStatuses();

    String getName();

    boolean isDeleted();

    boolean isFailed();

    boolean isDeletionInProgress();

    boolean isCreateFailed();

    boolean isDeletionCheck();

    boolean isFailedCheck();
}
