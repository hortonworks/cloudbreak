package com.sequenceiq.cloudbreak.service.validation;

import static com.sequenceiq.cloudbreak.service.validation.UpdatePublicDnsEntriesInPemValidator.NON_TRIGGERABLE_FINAL_STATES;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status;
import com.sequenceiq.cloudbreak.api.model.StatusKind;
import com.sequenceiq.cloudbreak.common.exception.BadRequestException;
import com.sequenceiq.cloudbreak.dto.StackDto;

class UpdatePublicDnsEntriesInPemValidatorTest {

    private UpdatePublicDnsEntriesInPemValidator underTest =  new UpdatePublicDnsEntriesInPemValidator();

    @Test
    void testValidateWhenTheStackStatusIsInProgressOrNonTriggerableFinal() {
        StackDto stack = mock(StackDto.class);
        Set<Status> nonTriggerableStatuses = Arrays.stream(Status.values())
                .filter(status -> StatusKind.PROGRESS.equals(status.getStatusKind()))
                .collect(Collectors.toSet());
        nonTriggerableStatuses.addAll(NON_TRIGGERABLE_FINAL_STATES);

        nonTriggerableStatuses.forEach(status -> {
            when(stack.getStatus()).thenReturn(status);

            assertThrows(BadRequestException.class, () -> underTest.validate(stack));
        });
    }

    @ParameterizedTest
    @EnumSource(names = { "AVAILABLE", "UPDATE_FAILED", "START_FAILED", "MAINTENANCE_MODE_ENABLED", "AMBIGUOUS", "UNREACHABLE", "NODE_FAILURE" })
    void testValidateWhenTheStackStatusIsATriggerable(Status status) {
        StackDto stack = mock(StackDto.class);
        when(stack.getStatus()).thenReturn(status);

        assertDoesNotThrow(() -> underTest.validate(stack));
    }
}