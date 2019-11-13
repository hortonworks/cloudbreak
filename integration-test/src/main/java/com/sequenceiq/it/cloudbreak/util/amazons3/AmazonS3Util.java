package com.sequenceiq.it.cloudbreak.util.amazons3;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.it.cloudbreak.SdxClient;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.sdx.SdxTestDto;
import com.sequenceiq.it.cloudbreak.util.amazons3.action.S3ClientActions;

@Component
public class AmazonS3Util {
    @Inject
    private S3ClientActions s3ClientActions;

    private AmazonS3Util() {
    }

    public SdxTestDto delete(TestContext testContext, SdxTestDto sdxTestDto, SdxClient sdxClient) {
        return s3ClientActions.deleteNonVersionedBucket(testContext, sdxTestDto, sdxClient);
    }

    public SdxTestDto list(TestContext testContext, SdxTestDto sdxTestDto, SdxClient sdxClient) {
        return s3ClientActions.listBucket(testContext, sdxTestDto, sdxClient);
    }
}
