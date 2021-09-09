package com.sequenceiq.environment.platformresource.v1.converter;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.PlatformParameters;
import com.sequenceiq.cloudbreak.cloud.model.Platform;
import com.sequenceiq.cloudbreak.cloud.model.TagSpecification;
import com.sequenceiq.environment.api.v1.platformresource.model.TagSpecificationsResponse;

@Component
public class TagSpecificationsToTagSpecificationsV1ResponseConverter {

    public TagSpecificationsResponse convert(Map<Platform, PlatformParameters> source) {
        TagSpecificationsResponse json = new TagSpecificationsResponse();
        Map<String, Map<String, Object>> specifications = new HashMap<>();
        json.setSpecifications(specifications);

        source.keySet().forEach(p -> {
            TagSpecification ts = source.get(p).tagSpecification();
            Map<String, Object> specification = Collections.emptyMap();
            if (ts != null) {
                specification = new HashMap<>();
                specification.put("maxAmount", ts.getMaxAmount());
                specification.put("minKeyLength", ts.getMinKeyLength());
                specification.put("maxKeyLength", ts.getMaxKeyLength());
                specification.put("keyLength", ts.getMaxKeyLength());
                specification.put("keyValidator", ts.getKeyValidator());
                specification.put("minValueLength", ts.getMinValueLength());
                specification.put("maxValueLength", ts.getMaxValueLength());
                specification.put("valueLength", ts.getMaxValueLength());
                specification.put("valueValidator", ts.getValueValidator());
            }
            specifications.put(p.value(), specification);
        });

        return json;
    }
}
