package com.sequenceiq.cloudbreak.cloud.azure.loadbalancer;

import com.sequenceiq.common.api.type.LoadBalancerSku;
import com.sequenceiq.common.api.type.LoadBalancerType;

import java.util.Collection;
import java.util.List;
import java.util.Set;

import static java.util.stream.Collectors.toSet;

public class AzureLoadBalancer {
    private static final String LOAD_BALANCER_NAME_PREFIX = "LoadBalancer";

    private final List<AzureLoadBalancingRule> rules;

    private final List<AzureOutboundRule> outboundRules;

    private final Set<AzureLoadBalancerProbe> probes;

    private final String name;

    private final LoadBalancerType type;

    private final Set<String> instanceGroupNames;

    private final LoadBalancerSku sku;

    private AzureLoadBalancer(Builder builder) {
        this.rules = List.copyOf(builder.rules);
        this.outboundRules = List.copyOf(builder.outboundRules);
        this.probes = Set.copyOf(builder.probes);
        this.name = getLoadBalancerName(builder.type, builder.stackName);
        this.type = builder.type;
        this.instanceGroupNames = Set.copyOf(builder.instanceGroupNames);
        this.sku = LoadBalancerSku.getValueOrDefault(builder.sku);
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

    public LoadBalancerSku getSku() {
        return sku;
    }

    public List<AzureOutboundRule> getOutboundRules() {
        return outboundRules;
    }

    @Override
    public String toString() {
        return "AzureLoadBalancer{" +
                "rules=" + rules +
                ", probes=" + probes +
                ", name='" + name + '\'' +
                ", type=" + type +
                ", instanceGroupNames=" + instanceGroupNames +
                ", sku=" + sku +
                '}';
    }

    public static final class Builder {
        private List<AzureLoadBalancingRule> rules;

        private List<AzureOutboundRule> outboundRules;

        private Set<AzureLoadBalancerProbe> probes;

        private String stackName;

        private LoadBalancerType type;

        private Set<String> instanceGroupNames;

        private LoadBalancerSku sku;

        public Builder setRules(List<AzureLoadBalancingRule> rules) {
            this.rules = rules;
            return this;
        }

        public Builder setOutboundRules(List<AzureOutboundRule> outboundRules) {
            this.outboundRules = outboundRules;
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

        public Builder setLoadBalancerSku(LoadBalancerSku sku) {
            this.sku = sku;
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