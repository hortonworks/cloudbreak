package com.sequenceiq.cloudbreak.service.stackpatch;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.domain.stack.StackPatch;
import com.sequenceiq.cloudbreak.domain.stack.StackPatchStatus;
import com.sequenceiq.cloudbreak.domain.stack.StackPatchType;
import com.sequenceiq.cloudbreak.repository.StackPatchRepository;

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
        when(stackPatchRepository.findByStackIdAndType(STACK_ID, STACK_PATCH_TYPE)).thenReturn(Optional.of(stackPatch));

        StackPatch result = underTest.getOrCreate(STACK_ID, STACK_PATCH_TYPE);

        assertThat(result).isEqualTo(stackPatch);
        verify(stackPatchRepository, never()).save(any());
    }

    @Test
    void getOrCreateShouldCreateWhenDoesNotExist() {
        when(stackPatchRepository.findByStackIdAndType(STACK_ID, STACK_PATCH_TYPE)).thenReturn(Optional.empty());

        StackPatch result = underTest.getOrCreate(STACK_ID, STACK_PATCH_TYPE);

        assertThat(result)
                .returns(STACK_ID, StackPatch::getStackId)
                .returns(STACK_PATCH_TYPE, StackPatch::getType);
        verify(stackPatchRepository).save(result);
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
    void updateStatusWithReason() {
        StackPatch stackPatch = new StackPatch();
        String reason = "reason";
        StackPatchStatus status = StackPatchStatus.SCHEDULED;
        StackPatch result = underTest.updateStatusAndReportUsage(stackPatch, status, reason);

        assertThat(result)
                .returns(status, StackPatch::getStatus)
                .returns(reason, StackPatch::getStatusReason);
        verify(stackPatchRepository).save(stackPatch);
        verify(stackPatchUsageReporterService).reportUsage(stackPatch);
    }

    @Test
    void findByTypeAndStackIdIn() {
        List<StackPatch> stackPatches = List.of(new StackPatch());
        List<Long> stackIds = List.of(1L, 2L);
        when(stackPatchRepository.findByTypeAndStackIdIn(STACK_PATCH_TYPE, stackIds)).thenReturn(stackPatches);

        List<StackPatch> result = underTest.findAllByTypeForStackIds(STACK_PATCH_TYPE, stackIds);

        assertThat(result).isEqualTo(stackPatches);
        verify(stackPatchRepository).findByTypeAndStackIdIn(STACK_PATCH_TYPE, stackIds);
    }

}