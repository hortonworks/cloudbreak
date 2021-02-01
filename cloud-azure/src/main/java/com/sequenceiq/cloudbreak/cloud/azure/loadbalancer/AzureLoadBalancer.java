package com.sequenceiq.cloudbreak.cloud.azure.loadbalancer;

import com.sequenceiq.common.api.type.LoadBalancerType;

import java.util.Collection;
import java.util.List;
import java.util.Set;

import static java.util.stream.Collectors.toSet;

public class AzureLoadBalancer {
    private static final String LOAD_BALANCER_NAME_PREFIX = "LoadBalancer";

    private final List<AzureLoadBalancingRule> rules;

    private final Set<AzureLoadBalancerProbe> probes;

    private final String name;

    private final LoadBalancerType type;

    private final Set<String> instanceGroupNames;

    private AzureLoadBalancer(Builder builder) {
        this.rules = List.copyOf(builder.rules);
        this.probes = Set.copyOf(builder.probes);
        this.name = getLoadBalancerName(builder.type, builder.stackName);
        this.type = builder.type;
        this.instanceGroupNames = Set.copyOf(builder.instanceGroupNames);
    }

    public static String getLoadBalancerName(LoadBalancerType type, String stackName) {
        return LOAD_BALANCER_NAME_PREFIX + stackName + type.toString();
    }

    public Collection<AzureLoadBalancingRule> getRules() {
        return rules;
    }

    public Collection<AzureLoadBalancerProbe> getProbes() {
        return probes;
    }

    public String getName() {
        return name;
    }

    public LoadBalancerType getType() {
        return type;
    }

    public Set<String> getInstanceGroupNames() {
        return instanceGroupNames;
    }

    @Override
    public String toString() {
        return "AzureLoadBalancer{" +
                "rules=" + rules +
                ", probes=" + probes +
                ", name='" + name + '\'' +
                ", type=" + type +
                ", instanceGroupNames=" + instanceGroupNames +
                '}';
    }

    public static final class Builder {
        private List<AzureLoadBalancingRule> rules;

        private Set<AzureLoadBalancerProbe> probes;

        private String stackName;

        private LoadBalancerType type;

        private Set<String> instanceGroupNames;

        public Builder setRules(List<AzureLoadBalancingRule> rules) {
            this.rules = rules;
            return this;
        }

        public Builder setStackName(String stackName) {
            this.stackName = stackName;
            return this;
        }

        public Builder setType(LoadBalancerType type) {
            this.type = type;
            return this;
        }

        public Builder setInstanceGroupNames(Set<String> instanceGroupNames) {
            this.instanceGroupNames = instanceGroupNames;
            return this;
        }

        public AzureLoadBalancer createAzureLoadBalancer() {
            probes = rules.stream()
                    .map(AzureLoadBalancingRule::getProbe)
                    .collect(toSet());

            return new AzureLoadBalancer(this);
        }
    }
}