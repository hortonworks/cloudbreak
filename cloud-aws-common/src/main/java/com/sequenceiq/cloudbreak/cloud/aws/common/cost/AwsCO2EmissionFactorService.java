package com.sequenceiq.cloudbreak.cloud.aws.common.cost;

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

@Service("awsCO2EmissionFactorService")
public class AwsCO2EmissionFactorService implements CO2EmissionFactorService {

    private static final double AWS_AVERAGE_MINIMUM_WATTS = 0.74;

    private static final double AWS_AVERAGE_MAXIMUM_WATTS = 3.5;

    private static final double AWS_POWER_USAGE_EFFECTIVENESS = 1.135;

    private static final String AWS_EMISSION_FACTORS_JSON_LOCATION = "cost/aws-emission-factors.json";

    private final Map<String, Double> emissionFactors = loadEmissionFactors();

    private static Map<String, Double> loadEmissionFactors() {
        ClassPathResource classPathResource = new ClassPathResource(AWS_EMISSION_FACTORS_JSON_LOCATION);
        if (classPathResource.exists()) {
            try {
                String json = FileReaderUtils.readFileFromClasspath(AWS_EMISSION_FACTORS_JSON_LOCATION);
                return JsonUtil.readValue(json, new TypeReference<>() { });
            } catch (IOException e) {
                throw new CloudbreakRuntimeException("Failed to load AWS emission factors json!", e);
            }
        }
        throw new CloudbreakRuntimeException("AWS emission factors json file not found!");
    }

    public double getAverageMinimumWatts() {
        return AWS_AVERAGE_MINIMUM_WATTS;
    }

    public double getAverageMaximumWatts() {
        return AWS_AVERAGE_MAXIMUM_WATTS;
    }

    public double getPowerUsageEffectiveness() {
        return AWS_POWER_USAGE_EFFECTIVENESS;
    }

    public double getEmissionFactorByRegion(String region) {
        return emissionFactors.getOrDefault(region, 0.0);
    }

    public CloudPlatform getCloudPlatform() {
        return CloudPlatform.AWS;
    }
}
