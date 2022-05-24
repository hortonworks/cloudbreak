package com.sequenceiq.it.cloudbreak.assertion.freeipa;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.it.cloudbreak.FreeIpaClient;
import com.sequenceiq.it.cloudbreak.assertion.Assertion;
import com.sequenceiq.it.cloudbreak.dto.freeipa.FreeIpaTestDto;
import com.sequenceiq.it.cloudbreak.util.ssh.SshJUtil;

public class RecipeTestAssertion {

    private static final Logger LOGGER = LoggerFactory.getLogger(RecipeTestAssertion.class);

    private RecipeTestAssertion() {
    }

    public static Assertion<FreeIpaTestDto, FreeIpaClient> validateFilesOnFreeIpa(String filePath, String fileName, long requiredNumberOfFiles,
            SshJUtil sshJUtil) {
        return (testContext, freeIpaTestDto, freeIpaClient) -> {
            sshJUtil.checkFilesOnFreeIpaByNameAndPath(freeIpaTestDto, freeIpaTestDto.getEnvironmentCrn(), freeIpaClient, filePath, fileName, 1,
                    null, null);

            return freeIpaTestDto;
        };
    }
}
