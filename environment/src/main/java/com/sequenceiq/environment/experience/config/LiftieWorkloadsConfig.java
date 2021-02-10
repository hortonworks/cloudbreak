package com.sequenceiq.environment.experience.config;

import java.util.Set;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import com.sequenceiq.environment.experience.liftie.LiftieWorkload;

@Component
@ConfigurationProperties("environment.experience.liftie")
public class LiftieWorkloadsConfig {

    private Set<LiftieWorkload> workloads;

    public Set<LiftieWorkload> getWorkloads() {
        return workloads != null ? workloads : Set.of();
    }

    public void setWorkloads(Set<LiftieWorkload> workloads) {
        this.workloads = workloads;
    }

}
