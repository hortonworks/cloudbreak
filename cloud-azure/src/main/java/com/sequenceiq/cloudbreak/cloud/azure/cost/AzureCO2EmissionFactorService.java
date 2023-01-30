package com.sequenceiq.cloudbreak.cloud.azure.cost;

import java.io.IOException;
import java.util.Map;

import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.sequenceiq.cloudbreak.co2.CO2EmissionFactorService;
import com.sequenceiq.cloudbreak.common.json.JsonUtil;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.cloudbreak.service.CloudbreakRuntimeException;
import com.sequenceiq.cloudbreak.util.FileReaderUtils;

@Service("azureCO2EmissionFactorService")
public class AzureCO2EmissionFactorService implements CO2EmissionFactorService {

    private static final double AZURE_AVERAGE_MINIMUM_WATTS = 0.78;

    private static final double AZURE_AVERAGE_MAXIMUM_WATTS = 3.76;

    private static final double AZURE_POWER_USAGE_EFFECTIVENESS = 1.18;

    private static final String AZURE_EMISSION_FACTORS_JSON_LOCATION = "cost/azure-emission-factors.json";

    private final Map<String, Double> emissionFactors = loadEmissionFactors();

    private static Map<String, Double> loadEmissionFactors() {
        ClassPathResource classPathResource = new ClassPathResource(AZURE_EMISSION_FACTORS_JSON_LOCATION);
        if (classPathResource.exists()) {
            try {
                String json = FileReaderUtils.readFileFromClasspath(AZURE_EMISSION_FACTORS_JSON_LOCATION);
                return JsonUtil.readValue(json, new TypeReference<>() { });
            } catch (IOException e) {
                throw new CloudbreakRuntimeException("Failed to load Azure emission factors json!", e);
            }
        }
        throw new CloudbreakRuntimeException("Azure emission factors json file not found!");
    }

    public double getAverageMinimumWatts() {
        return AZURE_AVERAGE_MINIMUM_WATTS;
    }

    public double getAverageMaximumWatts() {
        return AZURE_AVERAGE_MAXIMUM_WATTS;
    }

    public double getPowerUsageEffectiveness() {
        return AZURE_POWER_USAGE_EFFECTIVENESS;
    }

    public double getEmissionFactorByRegion(String region) {
        return emissionFactors.getOrDefault(region, 0.0);
    }

    public CloudPlatform getCloudPlatform() {
        return CloudPlatform.AZURE;
    }
}
