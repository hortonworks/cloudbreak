package com.sequenceiq.cloudbreak.service.sharedservice;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.StackType;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.StackV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.cluster.sharedservice.SharedServiceV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.StackV4Response;
import com.sequenceiq.cloudbreak.common.dal.ResourceBasicView;
import com.sequenceiq.cloudbreak.common.exception.BadRequestException;
import com.sequenceiq.cloudbreak.common.exception.NotFoundException;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.service.stack.StackDtoService;
import com.sequenceiq.cloudbreak.service.stack.StackIdViewImpl;
import com.sequenceiq.cloudbreak.service.stack.StackService;
import com.sequenceiq.cloudbreak.structuredevent.CloudbreakRestRequestThreadLocalService;
import com.sequenceiq.cloudbreak.template.views.SharedServiceConfigsView;
import com.sequenceiq.cloudbreak.view.StackView;
import com.sequenceiq.cloudbreak.workspace.model.Workspace;

@ExtendWith({MockitoExtension.class})
public class DatalakeServiceTest {

    @Mock
    private StackService stackService;

    @Mock
    private CloudbreakRestRequestThreadLocalService restRequestThreadLocalService;

    @Mock
    private StackDtoService stackDtoService;

    @InjectMocks
    private DatalakeService underTest;

    @BeforeEach
    public void setup() {
        Stack resultStack = new Stack();
        resultStack.setName("teststack");
        lenient().when(stackService.getByCrn(anyString())).thenReturn(resultStack);
        lenient().when(restRequestThreadLocalService.getRequestedWorkspaceId()).thenReturn(1L);
    }

    @Test
    public void testPrepareDatalakeRequestWhenDatalakeCrnIsNotNull() {
        Stack source = new Stack();
        source.setDatalakeCrn("crn");
        StackV4Request x = new StackV4Request();
        lenient().when(stackService.getResourceBasicViewByResourceCrn(anyString())).thenReturn(Optional.empty());
        underTest.prepareDatalakeRequest(source, x);
        verify(stackService, times(1)).getResourceBasicViewByResourceCrn("crn");
    }

    @Test
    public void testPrepareDatalakeRequestWhenDatalakeCrnIsNull() {
        Stack source = new Stack();
        source.setDatalakeCrn(null);
        StackV4Request x = new StackV4Request();
        underTest.prepareDatalakeRequest(source, x);
        verify(stackService, never()).getByCrn("crn");
    }

    @Test
    public void testAddSharedServiceResponseWhenDatalakeCrnIsNotNull() {
        ResourceBasicView resourceBasicView = mock(ResourceBasicView.class);
        when(resourceBasicView.getId()).thenReturn(1L);
        when(resourceBasicView.getName()).thenReturn("teststack");
        lenient().when(stackService.getResourceBasicViewByResourceCrn(anyString())).thenReturn(Optional.of(resourceBasicView));
        StackV4Response x = new StackV4Response();
        underTest.addSharedServiceResponse("crn", x);
        verify(stackService, times(1)).getResourceBasicViewByResourceCrn("crn");
        assertEquals(1L, x.getSharedService().getSharedClusterId());
        assertEquals("teststack", x.getSharedService().getSharedClusterName());
    }

    @Test
    public void testAddSharedServiceResponseWhenDatalakeCrnIsNotNullAndDatalakeIsMissing() {
        when(stackService.getResourceBasicViewByResourceCrn(anyString())).thenReturn(Optional.empty());
        StackV4Response x = new StackV4Response();
        underTest.addSharedServiceResponse("crn", x);
        verify(stackService, times(1)).getResourceBasicViewByResourceCrn("crn");
        assertNull(x.getSharedService().getSharedClusterId());
        assertNull(x.getSharedService().getSharedClusterName());
    }

    @Test
    public void testAddSharedServiceResponseWhenDatalakeCrnIsNull() {
        StackV4Response x = new StackV4Response();
        underTest.addSharedServiceResponse("crn", x);
        verify(stackService, never()).getByCrnOrElseNull("crn");
    }

    @Test
    public void testGetDatalakeStackByDatahubStackWhereDatalakeCrnIsNull() {
        Stack stack = new Stack();
        stack.setDatalakeCrn(null);
        underTest.getDatalakeStackByDatahubStack(stack);
        verify(stackService, never()).getByCrn("crn");
    }

    @Test
    public void testGetDatalakeStackByDatahubStackWhereDatalakeCrnIsNotNull() {
        Stack resultStack = new Stack();
        resultStack.setName("teststack");
        lenient().when(stackService.getByCrnOrElseNull(anyString())).thenReturn(resultStack);
        Stack stack = new Stack();
        stack.setDatalakeCrn("crn");
        Optional<Stack> datalake = underTest.getDatalakeStackByDatahubStack(stack);
        verify(stackService, times(1)).getByCrnOrElseNull("crn");
        assert datalake.isPresent();
    }

