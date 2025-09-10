package com.sequenceiq.thunderhead.util;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;

import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.type.TypeReference;
import com.sequenceiq.cloudbreak.common.json.JsonUtil;
import com.sequenceiq.cloudbreak.util.FileReaderUtils;

@Component
public class MockResponseLoader {

    public <T> List<T> load(Class<T> clazz, TypeReference<List<T>> listTypeReference, String pathOverride) throws IOException {
        String defaultPath = String.format("mock-responses/%s/list.json", clazz.getSimpleName().toLowerCase(Locale.ROOT));
        String resourcesJson = pathOverride.isEmpty()
                ? FileReaderUtils.readFileFromClasspath(defaultPath)
                : FileReaderUtils.readFileFromPath(Path.of(pathOverride));
        return JsonUtil.readValue(resourcesJson, listTypeReference);
    }
}
