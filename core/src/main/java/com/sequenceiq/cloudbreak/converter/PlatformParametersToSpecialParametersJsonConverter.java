package com.sequenceiq.cloudbreak.converter;

import java.util.HashMap;
import java.util.Map;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.model.SpecialParameters;
import com.sequenceiq.cloudbreak.cloud.PlatformParameters;
import com.sequenceiq.cloudbreak.cloud.model.Platform;

@Component
public class PlatformParametersToSpecialParametersJsonConverter {

    public Map<String, Map<String, Boolean>> convert(Map<Platform, PlatformParameters> source) {
        Map<String, Map<String, Boolean>> specialParameters = new HashMap<>();
        source.keySet().forEach(p -> {
            SpecialParameters sp = source.get(p).specialParameters();
            specialParameters.put(p.value(), sp.getSpecialParameters());
        });
        return specialParameters;
    }
}
