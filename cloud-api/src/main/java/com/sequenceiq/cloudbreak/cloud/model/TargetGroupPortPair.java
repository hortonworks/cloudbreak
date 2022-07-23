package com.sequenceiq.cloudbreak.cloud.model;

import java.io.IOException;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.KeyDeserializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.sequenceiq.cloudbreak.common.json.JsonUtil;

/**
 * A joining of ports related to target groups. Target groups are groups of instances that a load balancer routes traffic
 * to. The instances in the target group have a port on which they receive traffic from the load balancer (trafficPort),
 * and a port on which the load balancer runs its health checks (healthCheckPort). If the health check responses from
 * the healthCheckPort ever go into an unhealthy state, the load balancer will stop forwarding traffic to the trafficPort
 * for the unhealthy instance.
 */
public class TargetGroupPortPair {

    private final int trafficPort;

    private final int healthCheckPort;

    @JsonCreator
    public TargetGroupPortPair(
            @JsonProperty("trafficPort") int trafficPort,
            @JsonProperty("healthCheckPort") int healthCheckPort) {

        this.trafficPort = trafficPort;
        this.healthCheckPort = healthCheckPort;
    }

    public Integer getTrafficPort() {
        return trafficPort;
    }

    public Integer getHealthCheckPort() {
        return healthCheckPort;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        TargetGroupPortPair portPair = (TargetGroupPortPair) o;
        return trafficPort == portPair.trafficPort &&
            healthCheckPort == portPair.healthCheckPort;
    }

    @Override
    public int hashCode() {
        return Objects.hash(trafficPort, healthCheckPort);
    }

    @Override
    public String toString() {
        return "TargetGroupPortPair{" +
                "trafficPort=" + trafficPort +
                ", healthCheckPort=" + healthCheckPort +
                '}';
    }

    public static class TargetGroupPortPairDeserializer extends KeyDeserializer {
        @Override
        public Object deserializeKey(String key, DeserializationContext ctxt) throws IOException {
            return JsonUtil.readValue(key, TargetGroupPortPair.class);
        }
    }

    public static class TargetGroupPortPairSerializer extends JsonSerializer<TargetGroupPortPair> {
        @Override
        public void serialize(TargetGroupPortPair value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
            gen.writeFieldName(JsonUtil.writeValueAsStringUnchecked(value));
        }
    }
}
