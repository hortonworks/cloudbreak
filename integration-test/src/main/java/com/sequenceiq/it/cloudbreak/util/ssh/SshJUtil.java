package com.sequenceiq.it.cloudbreak.util.ssh;

import java.util.List;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.it.cloudbreak.SdxClient;
import com.sequenceiq.it.cloudbreak.dto.sdx.SdxInternalTestDto;
import com.sequenceiq.it.cloudbreak.util.ssh.action.SshJClientActions;

@Component
public class SshJUtil {
    @Inject
    private SshJClientActions sshJClientActions;

    private SshJUtil() {
    }

    public SdxInternalTestDto checkFilesOnHostByNameAndPath(SdxInternalTestDto testDto, SdxClient sdxClient,
            List<String> hostGroupNames, String filePath, String fileName, long requiredNumberOfFiles) {
        return sshJClientActions.checkFilesByNameAndPath(testDto, sdxClient, hostGroupNames, filePath, fileName, requiredNumberOfFiles);
    }
}
