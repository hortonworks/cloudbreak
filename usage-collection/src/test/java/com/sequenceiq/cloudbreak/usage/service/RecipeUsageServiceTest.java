package com.sequenceiq.cloudbreak.usage.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.cloudera.thunderhead.service.common.usage.UsageProto;
import com.sequenceiq.cloudbreak.usage.UsageReporter;

@ExtendWith(MockitoExtension.class)
public class RecipeUsageServiceTest {

    private static final String NAME = "name";

    private static final String CRN = "crn";

    private static final String STACK_CRN = "stackcrn";

    private static final String INVALID_TYPE = "type";

    private static final String VALID_TYPE = "PRE_TERMINATION";

    private static final String HOST_GROUP = "hostGroup";

    @Mock
    private UsageReporter usageReporter;

    @InjectMocks
    private RecipeUsageService underTest;

    @Test
    public void testCreationUsageReportWhenErrorOccurs() {
        underTest.sendCreatedUsageReport(null, null, null);
        verifyNoInteractions(usageReporter);
    }

    @Test
    public void testDeletionUsageReportWhenErrorOccurs() {
        underTest.sendDeletedUsageReport(null, null, null);
        verifyNoInteractions(usageReporter);
    }

    @Test
    public void testAttachmentUsageReportWhenErrorOccurs() {
        underTest.sendAttachedUsageReport(null, null, null, null, null);
        verifyNoInteractions(usageReporter);
    }

    @Test
    public void testDetachmentUsageReportWhenErrorOccurs() {
        underTest.sendDetachedUsageReport(null, null, null, null, null);
        verifyNoInteractions(usageReporter);
    }

    @Test
    public void testClusterCreationUsageReportWhenErrorOccurs() {
        underTest.sendClusterCreationRecipeUsageReport(null, 0, null, null);
        verifyNoInteractions(usageReporter);
    }

    @Test
    public void testAttachmentUsageReportWithLimitedInformation() {
        doNothing().when(usageReporter).cdpRecipeEvent(any());

        underTest.sendAttachedUsageReport(NAME, Optional.empty(), Optional.empty(), STACK_CRN, Optional.empty());

        ArgumentCaptor<UsageProto.CDPRecipeEvent> eventCaptor = ArgumentCaptor.forClass(UsageProto.CDPRecipeEvent.class);
        verify(usageReporter).cdpRecipeEvent(eventCaptor.capture());
        UsageProto.CDPRecipeEvent event = eventCaptor.getValue();
        assertEquals("", event.getRecipeCrn());
        assertEquals(NAME, event.getName());
        assertEquals(UsageProto.CDPRecipeStatus.Value.ATTACHED, event.getStatus());
    }

    @Test
    public void testDetachmentUsageReportWithLimitedInformation() {
        doNothing().when(usageReporter).cdpRecipeEvent(any());

        underTest.sendDetachedUsageReport(NAME, Optional.empty(), Optional.empty(), STACK_CRN, Optional.empty());

        ArgumentCaptor<UsageProto.CDPRecipeEvent> eventCaptor = ArgumentCaptor.forClass(UsageProto.CDPRecipeEvent.class);
        verify(usageReporter).cdpRecipeEvent(eventCaptor.capture());
        UsageProto.CDPRecipeEvent event = eventCaptor.getValue();
        assertEquals("", event.getRecipeCrn());
        assertEquals(NAME, event.getName());
        assertEquals(UsageProto.CDPRecipeStatus.Value.DETACHED, event.getStatus());
    }

    @Test
    public void testClusterCreationUsageReportWithLimitedInformation() {
        doNothing().when(usageReporter).cdpClusterCreationRecipeEvent(any());

        underTest.sendClusterCreationRecipeUsageReport(STACK_CRN, 0, Optional.empty(), Optional.empty());

        ArgumentCaptor<UsageProto.CDPClusterCreationRecipeEvent> eventCaptor = ArgumentCaptor.forClass(UsageProto.CDPClusterCreationRecipeEvent.class);
        verify(usageReporter).cdpClusterCreationRecipeEvent(eventCaptor.capture());
        UsageProto.CDPClusterCreationRecipeEvent event = eventCaptor.getValue();
        assertEquals(STACK_CRN, event.getStackCrn());
        assertEquals("", event.getTypeDetails());
    }

