package com.sequenceiq.it.cloudbreak.assertion.recipe;

import java.util.Collections;
import java.util.List;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.instancegroup.InstanceGroupV4Response;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.instance.InstanceMetadataType;
import com.sequenceiq.it.cloudbreak.assertion.Assertion;
import com.sequenceiq.it.cloudbreak.dto.AbstractSdxTestDto;
import com.sequenceiq.it.cloudbreak.dto.freeipa.FreeIpaTestDto;
import com.sequenceiq.it.cloudbreak.log.Log;
import com.sequenceiq.it.cloudbreak.microservice.FreeIpaClient;
import com.sequenceiq.it.cloudbreak.microservice.SdxClient;
import com.sequenceiq.it.cloudbreak.util.ssh.action.SshJClientActions;

@Component
public class RecipeTestAssertion {

    private static final Logger LOGGER = LoggerFactory.getLogger(RecipeTestAssertion.class);

    @Inject
    private SshJClientActions sshJClientActions;

    public <T extends AbstractSdxTestDto> Assertion<T, SdxClient> validateFilesOnHost(List<String> hostGroupNames, String filePath, String fileName,
            long requiredNumberOfFiles) {
        return (testContext, testDto, sdxClient) -> {
            Log.log(LOGGER, " Checking generated file(s) by recipe at datalake instance(s) (%s) on '%s' path by '%s' name! ",
                    testDto.getCrn(), filePath, fileName);
            sshJClientActions.checkFilesByNameAndPath(testDto, getInstanceGroups(testDto, sdxClient), hostGroupNames, filePath, fileName,
                    requiredNumberOfFiles, null, null);
            return testDto;
        };
    }

    public Assertion<FreeIpaTestDto, FreeIpaClient> validateFilesOnFreeIpa(String filePath, String fileName, long requiredNumberOfFiles) {
        return validateFilesOnFreeIpa(InstanceMetadataType.GATEWAY_PRIMARY, filePath, fileName, requiredNumberOfFiles);
    }

    public Assertion<FreeIpaTestDto, FreeIpaClient> validateFilesOnFreeIpa(InstanceMetadataType instanceMetadataType, String filePath, String fileName,
            long requiredNumberOfFiles) {
        return (testContext, freeIpaTestDto, freeIpaClient) -> {
            Log.log(LOGGER, " Checking generated file(s) by recipe at freeIpa (%s) '%s' instance(s) on '%s' path by '%s' name! ",
                    freeIpaTestDto.getCrn(), instanceMetadataType.toString(), filePath, fileName);
            sshJClientActions.checkFilesByNameAndPath(freeIpaTestDto, freeIpaTestDto.getEnvironmentCrn(), freeIpaClient, instanceMetadataType, filePath,
                    fileName, requiredNumberOfFiles, null, null);
            return freeIpaTestDto;
        };
    }

    private static <T extends AbstractSdxTestDto> List<InstanceGroupV4Response> getInstanceGroups(T testDto, SdxClient client) {
        return client.getDefaultClient(testDto.getTestContext())
                .sdxEndpoint()
                .getDetailByCrn(testDto.getCrn(), Collections.emptySet())
                .getStackV4Response().getInstanceGroups();
    }
}
