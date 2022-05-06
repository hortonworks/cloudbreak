package com.sequenceiq.consumption.flow.consumption;

import com.sequenceiq.consumption.domain.Consumption;
import com.sequenceiq.flow.core.CommonContext;
import com.sequenceiq.flow.core.FlowParameters;

public class ConsumptionContext extends CommonContext {

    private final Consumption consumption;

    public ConsumptionContext(FlowParameters flowParameters, Consumption consumption) {
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
