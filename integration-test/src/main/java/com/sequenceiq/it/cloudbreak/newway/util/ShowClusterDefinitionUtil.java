package com.sequenceiq.it.cloudbreak.newway.util;

import java.io.IOException;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Strings;
import com.sequenceiq.it.cloudbreak.exception.TestFailException;
import com.sequenceiq.it.cloudbreak.newway.CloudbreakClient;
import com.sequenceiq.it.cloudbreak.newway.context.TestContext;
import com.sequenceiq.it.cloudbreak.newway.entity.stack.StackTestDto;

public class ShowClusterDefinitionUtil {

    private ShowClusterDefinitionUtil() {

    }

    public static StackTestDto checkFutureClusterDefinition(TestContext testContext, StackTestDto stackTestDto, CloudbreakClient cloudbreakClient) {
        String extendedClusterDefinitionText = stackTestDto.getGeneratedClusterDefinition().getClusterDefinitionText();
        validateGeneratedClusterDefinition(extendedClusterDefinitionText);
        return stackTestDto;
    }

    public static StackTestDto checkGeneratedClusterDefinition(TestContext testContext, StackTestDto stackTestDto, CloudbreakClient cloudbreakClient) {
        String extendedClusterDefinitionText = stackTestDto.getResponse().getCluster().getExtendedClusterDefinitionText();
        validateGeneratedClusterDefinition(extendedClusterDefinitionText);
        return stackTestDto;
    }

    private static void validateGeneratedClusterDefinition(String extendedClusterDefinitionText) {
        if (Strings.isNullOrEmpty(extendedClusterDefinitionText)) {
            throw new TestFailException("Generated Cluster Definition does not exist");
        } else if (!isJSONValid(extendedClusterDefinitionText)) {
            throw new TestFailException("Generated Cluster Definition is not a valid json");
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
