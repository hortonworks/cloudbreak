package com.sequenceiq.cloudbreak.common.cost.service;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.sequenceiq.cloudbreak.common.cost.model.RegionEmissionFactor;
import com.sequenceiq.cloudbreak.common.json.JsonUtil;
import com.sequenceiq.cloudbreak.util.FileReaderUtils;

@Service
public class RegionEmissionFactorService {

    private static final Logger LOGGER = LoggerFactory.getLogger(RegionEmissionFactorService.class);

    private static final String EMISSION_FACTOR_LOCATION = "cost/emission-factor.json";

    private Map<String, RegionEmissionFactor> emissionFactorMap;

    @PostConstruct
    public void init() {
        try {
            this.emissionFactorMap = loadEmissionList().stream().collect(Collectors.toMap(RegionEmissionFactor::getRegion, Function.identity()));
        } catch (IOException e) {
            LOGGER.warn("Failed to load emission factor!", e);
        }
    }

    public RegionEmissionFactor get(String region) {
        return emissionFactorMap.getOrDefault(region, new RegionEmissionFactor());
    }

    private List<RegionEmissionFactor> loadEmissionList() throws IOException {
        ClassPathResource classPathResource = new ClassPathResource(EMISSION_FACTOR_LOCATION);
        if (classPathResource.exists()) {
            String json = FileReaderUtils.readFileFromClasspath(EMISSION_FACTOR_LOCATION);
            return JsonUtil.readValue(json, new TypeReference<List<RegionEmissionFactor>>() {
            });
        }
        throw new RuntimeException("Failed to load emission factor!");
    }
}
