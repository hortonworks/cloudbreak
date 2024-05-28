package com.sequenceiq.cloudbreak.service.sharedservice;

import static com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider.doAs;
import static com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider.doAsAndThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.StackType;
import com.sequenceiq.cloudbreak.common.exception.BadRequestException;
import com.sequenceiq.cloudbreak.common.exception.NotFoundException;
import com.sequenceiq.cloudbreak.service.stack.StackDtoService;
import com.sequenceiq.cloudbreak.view.StackView;

@ExtendWith({MockitoExtension.class})
public class PaasDatalakeAuthorizationServiceTest {

    private static final String USER_CRN = "crn:cdp:iam:us-west-1:hortonworks:user:test@test.com";

    @Mock
    private StackDtoService stackDtoService;

    @InjectMocks
    private PaasDatalakeAuthorizationService underTest;

    @Test
    public void testGetResourceCrnByResourceName() {
        when(stackDtoService.findNotTerminatedByNameAndAccountId(any(), any())).thenReturn(Optional.empty());
        assertThrows(NotFoundException.class, () -> doAsAndThrow(USER_CRN, () ->
                underTest.getResourceCrnByResourceName("name")), "name stack not found");

        StackView stackView = stackView(StackType.WORKLOAD);
        when(stackDtoService.findNotTerminatedByNameAndAccountId(any(), any())).thenReturn(Optional.of(stackView));
        assertThrows(BadRequestException.class, () -> doAsAndThrow(USER_CRN, () ->
                underTest.getResourceCrnByResourceName("name")), "name stack is not a Data Lake");

        StackView datalakeView = stackView(StackType.DATALAKE);
        when(datalakeView.getResourceCrn()).thenReturn("crn");
        when(stackDtoService.findNotTerminatedByNameAndAccountId(any(), any())).thenReturn(Optional.of(datalakeView));
        assertEquals("crn", doAs(USER_CRN, () -> underTest.getResourceCrnByResourceName("name")));
    }

    @Test
    public void testGetEnviromentCrnByResourceCrn() {
        when(stackDtoService.findNotTerminatedByCrn(any())).thenReturn(Optional.empty());
        assertEquals(Optional.empty(), underTest.getEnvironmentCrnByResourceCrn("crn"));

        StackView workloadView = stackView(StackType.WORKLOAD);
        when(stackDtoService.findNotTerminatedByCrn(any())).thenReturn(Optional.of(workloadView));
        assertThrows(BadRequestException.class, () -> underTest.getEnvironmentCrnByResourceCrn("crn"), "Stack with CRN crn is not a Data Lake");

        StackView datalakeView = stackView(StackType.DATALAKE);
        when(datalakeView.getEnvironmentCrn()).thenReturn("envCrn");
        when(stackDtoService.findNotTerminatedByCrn(any())).thenReturn(Optional.of(datalakeView));
        assertEquals(Optional.of("envCrn"), underTest.getEnvironmentCrnByResourceCrn("crn"));
    }

    @Test
    public void testGetResourceCrnListByResourceNameList() {
        StackView workloadView = stackView(StackType.WORKLOAD);
        StackView datalakeView = stackView(StackType.DATALAKE);
        when(datalakeView.getResourceCrn()).thenReturn("crn2");
        when(stackDtoService.findNotTerminatedByNamesAndAccountId(any(), any())).thenReturn(List.of(workloadView, datalakeView));
        List<String> crnList = doAs(USER_CRN, () -> underTest.getResourceCrnListByResourceNameList(List.of("name1", "name2")));
        assertFalse(crnList.isEmpty());
        assertEquals(1, crnList.size());
        assertTrue(crnList.contains("crn2"));
    }

    @Test
    public void testGetEnvironmentCrnListByResourceCrnList() {
        StackView workloadView = stackView(StackType.WORKLOAD);
        StackView datalakeView = stackView(StackType.DATALAKE);
        when(datalakeView.getResourceCrn()).thenReturn("crn2");
        when(datalakeView.getEnvironmentCrn()).thenReturn("envCrn2");
        when(stackDtoService.findNotTerminatedByCrns(any())).thenReturn(List.of(workloadView, datalakeView));
        Map<String, Optional<String>> crnMap = underTest.getEnvironmentCrnsByResourceCrns(List.of("crn1", "crn2"));
        assertFalse(crnMap.isEmpty());
        assertEquals(1, crnMap.entrySet().size());
        assertTrue(crnMap.keySet().contains("crn2"));
        assertTrue(crnMap.values().contains(Optional.of("envCrn2")));
    }

    private StackView stackView(StackType type) {
        StackView view = mock(StackView.class);
        when(view.getType()).thenReturn(type);
        return view;
    }
}
