package com.sequenceiq.it.cloudbreak.newway.util;

import java.io.IOException;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Strings;
import com.sequenceiq.it.cloudbreak.exception.TestFailException;
import com.sequenceiq.it.cloudbreak.newway.CloudbreakClient;
import com.sequenceiq.it.cloudbreak.newway.context.TestContext;
import com.sequenceiq.it.cloudbreak.newway.dto.stack.StackTestDto;

public class ShowBlueprintUtil {

    private ShowBlueprintUtil() {

    }

    public static StackTestDto checkFutureBlueprint(TestContext testContext, StackTestDto stackTestDto, CloudbreakClient cloudbreakClient) {
        String extendedBlueprintText = stackTestDto.getGeneratedBlueprint().getBlueprintText();
        validateGeneratedBlueprint(extendedBlueprintText);
        return stackTestDto;
    }

    public static StackTestDto checkGeneratedBlueprint(TestContext testContext, StackTestDto stackTestDto, CloudbreakClient cloudbreakClient) {
        String extendedBlueprintText = stackTestDto.getResponse().getCluster().getExtendedBlueprintText();
        validateGeneratedBlueprint(extendedBlueprintText);
        return stackTestDto;
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
