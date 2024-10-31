package com.sequenceiq.cloudbreak.cloud.gcp;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.google.api.services.compute.model.MachineType;

class ExtremeDiskCalculatorTest {

    private ExtremeDiskCalculator calculator = new ExtremeDiskCalculator();

    @Test
    void testExtremeDiskSupportedWithN2AndCpusAbove64() {
        MachineType machineType = new MachineType();
        machineType.setName("n2-highcpu-64");
        machineType.setGuestCpus(64);
        assertTrue(calculator.extremeDiskSupported(machineType));
    }

    @Test
    void testExtremeDiskSupportedWithN2AndCpusBelow64() {
        MachineType machineType = new MachineType();
        machineType.setName("n2-highcpu-32");
        machineType.setGuestCpus(32);
        assertFalse(calculator.extremeDiskSupported(machineType));
    }

    @Test
    void testExtremeDiskSupportedWithM2() {
        MachineType machineType = new MachineType();
        machineType.setName("m2-standard-16");
        machineType.setGuestCpus(16);
        assertTrue(calculator.extremeDiskSupported(machineType));
    }

    @Test
    void testExtremeDiskSupportedWithM3() {
        MachineType machineType = new MachineType();
        machineType.setName("m3-standard-16");
        machineType.setGuestCpus(16);
        assertTrue(calculator.extremeDiskSupported(machineType));
    }

    @Test
    void testExtremeDiskSupportedWithUnsupportedMachineType() {
        MachineType machineType = new MachineType();
        machineType.setName("c2-standard-16");
        machineType.setGuestCpus(16);
        assertFalse(calculator.extremeDiskSupported(machineType));
    }
}