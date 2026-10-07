package com.sequenceiq.stackpatcher.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.stackpatcher.TestStackPatchResource;
import com.sequenceiq.stackpatcher.domain.StackPatch;
import com.sequenceiq.stackpatcher.domain.StackPatchStatus;
import com.sequenceiq.stackpatcher.domain.StackPatchType;
import com.sequenceiq.stackpatcher.repository.StackPatchRepository;

@ExtendWith(MockitoExtension.class)
class StackPatchServiceTest {

    private static final Long STACK_ID = 123L;

    private static final StackPatchType STACK_PATCH_TYPE = StackPatchType.MOCK;

    @InjectMocks
    private StackPatchService underTest;

    @Mock
    private StackPatchRepository stackPatchRepository;

    @Mock
    private StackPatchUsageReporterService stackPatchUsageReporterService;

    @BeforeEach
    void setUp() {
        lenient().when(stackPatchRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void getOrCreateShouldNotCreateWhenExists() {
        StackPatch stackPatch = new StackPatch();
        when(stackPatchRepository.findByResourceIdAndType(STACK_ID, STACK_PATCH_TYPE)).thenReturn(Optional.of(stackPatch));

        StackPatch result = underTest.getOrCreate(STACK_ID, STACK_PATCH_TYPE);

        assertThat(result).isEqualTo(stackPatch);
        verify(stackPatchRepository, never()).save(any());
    }

    @Test
    void getOrCreateShouldCreateWhenDoesNotExist() {
        when(stackPatchRepository.findByResourceIdAndType(STACK_ID, STACK_PATCH_TYPE)).thenReturn(Optional.empty());

        StackPatch result = underTest.getOrCreate(STACK_ID, STACK_PATCH_TYPE);

        assertThat(result)
                .returns(STACK_ID, StackPatch::getResourceId)
                .returns(STACK_PATCH_TYPE, StackPatch::getType);
        verify(stackPatchRepository).save(result);
    }

    @Test
    void deleteByResourceId() {
        underTest.deleteByResourceId(STACK_ID);

        verify(stackPatchRepository).deleteByResourceId(STACK_ID);
    }

    @Test
    void updateStatusWithoutReason() {
        StackPatch stackPatch = new StackPatch();
        StackPatchStatus status = StackPatchStatus.SCHEDULED;
        StackPatch result = underTest.updateStatus(stackPatch, status);

        assertThat(result)
                .returns(status, StackPatch::getStatus)
                .returns("", StackPatch::getStatusReason);
        verify(stackPatchRepository).save(stackPatch);
    }

    @Test
    void updateStatusWithReasonShouldNotNotify() {
        StackPatch stackPatch = new StackPatch();
        String reason = "reason";
        StackPatchStatus status = StackPatchStatus.SCHEDULED;
        StackPatch result = underTest.updateStatus(stackPatch, status, reason);

        assertThat(result)
                .returns(status, StackPatch::getStatus)
                .returns(reason, StackPatch::getStatusReason);
        verify(stackPatchRepository).save(stackPatch);
        verifyNoInteractions(stackPatchUsageReporterService);
    }

    @Test
    void updateStatusAndNotifyWithReason() {
        StackPatch stackPatch = new StackPatch(STACK_ID, STACK_PATCH_TYPE);
        TestStackPatchResource resource = new TestStackPatchResource(STACK_ID, "crn");
        String reason = "reason";
        StackPatchStatus status = StackPatchStatus.FAILED;
        StackPatch result = underTest.updateStatusAndNotify(stackPatch, resource, status, reason);

        assertThat(result)
                .returns(status, StackPatch::getStatus)
                .returns(reason, StackPatch::getStatusReason);
        verify(stackPatchRepository).save(stackPatch);
        verify(stackPatchUsageReporterService).reportStatusChange(resource, STACK_PATCH_TYPE, status, reason);
    }

    @Test
    void updateStatusAndNotifyWithoutReason() {
        StackPatch stackPatch = new StackPatch(STACK_ID, STACK_PATCH_TYPE);
        TestStackPatchResource resource = new TestStackPatchResource(STACK_ID, "crn");
        StackPatchStatus status = StackPatchStatus.FIXED;
        StackPatch result = underTest.updateStatusAndNotify(stackPatch, resource, status);

        assertThat(result)
                .returns(status, StackPatch::getStatus)
                .returns("", StackPatch::getStatusReason);
        verify(stackPatchRepository).save(stackPatch);
        verify(stackPatchUsageReporterService).reportStatusChange(resource, STACK_PATCH_TYPE, status, "");
    }

    @Test
    void findByTypeAndResourceIdIn() {
        List<StackPatch> stackPatches = List.of(new StackPatch());
        List<Long> stackIds = List.of(1L, 2L);
        when(stackPatchRepository.findByTypeAndResourceIdIn(STACK_PATCH_TYPE, stackIds)).thenReturn(stackPatches);

        List<StackPatch> result = underTest.findAllByTypeForResourceIds(STACK_PATCH_TYPE, stackIds);

        assertThat(result).isEqualTo(stackPatches);
        verify(stackPatchRepository).findByTypeAndResourceIdIn(STACK_PATCH_TYPE, stackIds);
    }

}