package com.sequenceiq.distrox.v1.distrox.converter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.cluster.sharedservice.SharedServiceV4Request;
import com.sequenceiq.cloudbreak.sdx.common.model.SdxBasicView;

@ExtendWith(MockitoExtension.class)
public class SdxConverterTest {

    @InjectMocks
    private SdxConverter underTest;

    @Test
    public void testGetSharedServiceWhenSdxNullAndEnvironmentHasNotSdx() {
        SharedServiceV4Request sdxRequest = underTest.getSharedService(null);

        assertNull(sdxRequest);
    }

    @Test
    public void testGetSharedService() {
        SdxBasicView sdx = new SdxBasicView("name", null, "runtime", false, null, null, null);

        SharedServiceV4Request sdxRequest = underTest.getSharedService(sdx);

        assertEquals("name", sdxRequest.getDatalakeName());
        assertEquals("runtime", sdxRequest.getRuntimeVersion());
    }
}
