package com.sequenceiq.freeipa.controller;


import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.freeipa.api.v2.freeipa.model.rebuild.RebuildV2Request;
import com.sequenceiq.freeipa.api.v2.freeipa.model.rebuild.RebuildV2Response;
import com.sequenceiq.freeipa.service.rebuild.RebuildService;
import com.sequenceiq.freeipa.util.CrnService;

@ExtendWith(MockitoExtension.class)
class FreeIpaV2ControllerTest {

    @Mock
    private CrnService crnService;

    @Mock
    private RebuildService rebuildService;

    @InjectMocks
    private FreeIpaV2Controller underTest;

    @Test
    void rebuildv2() throws Exception {
        when(crnService.getCurrentAccountId()).thenReturn("accId");
        RebuildV2Response response = new RebuildV2Response();
        RebuildV2Request request = new RebuildV2Request();
        when(rebuildService.rebuild("accId", request)).thenReturn(response);

        RebuildV2Response result = underTest.rebuildv2(request);

        assertEquals(response, result);
    }
}