package com.sequenceiq.cloudbreak.controller.v4;


import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.api.endpoint.v4.imagecatalog.responses.RuntimeVersionsV4Response;
import com.sequenceiq.cloudbreak.service.image.ImageCatalogService;

@ExtendWith(MockitoExtension.class)
public class ImageCatalogV4ControllerTest {

    private static final Long WORKSPACE_ID = 1L;

    @Mock
    private ImageCatalogService imageCatalogService;

    @InjectMocks
    private ImageCatalogV4Controller victim;

    @Test
    public void testGetRuntimeVersionsFromDefault() throws Exception {
        List<String> expected = List.of("7.2.1", "7.2.2");
        when(imageCatalogService.getRuntimeVersionsFromDefault()).thenReturn(expected);

        RuntimeVersionsV4Response actual =  victim.getRuntimeVersionsFromDefault(WORKSPACE_ID);

        Assertions.assertEquals(expected, actual.getRuntimeVersions());
    }

}