    @Test
    public void testGetDatalakeStackByDatahubStackWhereDatalakeCrnIsNotNullAndDatalakeIsMissing() {
        Stack stack = new Stack();
        stack.setDatalakeCrn("crn");
        Optional<Stack> datalake = underTest.getDatalakeStackByDatahubStack(stack);
        verify(stackService, times(1)).getByCrnOrElseNull("crn");
        assert datalake.isEmpty();
    }

    @Test
    public void testGetDatalakeStackByStackEnvironmentCrnWhenParamStackIsNotDatahub() {
        Stack stack = new Stack();
        stack.setType(StackType.DATALAKE);
        Optional<Stack> res = underTest.getDatalakeStackByStackEnvironmentCrn(stack);
        Assertions.assertTrue(res.isEmpty());
    }

    @Test
    public void testGetDatalakeStackByStackEnvironmentCrnResultsAreEmpty() {
        Stack stack = new Stack();
        stack.setType(StackType.WORKLOAD);
        Optional<Stack> res = underTest.getDatalakeStackByStackEnvironmentCrn(stack);
        Assertions.assertTrue(res.isEmpty());
    }

    @Test
    public void testGetDatalakeStackByStackEnvironmentCrnWithResult() {
        Stack stack = new Stack();
        stack.setType(StackType.WORKLOAD);
        when(stackService.getByEnvironmentCrnAndStackType(any(), any())).thenReturn(List.of(new StackIdViewImpl(1L, "no", "no")));

        when(stackService.getByIdWithListsInTransaction(1L)).thenReturn(new Stack());

        Optional<Stack> res = underTest.getDatalakeStackByStackEnvironmentCrn(stack);
        Assertions.assertTrue(!res.isEmpty());
    }

    @Test
    public void testCreateSharedServiceConfigsViewByCrn() {

        SharedServiceConfigsView res = underTest.createSharedServiceConfigsView("pwd", StackType.WORKLOAD, "crn");

        verify(stackService, times(1)).getByCrnOrElseNull("crn");
        Assertions.assertFalse(res.isDatalakeCluster());

    }

    @Test
    public void testCreateSharedServiceConfigsViewFromBlueprintUtilsWhenDatalake() {

        SharedServiceConfigsView res = underTest.createSharedServiceConfigsView("pwd", StackType.DATALAKE, null);

        verify(stackService, times(0)).getByCrnOrElseNull("crn");
        Assertions.assertTrue(res.isDatalakeCluster());
    }

    @Test
    public void testCreateSharedServiceConfigsViewWhenDatahubButDatalakeCrnIsMissing() {
        SharedServiceConfigsView res = underTest.createSharedServiceConfigsView("pwd", StackType.WORKLOAD, null);

        verify(stackService, times(0)).getByCrnOrElseNull("crn");
        Assertions.assertFalse(res.isDatalakeCluster());
    }

    @Test
    public void testGetDatalakeCrnHasResult() {
        StackV4Request source = new StackV4Request();
        Workspace workspace = new Workspace();
        source.setSharedService(new SharedServiceV4Request());
        source.getSharedService().setDatalakeName("name");
        Stack output = new Stack();
        output.setResourceCrn("resultCrn");
        when(stackService.findStackByNameOrCrnAndWorkspaceId(any(), any())).thenReturn(Optional.of(output));
        String res = underTest.getDatalakeCrn(source, workspace);
        Assertions.assertTrue("resultCrn".equals(res));
    }

    @Test
    public void testGetResourceCrnByResourceName() {
        when(stackDtoService.findNotTerminatedByNameAndAccountId(any(), any())).thenReturn(Optional.empty());
        assertThrows(NotFoundException.class, () -> underTest.getResourceCrnByResourceName("name"), "name stack not found");

        StackView stackView = stackView(StackType.WORKLOAD);
        when(stackDtoService.findNotTerminatedByNameAndAccountId(any(), any())).thenReturn(Optional.of(stackView));
        assertThrows(BadRequestException.class, () -> underTest.getResourceCrnByResourceName("name"), "name stack is not a Data Lake");

        StackView datalakeView = stackView(StackType.DATALAKE);
        when(datalakeView.getResourceCrn()).thenReturn("crn");
        when(stackDtoService.findNotTerminatedByNameAndAccountId(any(), any())).thenReturn(Optional.of(datalakeView));
        assertEquals("crn", underTest.getResourceCrnByResourceName("name"));
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
        List<String> crnList = underTest.getResourceCrnListByResourceNameList(List.of("name1", "name2"));
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
