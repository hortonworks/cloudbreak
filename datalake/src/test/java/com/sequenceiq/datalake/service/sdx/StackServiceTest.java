package com.sequenceiq.datalake.service.sdx;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.StackV4Endpoint;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.StackV4Response;

@ExtendWith(MockitoExtension.class)
class StackServiceTest {
    @Mock
    private StackV4Endpoint stackV4Endpoint;

    @InjectMocks
    private StackService underTest;

    @Test
    void testSdxGetDetailsWithResources() {
        StackV4Response stackResponse = mock(StackV4Response.class);
        when(stackV4Endpoint.getWithResources(anyLong(), anyString(), any(), anyString())).thenReturn(stackResponse);
        Set<String> entries = Set.of();

        StackV4Response result = underTest.getDetailWithResources("test", entries, "accountId");
        assertEquals(stackResponse, result);
        verify(stackV4Endpoint).getWithResources(0L, "test", entries, "accountId");
    }

    @Test
    void testSdxGetDetailsWithResourcesNull() {
        when(stackV4Endpoint.getWithResources(anyLong(), anyString(), any(), anyString())).thenThrow(new jakarta.ws.rs.NotFoundException("test"));
        Set<String> entries = Set.of();

        StackV4Response result = underTest.getDetailWithResources("test", entries, "accountId");
        assertNull(result);
        verify(stackV4Endpoint).getWithResources(0L, "test", entries, "accountId");
    }
}