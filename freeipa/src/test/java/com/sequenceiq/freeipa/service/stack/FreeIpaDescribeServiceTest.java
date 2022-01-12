package com.sequenceiq.freeipa.service.stack;

import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.cloudbreak.common.exception.BadRequestException;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.Status;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.describe.DescribeFreeIpaResponse;
import com.sequenceiq.freeipa.converter.stack.StackToDescribeFreeIpaResponseConverter;
import com.sequenceiq.freeipa.entity.FreeIpa;
import com.sequenceiq.freeipa.entity.ImageEntity;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.entity.StackStatus;
import com.sequenceiq.freeipa.entity.UserSyncStatus;
import com.sequenceiq.freeipa.service.freeipa.FreeIpaService;
import com.sequenceiq.freeipa.service.freeipa.user.UserSyncStatusService;
import com.sequenceiq.freeipa.service.image.ImageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FreeIpaDescribeServiceTest {

    private static final String ENVIRONMENT_CRN = "test:environment:crn";

    private static final Long STACK_ID = 1L;

    private static final String STACK_NAME = "stack-name";

    private static final String ACCOUNT_ID = "account:id";

    @InjectMocks
    private FreeIpaDescribeService underTest;

    @Mock
    private StackService stackService;

    @Mock
    private ImageService imageService;

    @Mock
    private FreeIpaService freeIpaService;

    @Mock
    private UserSyncStatusService userSyncStatusService;

    @Mock
    private StackToDescribeFreeIpaResponseConverter stackToDescribeFreeIpaResponseConverter;

    @Mock
    private EntitlementService entitlementService;

    private Stack stack;

    @BeforeEach
    void setUp() {
        stack = new Stack();
        stack.setId(STACK_ID);
        stack.setName(STACK_NAME);
        StackStatus stackStatus = new StackStatus();
        stackStatus.setStatus(Status.AVAILABLE);
        stack.setStackStatus(stackStatus);
    }

    @Test
    void describeAll() {
        DescribeFreeIpaResponse describeResponse = mock(DescribeFreeIpaResponse.class);
        ImageEntity image = mock(ImageEntity.class);
        FreeIpa freeIpa = mock(FreeIpa.class);
        UserSyncStatus userSyncStatus = mock(UserSyncStatus.class);
        when(stackService.findMultipleByEnvironmentCrnAndAccountIdEvenIfTerminatedWithList(eq(ENVIRONMENT_CRN), eq(ACCOUNT_ID)))
                .thenReturn(Collections.singletonList(stack));
        when(imageService.getByStack(any())).thenReturn(image);
        when(freeIpaService.findByStackId(any())).thenReturn(freeIpa);
        when(userSyncStatusService.findByStack(any())).thenReturn(userSyncStatus);
        when(stackToDescribeFreeIpaResponseConverter.convert(any(), any(), any(), any(), any())).thenReturn(describeResponse);
        when(entitlementService.isFreeIpaRebuildEnabled(eq(ACCOUNT_ID))).thenReturn(true);

        assertEquals(List.of(describeResponse), underTest.describeAll(ENVIRONMENT_CRN, ACCOUNT_ID));
    }

    @Test
    void describeAllThrowsWhenEntitlementIsDisabled() {
        when(entitlementService.isFreeIpaRebuildEnabled(eq(ACCOUNT_ID))).thenReturn(false);

        assertThrows(BadRequestException.class, () -> underTest.describeAll(ENVIRONMENT_CRN, ACCOUNT_ID));
    }
}
