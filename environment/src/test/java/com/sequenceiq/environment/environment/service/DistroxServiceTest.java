package com.sequenceiq.environment.environment.service;

import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.dyngr.core.AttemptResults;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.StackViewV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.StackViewV4Responses;
import com.sequenceiq.distrox.api.v1.distrox.endpoint.DistroXV1Endpoint;

@ExtendWith(MockitoExtension.class)
public class DistroxServiceTest {

    @Mock
    private DistroXV1Endpoint distroXV1Endpoint;

    @Mock
    private PollerCollection pollerCollection;

    @InjectMocks
    private DistroxService underTest;

    @BeforeEach
    public void setup() {
        ReflectionTestUtils.setField(underTest, "attempt", 5);
        ReflectionTestUtils.setField(underTest, "sleeptime", 1);
    }

    @Test
    public void testStopAttachedDistroxWhenNoAttachedDistroX() {
        when(distroXV1Endpoint.list("envName", null)).thenReturn(new StackViewV4Responses(Collections.emptySet()));
        when(pollerCollection.stopDistroXPoller(new ArrayList<>(), new ArrayList<>(), 1L)).thenReturn(new ArrayList<>());
        when(pollerCollection.evaluateResult(anyList())).thenReturn(AttemptResults.finishWith(null));

        underTest.stopAttachedDistrox(1L, "envName");

        verify(distroXV1Endpoint, times(1)).putStopByCrns(anyList());
    }

    @Test
    public void testStopAttachedDistroxWhenDistroxNotStopped() {
        StackViewV4Response stack = new StackViewV4Response();
        stack.setCrn("crn");
        stack.setStatus(Status.AVAILABLE);
        ArrayList<String> pollingCrn = new ArrayList<>();
        pollingCrn.add("crn");
        when(distroXV1Endpoint.list("envName", null)).thenReturn(new StackViewV4Responses(Set.of(stack)));
        when(pollerCollection.stopDistroXPoller(pollingCrn, new ArrayList<>(), 1L)).thenReturn(List.of(AttemptResults.finishWith(null)));
        when(pollerCollection.evaluateResult(anyList())).thenReturn(AttemptResults.finishWith(null));

        underTest.stopAttachedDistrox(1L, "envName");

        verify(distroXV1Endpoint, times(1)).putStopByCrns(anyList());
    }

    @Test
    public void testStartAttachedDistroxWhenNoAttachedDistroX() {
        when(distroXV1Endpoint.list("envName", null)).thenReturn(new StackViewV4Responses(Collections.emptySet()));
        when(pollerCollection.startDistroXPoller(new ArrayList<>(), new ArrayList<>(), 1L)).thenReturn(new ArrayList<>());
        when(pollerCollection.evaluateResult(anyList())).thenReturn(AttemptResults.finishWith(null));

        underTest.startAttachedDistrox(1L, "envName");

        verify(distroXV1Endpoint, times(1)).putStartByCrns(anyList());
    }

    @Test
    public void testStartAttachedDistroxWhenDistroxNotAvailable() {
        StackViewV4Response stack = new StackViewV4Response();
        stack.setCrn("crn");
        stack.setName("name");
        stack.setStatus(Status.STOPPED);
        when(distroXV1Endpoint.list("envName", null)).thenReturn(new StackViewV4Responses(Set.of(stack)));
        when(pollerCollection.evaluateResult(anyList())).thenReturn(AttemptResults.finishWith(null));

        underTest.startAttachedDistrox(1L, "envName");

        verify(distroXV1Endpoint, times(1)).putStartByCrns(anyList());
    }
}
