package com.sequenceiq.cloudbreak.orchestrator.state;

public interface ExitCriteria {

    boolean isExitNeeded(ExitCriteriaModel exitCriteriaModel);

    String exitMessage();
}
