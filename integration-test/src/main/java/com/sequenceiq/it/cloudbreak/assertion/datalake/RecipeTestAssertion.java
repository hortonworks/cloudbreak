package com.sequenceiq.it.cloudbreak.assertion.datalake;

import java.util.Collections;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.instancegroup.InstanceGroupV4Response;
import com.sequenceiq.it.cloudbreak.assertion.Assertion;
import com.sequenceiq.it.cloudbreak.dto.AbstractSdxTestDto;
import com.sequenceiq.it.cloudbreak.log.Log;
import com.sequenceiq.it.cloudbreak.microservice.SdxClient;
import com.sequenceiq.it.cloudbreak.util.ssh.SshJUtil;

public class RecipeTestAssertion {

    private static final Logger LOGGER = LoggerFactory.getLogger(RecipeTestAssertion.class);

    private RecipeTestAssertion() {
    }

    public static <T extends AbstractSdxTestDto> Assertion<T, SdxClient> validateFilesOnHost(List<String> hostGroupNames, String filePath, String fileName,
            long requiredNumberOfFiles, SshJUtil sshJUtil) {
        return (testContext, testDto, sdxClient) -> {
            Log.log(LOGGER, " Checking generated file(s) by recipe at datalake instance(s) (%s) on '%s' path by '%s' name! ", testDto.getCrn(),
                    filePath, fileName);
            sshJUtil.checkFilesOnHostByNameAndPath(testDto, getInstanceGroups(testDto, sdxClient), hostGroupNames, filePath,
                    fileName, 1, null, null);

            return testDto;
        };
    }

    private static <T extends AbstractSdxTestDto> List<InstanceGroupV4Response> getInstanceGroups(T testDto, SdxClient client) {
        return client.getDefaultClient(testDto.getTestContext())
                .sdxEndpoint()
                .getDetailByCrn(testDto.getCrn(), Collections.emptySet())
                .getStackV4Response().getInstanceGroups();
    }
}
