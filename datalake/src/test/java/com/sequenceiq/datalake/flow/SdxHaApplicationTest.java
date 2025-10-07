package com.sequenceiq.datalake.flow;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.datalake.service.sdx.SdxService;

@ExtendWith(MockitoExtension.class)
class SdxHaApplicationTest {
    @InjectMocks
    private SdxHaApplication sdxHaApplication;

    @Mock
    private SdxService sdxService;

    @Test
    void getDeletingResourcesDoesNotIncludeResizingSdx() {
        Set<Long> resources = Set.of(1L, 2L, 3L);
        when(sdxService.findByResourceIdsAndStatuses(eq(resources), any())).thenReturn(resources);
        when(sdxService.findAllNotDetachedIdsByIds(resources)).thenReturn(List.of(1L, 3L));

        Set<Long> deletingResources = sdxHaApplication.getDeletingResources(resources);
        assertTrue(deletingResources.containsAll(List.of(1L, 3L)));
    }

    @Test
    void getAllDeletingResourcesDoesNotIncludeResizingSdx() {
        Set<Long> resources = Set.of(1L, 2L, 3L);
        when(sdxService.findByResourceIdsAndStatuses(any(), any())).thenReturn(resources);
        when(sdxService.findAllNotDetachedIdsByIds(resources)).thenReturn(List.of(1L, 3L));

        Set<Long> deletingResources = sdxHaApplication.getAllDeletingResources();
        assertTrue(deletingResources.containsAll(List.of(1L, 3L)));
    }
}
