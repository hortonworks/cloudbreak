package com.sequenceiq.it.cloudbreak.performance;

public interface KeyMeasurementFormatter {
    String header();

    String element(KeyMeasurement p);

    String foot();
}
