package com.sequenceiq.cloudbreak.converter.v4.customimage;

import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.model.catalog.Image;

@Component
public class ImageVersionsConverter {

    private static final Map<String, String> KEY_MAPPING = Map.of(
            "cm", "cm-version",
            "cm-build-number", "cm-gbn",
            "stack", "cdh-version",
            "cdh-build-number", "cdh-gbn"
    );

    public Map<String, String> convert(Image source) {
        return KEY_MAPPING.entrySet().stream()
                .filter(e -> source.getPackageVersions().containsKey(e.getKey()))
                .collect(Collectors.toMap(Map.Entry::getValue, e -> source.getPackageVersions().get(e.getKey())));
    }
}
