package com.sequenceiq.cloudbreak.cloud.aws.common.cost.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class Attributes {

    private String enhancedNetworkingSupported;

    private String intelTurboAvailable;

    private String memory;

    private String dedicatedEbsThroughput;

    private int vcpu;

    private String classicnetworkingsupport;

    private String capacitystatus;

    private String locationType;

    private String storage;

    private String instanceFamily;

    private String operatingSystem;

    private String intelAvx2Available;

    private String regionCode;

    private String physicalProcessor;

    private String clockSpeed;

    private String ecu;

    private String networkPerformance;

    private String servicename;

    private String vpcnetworkingsupport;

    private String instanceType;

    private String tenancy;

    private String usagetype;

    private String normalizationSizeFactor;

    private String intelAvxAvailable;

    private String processorFeatures;

    private String servicecode;

    private String licenseModel;

    private String currentGeneration;

    private String preInstalledSw;

    private String location;

    private String processorArchitecture;

    private String marketoption;

    private String operation;

    private String availabilityzone;

    public String getEnhancedNetworkingSupported() {
        return enhancedNetworkingSupported;
    }

    public String getIntelTurboAvailable() {
        return intelTurboAvailable;
    }

    public String getMemory() {
        return memory;
    }

    public String getDedicatedEbsThroughput() {
        return dedicatedEbsThroughput;
    }

    public int getVcpu() {
        return vcpu;
    }

    public String getClassicnetworkingsupport() {
        return classicnetworkingsupport;
    }

    public String getCapacitystatus() {
        return capacitystatus;
    }

    public String getLocationType() {
        return locationType;
    }

    public String getStorage() {
        return storage;
    }

    public String getInstanceFamily() {
        return instanceFamily;
    }

    public String getOperatingSystem() {
        return operatingSystem;
    }

    public String getIntelAvx2Available() {
        return intelAvx2Available;
    }

    public String getRegionCode() {
        return regionCode;
    }

    public String getPhysicalProcessor() {
        return physicalProcessor;
    }

    public String getClockSpeed() {
        return clockSpeed;
    }

    public String getEcu() {
        return ecu;
    }

    public String getNetworkPerformance() {
        return networkPerformance;
    }

    public String getServicename() {
        return servicename;
    }

    public String getVpcnetworkingsupport() {
        return vpcnetworkingsupport;
    }

    public String getInstanceType() {
        return instanceType;
    }

    public String getTenancy() {
        return tenancy;
    }

    public String getUsagetype() {
        return usagetype;
    }

    public String getNormalizationSizeFactor() {
        return normalizationSizeFactor;
    }

    public String getIntelAvxAvailable() {
        return intelAvxAvailable;
    }

    public String getProcessorFeatures() {
        return processorFeatures;
    }

    public String getServicecode() {
        return servicecode;
    }

    public String getLicenseModel() {
        return licenseModel;
    }

    public String getCurrentGeneration() {
        return currentGeneration;
    }

    public String getPreInstalledSw() {
        return preInstalledSw;
    }

    public String getLocation() {
        return location;
    }

    public String getProcessorArchitecture() {
        return processorArchitecture;
    }

    public String getMarketoption() {
        return marketoption;
    }

    public String getOperation() {
        return operation;
    }

    public String getAvailabilityzone() {
        return availabilityzone;
    }
}

