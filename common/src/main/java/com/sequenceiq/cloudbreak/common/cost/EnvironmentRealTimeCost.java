package com.sequenceiq.cloudbreak.common.cost;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

public class EnvironmentRealTimeCost implements Serializable {

    private double hourlyProviderUsd;

    private double hourlyClouderaUsd;

    private double hourlyCO2;

    private RealTimeCost freeipa;

    private RealTimeCost datalake;

    private Map<String, RealTimeCost> datahubs;

    public EnvironmentRealTimeCost() {
        datahubs = new HashMap<>();
    }

    public double getHourlyProviderUsd() {
        return hourlyProviderUsd;
    }

    public void setHourlyProviderUsd(double hourlyProviderUsd) {
        this.hourlyProviderUsd = hourlyProviderUsd;
    }

    public double getHourlyClouderaUsd() {
        return hourlyClouderaUsd;
    }

    public void setHourlyClouderaUsd(double hourlyClouderaUsd) {
        this.hourlyClouderaUsd = hourlyClouderaUsd;
    }

    public double getHourlyCO2() {
        return hourlyCO2;
    }

    public void setHourlyCO2(double hourlyCO2) {
        this.hourlyCO2 = hourlyCO2;
    }

    public RealTimeCost getFreeipa() {
        return freeipa;
    }

    public void setFreeipa(RealTimeCost freeipa) {
        this.freeipa = freeipa;
    }

    public RealTimeCost getDatalake() {
        return datalake;
    }

    public void setDatalake(RealTimeCost datalake) {
        this.datalake = datalake;
    }

    public Map<String, RealTimeCost> getDatahubs() {
        return datahubs;
    }

    public void setDatahubs(Map<String, RealTimeCost> datahubs) {
        this.datahubs = datahubs;
    }

    public EnvironmentRealTimeCost add(String resourceCrn, RealTimeCost o1) {
        this.hourlyProviderUsd += o1.getHourlyProviderUsd();
        this.hourlyClouderaUsd += o1.getHourlyClouderaUsd();
        this.hourlyCO2 += o1.getHourlyCO2();
        if (o1.getType() != null) {

            switch (o1.getType()) {
                case "FREEIPA":
                    freeipa = o1;
                    break;
                case "DATALAKE":
                    datalake = o1;
                    break;
                case "WORKLOAD":
                    datahubs.put(resourceCrn, o1);
                    break;
            }

        }
        return this;
    }
}
