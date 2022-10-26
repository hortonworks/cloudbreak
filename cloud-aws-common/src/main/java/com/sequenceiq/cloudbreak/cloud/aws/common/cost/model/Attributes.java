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

    public Attributes() {
    }

    public Attributes(String enhancedNetworkingSupported, String intelTurboAvailable, String memory, String dedicatedEbsThroughput, int vcpu,
            String classicnetworkingsupport, String capacitystatus, String locationType, String storage, String instanceFamily, String operatingSystem,
            String intelAvx2Available, String regionCode, String physicalProcessor, String clockSpeed, String ecu, String networkPerformance,
            String servicename, String vpcnetworkingsupport, String instanceType, String tenancy, String usagetype, String normalizationSizeFactor,
            String intelAvxAvailable, String processorFeatures, String servicecode, String licenseModel, String currentGeneration, String preInstalledSw,
            String location, String processorArchitecture, String marketoption, String operation, String availabilityzone) {
        this.enhancedNetworkingSupported = enhancedNetworkingSupported;
        this.intelTurboAvailable = intelTurboAvailable;
        this.memory = memory;
        this.dedicatedEbsThroughput = dedicatedEbsThroughput;
        this.vcpu = vcpu;
        this.classicnetworkingsupport = classicnetworkingsupport;
        this.capacitystatus = capacitystatus;
        this.locationType = locationType;
        this.storage = storage;
        this.instanceFamily = instanceFamily;
        this.operatingSystem = operatingSystem;
        this.intelAvx2Available = intelAvx2Available;
        this.regionCode = regionCode;
        this.physicalProcessor = physicalProcessor;
        this.clockSpeed = clockSpeed;
        this.ecu = ecu;
        this.networkPerformance = networkPerformance;
        this.servicename = servicename;
        this.vpcnetworkingsupport = vpcnetworkingsupport;
        this.instanceType = instanceType;
        this.tenancy = tenancy;
        this.usagetype = usagetype;
        this.normalizationSizeFactor = normalizationSizeFactor;
        this.intelAvxAvailable = intelAvxAvailable;
        this.processorFeatures = processorFeatures;
        this.servicecode = servicecode;
        this.licenseModel = licenseModel;
        this.currentGeneration = currentGeneration;
        this.preInstalledSw = preInstalledSw;
        this.location = location;
        this.processorArchitecture = processorArchitecture;
        this.marketoption = marketoption;
        this.operation = operation;
        this.availabilityzone = availabilityzone;
    }

    public String getEnhancedNetworkingSupported() {
        return enhancedNetworkingSupported;
    }

    public void setEnhancedNetworkingSupported(String enhancedNetworkingSupported) {
        this.enhancedNetworkingSupported = enhancedNetworkingSupported;
    }

    public String getIntelTurboAvailable() {
        return intelTurboAvailable;
    }

    public void setIntelTurboAvailable(String intelTurboAvailable) {
        this.intelTurboAvailable = intelTurboAvailable;
    }

    public String getMemory() {
        return memory;
    }

    public void setMemory(String memory) {
        this.memory = memory;
    }

    public String getDedicatedEbsThroughput() {
        return dedicatedEbsThroughput;
    }

    public void setDedicatedEbsThroughput(String dedicatedEbsThroughput) {
        this.dedicatedEbsThroughput = dedicatedEbsThroughput;
    }

    public int getVcpu() {
        return vcpu;
    }

    public void setVcpu(int vcpu) {
        this.vcpu = vcpu;
    }

    public String getClassicnetworkingsupport() {
        return classicnetworkingsupport;
    }

    public void setClassicnetworkingsupport(String classicnetworkingsupport) {
        this.classicnetworkingsupport = classicnetworkingsupport;
    }

    public String getCapacitystatus() {
        return capacitystatus;
    }

    public void setCapacitystatus(String capacitystatus) {
        this.capacitystatus = capacitystatus;
    }

    public String getLocationType() {
        return locationType;
    }

    public void setLocationType(String locationType) {
        this.locationType = locationType;
    }

    public String getStorage() {
        return storage;
    }

    public void setStorage(String storage) {
        this.storage = storage;
    }

    public String getInstanceFamily() {
        return instanceFamily;
    }

    public void setInstanceFamily(String instanceFamily) {
        this.instanceFamily = instanceFamily;
    }

    public String getOperatingSystem() {
        return operatingSystem;
    }

    public void setOperatingSystem(String operatingSystem) {
        this.operatingSystem = operatingSystem;
    }

    public String getIntelAvx2Available() {
        return intelAvx2Available;
    }

    public void setIntelAvx2Available(String intelAvx2Available) {
        this.intelAvx2Available = intelAvx2Available;
    }

    public String getRegionCode() {
        return regionCode;
    }

    public void setRegionCode(String regionCode) {
        this.regionCode = regionCode;
    }

    public String getPhysicalProcessor() {
        return physicalProcessor;
    }

    public void setPhysicalProcessor(String physicalProcessor) {
        this.physicalProcessor = physicalProcessor;
    }

    public String getClockSpeed() {
        return clockSpeed;
    }

    public void setClockSpeed(String clockSpeed) {
        this.clockSpeed = clockSpeed;
    }

    public String getEcu() {
        return ecu;
    }

    public void setEcu(String ecu) {
        this.ecu = ecu;
    }

    public String getNetworkPerformance() {
        return networkPerformance;
    }

    public void setNetworkPerformance(String networkPerformance) {
        this.networkPerformance = networkPerformance;
    }

    public String getServicename() {
        return servicename;
    }

    public void setServicename(String servicename) {
        this.servicename = servicename;
    }

    public String getVpcnetworkingsupport() {
        return vpcnetworkingsupport;
    }

    public void setVpcnetworkingsupport(String vpcnetworkingsupport) {
        this.vpcnetworkingsupport = vpcnetworkingsupport;
    }

    public String getInstanceType() {
        return instanceType;
    }

    public void setInstanceType(String instanceType) {
        this.instanceType = instanceType;
    }

    public String getTenancy() {
        return tenancy;
    }

    public void setTenancy(String tenancy) {
        this.tenancy = tenancy;
    }

    public String getUsagetype() {
        return usagetype;
    }

    public void setUsagetype(String usagetype) {
        this.usagetype = usagetype;
    }

    public String getNormalizationSizeFactor() {
        return normalizationSizeFactor;
    }

    public void setNormalizationSizeFactor(String normalizationSizeFactor) {
        this.normalizationSizeFactor = normalizationSizeFactor;
    }

    public String getIntelAvxAvailable() {
        return intelAvxAvailable;
    }

    public void setIntelAvxAvailable(String intelAvxAvailable) {
        this.intelAvxAvailable = intelAvxAvailable;
    }

    public String getProcessorFeatures() {
        return processorFeatures;
    }

    public void setProcessorFeatures(String processorFeatures) {
        this.processorFeatures = processorFeatures;
    }

    public String getServicecode() {
        return servicecode;
    }

    public void setServicecode(String servicecode) {
        this.servicecode = servicecode;
    }

    public String getLicenseModel() {
        return licenseModel;
    }

    public void setLicenseModel(String licenseModel) {
        this.licenseModel = licenseModel;
    }

    public String getCurrentGeneration() {
        return currentGeneration;
    }

    public void setCurrentGeneration(String currentGeneration) {
        this.currentGeneration = currentGeneration;
    }

    public String getPreInstalledSw() {
        return preInstalledSw;
    }

    public void setPreInstalledSw(String preInstalledSw) {
        this.preInstalledSw = preInstalledSw;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public String getProcessorArchitecture() {
        return processorArchitecture;
    }

    public void setProcessorArchitecture(String processorArchitecture) {
        this.processorArchitecture = processorArchitecture;
    }

    public String getMarketoption() {
        return marketoption;
    }

    public void setMarketoption(String marketoption) {
        this.marketoption = marketoption;
    }

    public String getOperation() {
        return operation;
    }

    public void setOperation(String operation) {
        this.operation = operation;
    }

    public String getAvailabilityzone() {
        return availabilityzone;
    }

    public void setAvailabilityzone(String availabilityzone) {
        this.availabilityzone = availabilityzone;
    }
}

