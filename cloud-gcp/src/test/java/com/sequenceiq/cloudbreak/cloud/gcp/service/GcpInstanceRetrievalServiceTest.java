package com.sequenceiq.cloudbreak.cloud.gcp.service;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.google.api.services.compute.Compute;
import com.google.api.services.compute.model.Instance;
import com.sequenceiq.cloudbreak.cloud.gcp.util.GcpStackUtil;

@ExtendWith(MockitoExtension.class)
class GcpInstanceRetrievalServiceTest {

    private static final String PROJECT_ID = "test-project";

    private static final String ZONE = "us-central1-a";

    private static final String INSTANCE_ID = "instance1";

    @Mock
    private GcpStackUtil gcpStackUtil;

    @Mock
    private Compute compute;

    @InjectMocks
    private GcpInstanceRetrievalService underTest;

    @Test
    void testGetInstanceDelegatesToGcpStackUtil() throws Exception {
        Instance instance = new Instance().setName(INSTANCE_ID);
        when(gcpStackUtil.getComputeInstanceWithId(eq(compute), eq(PROJECT_ID), eq(ZONE), eq(INSTANCE_ID))).thenReturn(instance);

        Instance result = underTest.getInstance(compute, PROJECT_ID, ZONE, INSTANCE_ID);

        assertSame(instance, result);
        verify(gcpStackUtil).getComputeInstanceWithId(compute, PROJECT_ID, ZONE, INSTANCE_ID);
    }
}
