package com.sequenceiq.it.cloudbreak.assertion.datalake;

import java.util.Collections;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.instancegroup.InstanceGroupV4Response;
import com.sequenceiq.it.cloudbreak.SdxClient;
import com.sequenceiq.it.cloudbreak.assertion.Assertion;
import com.sequenceiq.it.cloudbreak.dto.AbstractSdxTestDto;
import com.sequenceiq.it.cloudbreak.util.ssh.SshJUtil;

public class RecipeTestAssertion {

    private static final Logger LOGGER = LoggerFactory.getLogger(RecipeTestAssertion.class);

    private RecipeTestAssertion() {
    }

    public static <T extends AbstractSdxTestDto> Assertion<T, SdxClient> validateFilesOnHost(List<String> hostGroupNames, String filePath, String fileName,
            long requiredNumberOfFiles, String user, String password, SshJUtil sshJUtil) {
        return (testContext, testDto, sdxClient) -> {
            sshJUtil.checkFilesOnHostByNameAndPath(testDto, getInstanceGroups(testDto, sdxClient), hostGroupNames, filePath,
                    fileName, 1, null, null);

            return testDto;
        };
    }

    private static <T extends AbstractSdxTestDto> List<InstanceGroupV4Response> getInstanceGroups(T testDto, SdxClient client) {
        return client.getDefaultClient()
                .sdxEndpoint()
                .getDetailByCrn(testDto.getCrn(), Collections.emptySet())
                .getStackV4Response().getInstanceGroups();
    }
}
