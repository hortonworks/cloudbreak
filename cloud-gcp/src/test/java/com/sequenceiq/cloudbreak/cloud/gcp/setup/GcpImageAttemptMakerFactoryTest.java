package com.sequenceiq.cloudbreak.cloud.gcp.setup;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import com.google.api.services.storage.Storage;

@ExtendWith(MockitoExtension.class)
public class GcpImageAttemptMakerFactoryTest {

    @InjectMocks
    public GcpImageAttemptMakerFactory underTest;

    @Test
    public void testCreateGcpImageAttemptMakerSimplFlowAndRewriteTokenShouldMatch() {
        Storage storage = mock(Storage.class);
        GcpImageAttemptMaker result = underTest.create(
                "rewriteToken",
                "sourceBucket",
                "sourceKey",
                "destBucket",
                "destKey",
                storage
        );

        GcpImageAttemptMaker expected = new GcpImageAttemptMaker(
                "rewriteToken",
                "sourceBucket",
                "sourceKey",
                "destBucket",
                "destKey",
                storage
        );
        assertEquals(expected.getRewriteToken(), result.getRewriteToken());
    }
}