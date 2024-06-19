package com.sequenceiq.cloudbreak.service.sharedservice;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.StackType;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.StackV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.StackV4Response;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.sdx.common.PlatformAwareSdxConnector;
import com.sequenceiq.cloudbreak.sdx.common.model.SdxBasicView;
import com.sequenceiq.cloudbreak.service.stack.StackService;
import com.sequenceiq.cloudbreak.template.views.SharedServiceConfigsView;

@ExtendWith({MockitoExtension.class})
public class DatalakeServiceTest {

    @Mock
    private StackService stackService;

    @Mock
    private PlatformAwareSdxConnector platformAwareSdxConnector;

    @InjectMocks
    private DatalakeService underTest;

    @Test
    public void testPrepareDatalakeRequestWhenDatalakeIsNotNull() {
        Stack source = new Stack();
        source.setEnvironmentCrn("envCrn");
        when(platformAwareSdxConnector.getSdxBasicViewByEnvironmentCrn(anyString())).thenReturn(Optional.of(
                new SdxBasicView("name", "crn", null, true, 1L, null)));
        StackV4Request stackRequest = new StackV4Request();
        underTest.prepareDatalakeRequest(source, stackRequest);
        verify(platformAwareSdxConnector, times(1)).getSdxBasicViewByEnvironmentCrn(eq("envCrn"));
        assertEquals("name", stackRequest.getSharedService().getDatalakeName());
    }

    @Test
    public void testPrepareDatalakeRequestWhenDatalakeIsMissing() {
        Stack source = new Stack();
        source.setEnvironmentCrn("envCrn");
        when(platformAwareSdxConnector.getSdxBasicViewByEnvironmentCrn(anyString())).thenReturn(Optional.empty());
        StackV4Request stackRequest = new StackV4Request();
        underTest.prepareDatalakeRequest(source, stackRequest);
        assertNull(stackRequest.getSharedService().getDatalakeName());
    }

    @Test
    public void testAddSharedServiceResponse() {
        when(platformAwareSdxConnector.getSdxBasicViewByEnvironmentCrn(anyString())).thenReturn(Optional.of(
                new SdxBasicView("name", "crn", null, true, 1L, null)));
        StackV4Response x = new StackV4Response();
        x.setEnvironmentCrn("envCrn");
        underTest.addSharedServiceResponse(x);
        verify(platformAwareSdxConnector, times(1)).getSdxBasicViewByEnvironmentCrn(eq("envCrn"));
        assertEquals("name", x.getSharedService().getSharedClusterName());
        assertEquals("name", x.getSharedService().getSdxName());
        assertEquals("crn", x.getSharedService().getSdxCrn());
    }

    @Test
    public void testAddSharedServiceResponseWhenDatalakeIsMissing() {
        when(platformAwareSdxConnector.getSdxBasicViewByEnvironmentCrn(anyString())).thenReturn(Optional.empty());
        StackV4Response x = new StackV4Response();
        x.setEnvironmentCrn("envCrn");
        underTest.addSharedServiceResponse(x);
        verify(platformAwareSdxConnector, times(1)).getSdxBasicViewByEnvironmentCrn(eq("envCrn"));
        assertNull(x.getSharedService().getSharedClusterName());
        assertNull(x.getSharedService().getSdxCrn());
        assertNull(x.getSharedService().getSdxName());
    }

    @Test
    public void testAddSharedServiceResponseWhenEnvCrnIsNull() {
        StackV4Response x = new StackV4Response();
        underTest.addSharedServiceResponse(x);
        verify(platformAwareSdxConnector, never()).getSdxBasicViewByEnvironmentCrn(anyString());
    }

    @Test
    public void testCreateSharedServiceConfigsViewByCrn() {
        SharedServiceConfigsView res = underTest.createSharedServiceConfigsView("pwd", StackType.WORKLOAD, "envcrn");

        verify(platformAwareSdxConnector, times(1)).getSdxAccessViewByEnvironmentCrn(anyString());
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
}
