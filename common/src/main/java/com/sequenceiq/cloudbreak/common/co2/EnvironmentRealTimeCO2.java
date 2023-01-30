package com.sequenceiq.cloudbreak.common.co2;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

public class EnvironmentRealTimeCO2 implements Serializable {

    private double hourlyCO2InGrams;

    private RealTimeCO2 freeipa;

    private RealTimeCO2 datalake;

    private Map<String, RealTimeCO2> datahubs = new HashMap<>();

    public EnvironmentRealTimeCO2 addCO2ByType(String resourceCrn, RealTimeCO2 realTimeCO2) {
        this.hourlyCO2InGrams += realTimeCO2.getHourlyCO2InGrams();

        if (realTimeCO2.getType() != null) {
            switch (realTimeCO2.getType()) {
                case "FREEIPA":
                    setFreeipa(realTimeCO2);
                    break;
                case "DATALAKE":
                    setDatalake(realTimeCO2);
                    break;
                case "WORKLOAD":
                    datahubs.put(resourceCrn, realTimeCO2);
                    break;
                default:
            }
        }
        return this;
    }

    public double getHourlyCO2InGrams() {
        return hourlyCO2InGrams;
    }

    public void setHourlyCO2InGrams(double hourlyCO2InGrams) {
        this.hourlyCO2InGrams = hourlyCO2InGrams;
    }

    public RealTimeCO2 getFreeipa() {
        return freeipa;
    }

    public void setFreeipa(RealTimeCO2 freeipa) {
        this.freeipa = freeipa;
    }

    public RealTimeCO2 getDatalake() {
        return datalake;
    }

    public void setDatalake(RealTimeCO2 datalake) {
        this.datalake = datalake;
    }

    public Map<String, RealTimeCO2> getDatahubs() {
        return datahubs;
    }

    public void setDatahubs(Map<String, RealTimeCO2> datahubs) {
        this.datahubs = datahubs;
    }
}
