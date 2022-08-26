package com.sequenceiq.it.cloudbreak.assertion.freeipa;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.instance.InstanceMetadataType;
import com.sequenceiq.it.cloudbreak.FreeIpaClient;
import com.sequenceiq.it.cloudbreak.assertion.Assertion;
import com.sequenceiq.it.cloudbreak.dto.freeipa.FreeIpaTestDto;
import com.sequenceiq.it.cloudbreak.log.Log;
import com.sequenceiq.it.cloudbreak.util.ssh.SshJUtil;

public class RecipeTestAssertion {

    private static final Logger LOGGER = LoggerFactory.getLogger(RecipeTestAssertion.class);

    private RecipeTestAssertion() {
    }

    public static Assertion<FreeIpaTestDto, FreeIpaClient> validateFilesOnFreeIpa(String filePath, String fileName, long requiredNumberOfFiles,
            SshJUtil sshJUtil) {
        return validateFilesOnFreeIpa(InstanceMetadataType.GATEWAY_PRIMARY, filePath, fileName, requiredNumberOfFiles, sshJUtil);
    }

    public static Assertion<FreeIpaTestDto, FreeIpaClient> validateFilesOnFreeIpa(InstanceMetadataType istanceMetadataType, String filePath, String fileName,
            long requiredNumberOfFiles, SshJUtil sshJUtil) {
        return (testContext, freeIpaTestDto, freeIpaClient) -> {
            Log.log(LOGGER, " Checking generated file(s) by recipe at freeIpa (%s) instance(s) '%s' on '%s' path by '%s' name! ",
                    freeIpaTestDto.getCrn(), istanceMetadataType.toString(), filePath, fileName);
            sshJUtil.checkFilesOnFreeIpaByNameAndPath(freeIpaTestDto, freeIpaTestDto.getEnvironmentCrn(), freeIpaClient, istanceMetadataType, filePath,
                    fileName, 1, null, null);

            return freeIpaTestDto;
        };
    }
}
