package com.sequenceiq.environment.experience.liftie;


import java.util.Collections;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class ExperienceIndependentLiftieClusterWorkloadProvider {

    private static final Logger LOGGER = LoggerFactory.getLogger(ExperienceIndependentLiftieClusterWorkloadProvider.class);

    private Set<String> workloadLabels;

    public ExperienceIndependentLiftieClusterWorkloadProvider(@Value("${experience.scan.liftie.workloads}") Set<String> workloadLabels) {
        this.workloadLabels = workloadLabels != null ? workloadLabels : Collections.emptySet();
        LOGGER.debug("The list of configured, experience independent liftie cluster workload label is the following: "
                + String.join(",", this.workloadLabels));
    }

    public Set<String> getWorkloadsLabels() {
        return workloadLabels;
    }

}
