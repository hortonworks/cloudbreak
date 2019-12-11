package com.sequenceiq.it.cloudbreak.performance;

import java.util.List;

public class KeyPerformanceIndicator<T extends KeyMeasurement> {
    private List<T> keyPerformanceIndicatorList;

    private KeyMeasurementFormatter formatter;

    protected KeyPerformanceIndicator() {
    }

    KeyPerformanceIndicator(List<T> list) {
        keyPerformanceIndicatorList = list;
    }

    public List<T> getKeyPerformanceIndicatorList() {
        return keyPerformanceIndicatorList;
    }

    public void setFormatter(KeyMeasurementFormatter formatter) {
        this.formatter = formatter;
    }

    public String print() {
        if (formatter != null) {
            StringBuffer text = new StringBuffer();
            text.append(formatter.header());
            keyPerformanceIndicatorList.stream()
                    .forEach(pi -> text.append(formatter.element(pi)));
            text.append(formatter.foot());
            return text.toString();
        }
        return toString();
    }
}
