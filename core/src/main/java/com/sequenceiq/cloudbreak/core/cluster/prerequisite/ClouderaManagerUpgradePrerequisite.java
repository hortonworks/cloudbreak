package com.sequenceiq.cloudbreak.core.cluster.prerequisite;

import com.sequenceiq.cloudbreak.dto.StackDto;

/**
 * A step that has to run before the Cloudera Manager upgrade begins, typically a temporary workaround for an image or
 * service defect. {@link ClouderaManagerUpgradePrerequisiteService} runs the implementations before any Cloudera
 * Manager package or cluster service is touched, so a failing prerequisite leaves the cluster untouched.
 * <p>
 * Not to be confused with {@link com.sequenceiq.cloudbreak.service.upgrade.ClusterUpgradePrerequisitesService}, which
 * removes services incompatible with the target runtime during the upgrade init step.
 */
public interface ClouderaManagerUpgradePrerequisite {

    void execute(StackDto stackDto);
}
