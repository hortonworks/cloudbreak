package com.sequenceiq.cloudbreak.core.flow2.cluster.cmsync.handler;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.instanceOf;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.cloud.model.catalog.Image;
import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.common.exception.CloudbreakServiceException;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.reactor.api.event.resource.CmSyncRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.resource.CmSyncResult;
import com.sequenceiq.cloudbreak.service.stack.StackService;
import com.sequenceiq.cloudbreak.service.upgrade.sync.CmSyncImageCollectorService;
import com.sequenceiq.cloudbreak.service.upgrade.sync.CmSyncImageUpdateService;
import com.sequenceiq.cloudbreak.service.upgrade.sync.CmSyncerService;
import com.sequenceiq.cloudbreak.service.upgrade.sync.operationresult.CmSyncOperationStatus;
import com.sequenceiq.cloudbreak.service.upgrade.sync.operationresult.CmSyncOperationSummary;
import com.sequenceiq.cloudbreak.service.upgrade.sync.template.ClusterManagerTemplateSyncService;
import com.sequenceiq.flow.reactor.api.handler.HandlerEvent;

@ExtendWith(MockitoExtension.class)
public class CmSyncHandlerTest {

    private static final long STACK_ID = 1L;

    private static final String IMAGE_UUID_1 = "imageUuid1";

    private static final String USER_CRN = "userCrn";

    @Mock
    private CmSyncImageUpdateService cmSyncImageUpdateService;

    @Mock
    private CmSyncImageCollectorService cmSyncImageCollectorService;

    @Mock
    private CmSyncerService cmSyncerService;

    @Mock
    private StackService stackService;

    @Mock
    private ClusterManagerTemplateSyncService clusterManagerTemplateSyncService;

    @InjectMocks
    private CmSyncHandler underTest;

    @Test
    void testAcceptWhenSuccess() {
        Set<String> candidateImageUuids = Set.of(IMAGE_UUID_1);
        HandlerEvent<CmSyncRequest> event = getCmSyncRequestHandlerEvent(candidateImageUuids);
        Stack stack = new Stack();
        stack.setId(1L);
        when(stackService.getByIdWithListsInTransaction(STACK_ID)).thenReturn(stack);
        doNothing().when(clusterManagerTemplateSyncService).sync(anyLong());
        Set<Image> foundImages = Set.of(mock(Image.class));
        when(cmSyncImageCollectorService.collectImages(stack, candidateImageUuids)).thenReturn(foundImages);
        CmSyncOperationStatus cmSyncOperationStatus = CmSyncOperationStatus.builder().withSuccess("").build();
        CmSyncOperationSummary cmSyncOperationSummary = new CmSyncOperationSummary(cmSyncOperationStatus);
        when(cmSyncerService.syncFromCmToDb(stack, foundImages)).thenReturn(cmSyncOperationSummary);

        Selectable result = underTest.doAccept(event);

        assertEquals("CMSYNCRESULT", result.selector());
        verify(stackService).getByIdWithListsInTransaction(STACK_ID);
        verify(clusterManagerTemplateSyncService).sync(anyLong());
        verify(cmSyncImageCollectorService).collectImages(stack, candidateImageUuids);
        verify(cmSyncerService).syncFromCmToDb(stack, foundImages);
    }

    @Test
    void testAcceptWhenStackNotFound() {
        Set<String> candidateImageUuids = Set.of(IMAGE_UUID_1);
        HandlerEvent<CmSyncRequest> event = getCmSyncRequestHandlerEvent(candidateImageUuids);
        Exception exception = new CloudbreakServiceException("errordetail");
        when(stackService.getByIdWithListsInTransaction(STACK_ID)).thenThrow(exception);

        CmSyncResult result = (CmSyncResult) underTest.doAccept(event);

        assertEquals("CMSYNCRESULT_ERROR", result.selector());
        assertThat(result.getErrorDetails(), instanceOf(CloudbreakServiceException.class));
        assertEquals("unexpected error: errordetail", result.getErrorDetails().getMessage());
        assertEquals("unexpected error: errordetail", result.getStatusReason());
        verify(stackService).getByIdWithListsInTransaction(STACK_ID);
        verify(cmSyncImageCollectorService, never()).collectImages(any(), any());
        verify(cmSyncerService, never()).syncFromCmToDb(any(), any());
    }

    @Test
    void testAcceptWhenOperationSummaryIsFail() {
        Set<String> candidateImageUuids = Set.of(IMAGE_UUID_1);
        HandlerEvent<CmSyncRequest> event = getCmSyncRequestHandlerEvent(candidateImageUuids);
        Stack stack = new Stack();
        when(stackService.getByIdWithListsInTransaction(STACK_ID)).thenReturn(stack);
        Set<Image> foundImages = Set.of(mock(Image.class));
        when(cmSyncImageCollectorService.collectImages(stack, candidateImageUuids)).thenReturn(foundImages);
        CmSyncOperationStatus cmSyncOperationStatus = CmSyncOperationStatus.builder().withError("My error description").build();
        CmSyncOperationSummary cmSyncOperationSummary = new CmSyncOperationSummary(cmSyncOperationStatus);
        when(cmSyncerService.syncFromCmToDb(stack, foundImages)).thenReturn(cmSyncOperationSummary);

        CmSyncResult result = (CmSyncResult) underTest.doAccept(event);

        assertEquals("CMSYNCRESULT_ERROR", result.selector());
        assertThat(result.getErrorDetails(), instanceOf(CloudbreakServiceException.class));
        assertEquals("My error description", result.getErrorDetails().getMessage());
        assertEquals("My error description", result.getStatusReason());
    }

    private HandlerEvent<CmSyncRequest> getCmSyncRequestHandlerEvent(Set<String> candidateImageUuids) {
        CmSyncRequest cmSyncRequest = new CmSyncRequest(STACK_ID, candidateImageUuids, USER_CRN);
        HandlerEvent<CmSyncRequest> event = mock(HandlerEvent.class);
        when(event.getData()).thenReturn(cmSyncRequest);
        return event;
    }

}