package com.sequenceiq.cloudbreak.orchestrator;

public interface ExitCriteria {

    boolean isExitNeeded(ExitCriteriaModel exitCriteriaModel);

    String exitMessage();
}
