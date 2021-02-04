package com.sequenceiq.environment.experience.liftie;


import java.util.Collections;
import java.util.Set;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class ExperienceIndependentLiftieClusterWorkloadProvider {

    private Set<String> workloadLabels;

    public ExperienceIndependentLiftieClusterWorkloadProvider(@Value("${experience.scan.liftie.workloads") Set<String> workloadLabels) {
        this.workloadLabels = workloadLabels != null ? workloadLabels : Collections.emptySet();
    }

    public Set<String> getWorkloadsLabels() {
        return workloadLabels;
    }

}
