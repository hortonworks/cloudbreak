package com.sequenceiq.cloudbreak.core.flow2.chain.util;

import java.util.Set;

@FunctionalInterface
public interface RepairServiceRestartPolicy {

    boolean restartServicesNeeded(Set<String> repairedHostGroups);
}
