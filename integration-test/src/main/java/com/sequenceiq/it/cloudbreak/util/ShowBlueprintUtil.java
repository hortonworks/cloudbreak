package com.sequenceiq.it.cloudbreak.util;

import java.io.IOException;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Strings;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.distrox.DistroXTestDto;
import com.sequenceiq.it.cloudbreak.exception.TestFailException;
import com.sequenceiq.it.cloudbreak.microservice.CloudbreakClient;

public class ShowBlueprintUtil {

    private ShowBlueprintUtil() {

    }

    public static DistroXTestDto checkFutureBlueprint(TestContext testContext, DistroXTestDto distroXTestDto, CloudbreakClient cloudbreakClient) {
        String extendedBlueprintText = distroXTestDto.getGeneratedBlueprint().getBlueprintText();
        validateGeneratedBlueprint(extendedBlueprintText);
        return distroXTestDto;
    }

    public static DistroXTestDto checkGeneratedBlueprint(TestContext testContext, DistroXTestDto distroXTestDto, CloudbreakClient cloudbreakClient) {
        String extendedBlueprintText = distroXTestDto.getResponse().getCluster().getExtendedBlueprintText();
        validateGeneratedBlueprint(extendedBlueprintText);
        return distroXTestDto;
    }

    private static void validateGeneratedBlueprint(String extendedBlueprintText) {
        if (Strings.isNullOrEmpty(extendedBlueprintText)) {
            throw new TestFailException("Generated Blueprint does not exist");
        } else if (!isJSONValid(extendedBlueprintText)) {
            throw new TestFailException("Generated Blueprint is not a valid json");
        }
    }

    private static boolean isJSONValid(String jsonInString) {
        try {
            final ObjectMapper mapper = new ObjectMapper();
            mapper.readTree(jsonInString);
            return true;
        } catch (IOException e) {
            return false;
        }
    }
}
