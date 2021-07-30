package com.sequenceiq.cloudbreak.core.flow2.cluster.cmsync.handler;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.instanceOf;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
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
import com.sequenceiq.cloudbreak.common.exception.NotFoundException;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.reactor.api.event.resource.CmSyncRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.resource.CmSyncResult;
import com.sequenceiq.cloudbreak.service.stack.StackService;
import com.sequenceiq.cloudbreak.service.upgrade.sync.CmSyncImageCollectorService;
import com.sequenceiq.cloudbreak.service.upgrade.sync.CmSyncerService;
import com.sequenceiq.cloudbreak.service.upgrade.sync.operationresult.CmSyncOperationSummary;
import com.sequenceiq.flow.reactor.api.handler.HandlerEvent;

@ExtendWith(MockitoExtension.class)
public class CmSyncHandlerTest {

    private static final long STACK_ID = 1L;

    private static final String IMAGE_UUID_1 = "imageUuid1";

    private static final String USER_CRN = "userCrn";

    @Mock
    private CmSyncImageCollectorService cmSyncImageCollectorService;

    @Mock
    private CmSyncerService cmSyncerService;

    @Mock
    private StackService stackService;

    @InjectMocks
    private CmSyncHandler underTest;

    @Test
    void testAcceptWhenSuccess() {
        Set<String> candidateImageUuids = Set.of(IMAGE_UUID_1);
        HandlerEvent<CmSyncRequest> event = getCmSyncRequestHandlerEvent(candidateImageUuids);
        Stack stack = new Stack();
        when(stackService.getById(STACK_ID)).thenReturn(stack);
        Set<Image> foundImages = Set.of(mock(Image.class));
        when(cmSyncImageCollectorService.collectImages(USER_CRN, stack, candidateImageUuids)).thenReturn(foundImages);
        CmSyncOperationSummary cmSyncOperationSummary = CmSyncOperationSummary.builder().withSuccess("").build();
        when(cmSyncerService.syncFromCmToDb(stack, foundImages)).thenReturn(cmSyncOperationSummary);

        Selectable result = underTest.doAccept(event);

        assertEquals("CMSYNCRESULT", result.selector());
        verify(stackService).getById(STACK_ID);
        verify(cmSyncImageCollectorService).collectImages(USER_CRN, stack, candidateImageUuids);
        verify(cmSyncerService).syncFromCmToDb(stack, foundImages);
    }

    @Test
    void testAcceptWhenStackNotFound() {
        Set<String> candidateImageUuids = Set.of(IMAGE_UUID_1);
        HandlerEvent<CmSyncRequest> event = getCmSyncRequestHandlerEvent(candidateImageUuids);
        Exception exception = new NotFoundException("Stack not found");
        when(stackService.getById(STACK_ID)).thenThrow(exception);

        CmSyncResult result = (CmSyncResult) underTest.doAccept(event);

        assertEquals("CMSYNCRESULT_ERROR", result.selector());
        assertEquals(exception, result.getErrorDetails());
        assertEquals("Unexpected error during syncing from CM", result.getStatusReason());
        verify(stackService).getById(STACK_ID);
        verify(cmSyncImageCollectorService, never()).collectImages(anyString(), any(), any());
        verify(cmSyncerService, never()).syncFromCmToDb(any(), any());
    }

    @Test
    void testAcceptWhenOperationSummaryIsFail() {
        Set<String> candidateImageUuids = Set.of(IMAGE_UUID_1);
        HandlerEvent<CmSyncRequest> event = getCmSyncRequestHandlerEvent(candidateImageUuids);
        Stack stack = new Stack();
        when(stackService.getById(STACK_ID)).thenReturn(stack);
        Set<Image> foundImages = Set.of(mock(Image.class));
        when(cmSyncImageCollectorService.collectImages(USER_CRN, stack, candidateImageUuids)).thenReturn(foundImages);
        CmSyncOperationSummary cmSyncOperationSummary = CmSyncOperationSummary.builder().withError("Error").build();
        when(cmSyncerService.syncFromCmToDb(stack, foundImages)).thenReturn(cmSyncOperationSummary);

        CmSyncResult result = (CmSyncResult) underTest.doAccept(event);

        assertEquals("CMSYNCRESULT_ERROR", result.selector());
        assertThat(result.getErrorDetails(), instanceOf(CloudbreakServiceException.class));
        assertEquals("Syncing CM version and installed parcels encountered failures. Details: Error", result.getErrorDetails().getMessage());
        assertEquals("Syncing CM version and installed parcels encountered failures. Details: Error", result.getStatusReason());
    }

    private HandlerEvent<CmSyncRequest> getCmSyncRequestHandlerEvent(Set<String> candidateImageUuids) {
        CmSyncRequest cmSyncRequest = new CmSyncRequest(STACK_ID, candidateImageUuids, USER_CRN);
        HandlerEvent<CmSyncRequest> event = mock(HandlerEvent.class);
        when(event.getData()).thenReturn(cmSyncRequest);
        return event;
    }

}