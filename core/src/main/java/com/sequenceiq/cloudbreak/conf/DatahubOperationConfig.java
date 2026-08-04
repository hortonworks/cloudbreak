package com.sequenceiq.cloudbreak.conf;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.StringJoiner;

import jakarta.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "datahub-operation")
public class DatahubOperationConfig {

    private static final Logger LOGGER = LoggerFactory.getLogger(DatahubOperationConfig.class);

    private Map<String, OperationConfig> operations = new HashMap<>();

    public Map<String, OperationConfig> getOperations() {
        return operations;
    }

    public void setOperations(Map<String, OperationConfig> operations) {
        this.operations = operations;
    }

    @PostConstruct
    public void log() {
        LOGGER.info("Datahub operation configurations loaded: {}", toString());
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", DatahubOperationConfig.class.getSimpleName() + "[", "]")
                .add("operations=" + operations)
                .toString();
    }

    public static class OperationConfig {

        private Set<String> mandatoryHealthyHostgroups = new HashSet<>(Set.of("master"));

        private Set<String> requiredPartialHostgroups = new HashSet<>(Set.of("idbroker", "gateway"));

        public Set<String> getMandatoryHealthyHostgroups() {
            return mandatoryHealthyHostgroups;
        }

        public void setMandatoryHealthyHostgroups(Set<String> mandatoryHealthyHostgroups) {
            this.mandatoryHealthyHostgroups = mandatoryHealthyHostgroups;
        }

        public Set<String> getRequiredPartialHostgroups() {
            return requiredPartialHostgroups;
        }

        public void setRequiredPartialHostgroups(Set<String> requiredPartialHostgroups) {
            this.requiredPartialHostgroups = requiredPartialHostgroups;
        }

        @Override
        public String toString() {
            return new StringJoiner(", ", OperationConfig.class.getSimpleName() + "[", "]")
                    .add("mandatoryHealthyHostgroups=" + mandatoryHealthyHostgroups)
                    .add("requiredPartialHostgroups=" + requiredPartialHostgroups)
                    .toString();
        }
    }
}
