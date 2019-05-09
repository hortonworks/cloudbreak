package com.sequenceiq.it.cloudbreak.newway.assertion.info;

import java.util.Map;

import com.sequenceiq.it.cloudbreak.newway.assertion.AssertionV2;
import com.sequenceiq.it.cloudbreak.newway.dto.info.CloudbreakInfoTestDto;

public class InfoTestAssertion {

    private static final String APP = "app";

    private static final String NAME = "name";

    private static final String VERSION = "version";

    private InfoTestAssertion() {
    }

    public static AssertionV2<CloudbreakInfoTestDto> infoContainsProperties(String name) {
        return (testContext, testDto, cloudbreakClient) -> {
            hasInfoVersion(testDto);
            hasAppName(testDto, name);
            return testDto;
        };
    }

    private static void hasInfoVersion(CloudbreakInfoTestDto dto) {
        Map<String, Object> info = dto.getResponse().getInfo();
        if (info.get(APP) != null) {
            Map<String, Object> app = (Map<String, Object>) info.get(APP);
            if (app.get(VERSION) == null || "".equals(app.get(VERSION))) {
                throw new IllegalArgumentException(String.format("The Service version is null or empty."));
            }
        }
    }

    private static void hasAppName(CloudbreakInfoTestDto dto, String name) {
        Map<String, Object> info = dto.getResponse().getInfo();
        if (info.get(APP) != null) {
            Map<String, Object> app = (Map<String, Object>) info.get(APP);
            if (!name.equals(app.get(NAME))) {
                throw new IllegalArgumentException(String.format("The Service name is not equal with %s.", name));
            }
        }
    }
}
