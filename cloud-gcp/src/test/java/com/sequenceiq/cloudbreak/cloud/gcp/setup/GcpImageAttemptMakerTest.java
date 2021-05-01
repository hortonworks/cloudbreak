package com.sequenceiq.cloudbreak.cloud.gcp.setup;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.Assert;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import com.dyngr.core.AttemptResult;
import com.dyngr.core.AttemptState;
import com.google.api.services.storage.Storage;
import com.google.api.services.storage.model.RewriteResponse;
import com.google.api.services.storage.model.StorageObject;

@ExtendWith(MockitoExtension.class)
public class GcpImageAttemptMakerTest {

    Storage storage = mock(Storage.class);

    private GcpImageAttemptMaker underTest;

    @BeforeEach
    public void before() {
        underTest = new GcpImageAttemptMaker(
                "token123",
                "source-bucket",
                "source-key",
                "dest-bucket",
                "dest-key",
                storage
        );
    }

    @Test
    public void testGetRewriteTokenWhenTokenSpecified() {
        Assert.assertEquals("token123", underTest.getRewriteToken());
    }

    @Test
    public void testSetRewriteTokenWhenTokenSpecified() {
        Assert.assertEquals("token123", underTest.getRewriteToken());
        underTest.setRewriteToken("token456");
        Assert.assertEquals("token456", underTest.getRewriteToken());
    }

    @Test
    public void testProcessWhenFinishedSuccess() throws Exception {
        Storage.Objects objects = mock(Storage.Objects.class);
        Storage.Objects.Rewrite rewrite = mock(Storage.Objects.Rewrite.class);
        RewriteResponse rewriteResponse = mock(RewriteResponse.class);

        when(storage.objects()).thenReturn(objects);
        when(objects.rewrite(anyString(), anyString(), anyString(), anyString(), any(StorageObject.class))).thenReturn(rewrite);
        when(rewrite.execute()).thenReturn(rewriteResponse);
        when(rewriteResponse.getRewriteToken()).thenReturn("token");
        when(rewriteResponse.getTotalBytesRewritten()).thenReturn(1L);
        when(rewriteResponse.getObjectSize()).thenReturn(1L);
        when(rewriteResponse.getDone()).thenReturn(true);

        AttemptResult process = underTest.process();

        Assert.assertEquals("token", underTest.getRewriteToken());
        Assert.assertEquals(AttemptState.FINISH, process.getState());
    }

    @Test
    public void testProcessWhenContinue() throws Exception {
        Storage.Objects objects = mock(Storage.Objects.class);
        Storage.Objects.Rewrite rewrite = mock(Storage.Objects.Rewrite.class);
        RewriteResponse rewriteResponse = mock(RewriteResponse.class);

        when(storage.objects()).thenReturn(objects);
        when(objects.rewrite(anyString(), anyString(), anyString(), anyString(), any(StorageObject.class))).thenReturn(rewrite);
        when(rewrite.execute()).thenReturn(rewriteResponse);
        when(rewriteResponse.getRewriteToken()).thenReturn("token");
        when(rewriteResponse.getTotalBytesRewritten()).thenReturn(1L);
        when(rewriteResponse.getObjectSize()).thenReturn(1L);
        when(rewriteResponse.getDone()).thenReturn(false);

        AttemptResult process = underTest.process();

        Assert.assertEquals("token", underTest.getRewriteToken());
        Assert.assertEquals(AttemptState.CONTINUE, process.getState());
    }
}