    @Test
    public void testAttachmentUsageReport() {
        doNothing().when(usageReporter).cdpRecipeEvent(any());

        underTest.sendAttachedUsageReport(NAME, Optional.of(CRN), Optional.of(VALID_TYPE), STACK_CRN, Optional.of(HOST_GROUP));

        ArgumentCaptor<UsageProto.CDPRecipeEvent> eventCaptor = ArgumentCaptor.forClass(UsageProto.CDPRecipeEvent.class);
        verify(usageReporter).cdpRecipeEvent(eventCaptor.capture());
        UsageProto.CDPRecipeEvent event = eventCaptor.getValue();
        assertEquals(CRN, event.getRecipeCrn());
        assertEquals(NAME, event.getName());
        assertEquals(UsageProto.CDPRecipeType.Value.PRE_TERMINATION, event.getType());
        assertEquals(UsageProto.CDPRecipeStatus.Value.ATTACHED, event.getStatus());
    }

    @Test
    public void testDetachmentUsageReport() {
        doNothing().when(usageReporter).cdpRecipeEvent(any());

        underTest.sendDetachedUsageReport(NAME, Optional.of(CRN), Optional.of(INVALID_TYPE), STACK_CRN, Optional.of(HOST_GROUP));

        ArgumentCaptor<UsageProto.CDPRecipeEvent> eventCaptor = ArgumentCaptor.forClass(UsageProto.CDPRecipeEvent.class);
        verify(usageReporter).cdpRecipeEvent(eventCaptor.capture());
        UsageProto.CDPRecipeEvent event = eventCaptor.getValue();
        assertEquals(CRN, event.getRecipeCrn());
        assertEquals(NAME, event.getName());
        assertEquals(UsageProto.CDPRecipeType.Value.UNKNOWN, event.getType());
        assertEquals(UsageProto.CDPRecipeStatus.Value.DETACHED, event.getStatus());
    }

    @Test
    public void testCreationUsageReport() {
        doNothing().when(usageReporter).cdpRecipeEvent(any());

        underTest.sendCreatedUsageReport(NAME, CRN, VALID_TYPE);

        ArgumentCaptor<UsageProto.CDPRecipeEvent> eventCaptor = ArgumentCaptor.forClass(UsageProto.CDPRecipeEvent.class);
        verify(usageReporter).cdpRecipeEvent(eventCaptor.capture());
        UsageProto.CDPRecipeEvent event = eventCaptor.getValue();
        assertEquals(CRN, event.getRecipeCrn());
        assertEquals(NAME, event.getName());
        assertEquals(UsageProto.CDPRecipeType.Value.PRE_TERMINATION, event.getType());
        assertEquals(UsageProto.CDPRecipeStatus.Value.CREATED, event.getStatus());
    }

    @Test
    public void testDeletionUsageReport() {
        doNothing().when(usageReporter).cdpRecipeEvent(any());

        underTest.sendDeletedUsageReport(NAME, CRN, INVALID_TYPE);

        ArgumentCaptor<UsageProto.CDPRecipeEvent> eventCaptor = ArgumentCaptor.forClass(UsageProto.CDPRecipeEvent.class);
        verify(usageReporter).cdpRecipeEvent(eventCaptor.capture());
        UsageProto.CDPRecipeEvent event = eventCaptor.getValue();
        assertEquals(CRN, event.getRecipeCrn());
        assertEquals(NAME, event.getName());
        assertEquals(UsageProto.CDPRecipeType.Value.UNKNOWN, event.getType());
        assertEquals(UsageProto.CDPRecipeStatus.Value.DELETED, event.getStatus());
    }

    @Test
    public void testClusterCreationUsageReport() {
        doNothing().when(usageReporter).cdpClusterCreationRecipeEvent(any());

        underTest.sendClusterCreationRecipeUsageReport(STACK_CRN, 0, Optional.of("{}"), Optional.of("{}"));

        ArgumentCaptor<UsageProto.CDPClusterCreationRecipeEvent> eventCaptor = ArgumentCaptor.forClass(UsageProto.CDPClusterCreationRecipeEvent.class);
        verify(usageReporter).cdpClusterCreationRecipeEvent(eventCaptor.capture());
        UsageProto.CDPClusterCreationRecipeEvent event = eventCaptor.getValue();
        assertEquals(STACK_CRN, event.getStackCrn());
        assertEquals("{}", event.getTypeDetails());
    }
}
