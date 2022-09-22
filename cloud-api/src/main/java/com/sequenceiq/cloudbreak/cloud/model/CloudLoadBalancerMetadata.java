package com.sequenceiq.cloudbreak.cloud.model;

import java.util.Map;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.cloudbreak.cloud.model.generic.DynamicModel;
import com.sequenceiq.common.api.type.LoadBalancerType;

public class CloudLoadBalancerMetadata extends DynamicModel {

    private final LoadBalancerType type;

    private final String cloudDns;

    private final String hostedZoneId;

    private final String ip;

    private final String name;

    @JsonCreator
    private CloudLoadBalancerMetadata(
            @JsonProperty("type") LoadBalancerType type,
            @JsonProperty("cloudDns") String cloudDns,
            @JsonProperty("hostedZoneId") String hostedZoneId,
            @JsonProperty("ip") String ip,
            @JsonProperty("name") String name,
            @JsonProperty("parameters") Map<String, Object> parameters) {

        super(parameters);
        this.type = type;
        this.cloudDns = cloudDns;
        this.hostedZoneId = hostedZoneId;
        this.ip = ip;
        this.name = name;
    }

    public LoadBalancerType getType() {
        return type;
    }

    public String getCloudDns() {
        return cloudDns;
    }

    public String getHostedZoneId() {
        return hostedZoneId;
    }

    public String getIp() {
        return ip;
    }

    public String getName() {
        return name;
    }

    public static Builder builder() {
        return new Builder();
    }

    @Override
    public String toString() {
        return "CloudLoadBalancerMetadata{" +
            "type=" + type +
            ", cloudDns='" + cloudDns + '\'' +
            ", hostedZoneId='" + hostedZoneId + '\'' +
            ", ip='" + ip + '\'' +
            ", name='" + name + '\'' +
            '}';
    }

    public static class Builder {

        private LoadBalancerType type;

        private String cloudDns;

        private String hostedZoneId;

        private String ip;

        private String name;

        private Map<String, Object> parameters;

        private Builder() {
        }

        public Builder withType(LoadBalancerType type) {
            this.type = type;
            return this;
        }

        public Builder withCloudDns(String cloudDns) {
            this.cloudDns = cloudDns;
            return this;
        }

        public Builder withHostedZoneId(String hostedZoneId) {
            this.hostedZoneId = hostedZoneId;
            return this;
        }

        public Builder withIp(String ip) {
            this.ip = ip;
            return this;
        }

        public Builder withName(String name) {
            this.name = name;
            return this;
        }

        public Builder withParameters(Map<String, Object> parameters) {
            this.parameters = parameters;
            return this;
        }

        public CloudLoadBalancerMetadata build() {
            return new CloudLoadBalancerMetadata(type, cloudDns, hostedZoneId, ip, name, parameters);
        }
    }
}
