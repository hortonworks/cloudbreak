package com.sequenceiq.consumption.flow.consumption;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.consumption.domain.Consumption;
import com.sequenceiq.flow.core.CommonContext;
import com.sequenceiq.flow.core.FlowParameters;

public class ConsumptionContext extends CommonContext {

    private final Consumption consumption;

    @JsonCreator
    public ConsumptionContext(
            @JsonProperty("flowParameters") FlowParameters flowParameters,
            @JsonProperty("consumption") Consumption consumption) {

        super(flowParameters);
        this.consumption = consumption;
    }

    public Consumption getConsumption() {
        return consumption;
    }

    @Override
    public String toString() {
        return "ConsumptionContext{" +
                "consumption=" + consumption +
                "} " + super.toString();
    }
}
