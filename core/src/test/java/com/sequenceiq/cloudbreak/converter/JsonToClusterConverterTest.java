package com.sequenceiq.cloudbreak.converter;

import static org.mockito.BDDMockito.given;
import static org.mockito.Matchers.any;

import java.util.Arrays;

import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.core.convert.ConversionService;

import com.sequenceiq.cloudbreak.controller.json.AmbariStackDetailsJson;
import com.sequenceiq.cloudbreak.controller.json.ClusterRequest;
import com.sequenceiq.cloudbreak.domain.AmbariStackDetails;
import com.sequenceiq.cloudbreak.domain.Cluster;

public class JsonToClusterConverterTest extends AbstractJsonConverterTest<ClusterRequest> {

    @InjectMocks
    private JsonToClusterConverter underTest;

    @Mock
    private ConversionService conversionService;

    @Before
    public void setUp() {
        underTest = new JsonToClusterConverter();
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void testConvert() {
        // GIVEN
        // WHEN
        Cluster result = underTest.convert(getRequest("stack/cluster.json"));
        // THEN
        assertAllFieldsNotNull(result, Arrays.asList("stack", "blueprint", "creationStarted",
                "creationFinished", "upSince", "statusReason", "ambariIp", "ambariStackDetails", "fileSystem"));
    }

    @Test
    public void testConvertWithAmbariStackDetails() {
        // GIVEN
        given(conversionService.convert(any(AmbariStackDetailsJson.class), any(Class.class))).willReturn(new AmbariStackDetails());
        // WHEN
        Cluster result = underTest.convert(getRequest("stack/cluster-with-ambari-stack-details.json"));
        // THEN
        assertAllFieldsNotNull(result, Arrays.asList("stack", "blueprint", "creationStarted",
                "creationFinished", "upSince", "statusReason", "ambariIp", "fileSystem"));
    }

    @Override
    public Class<ClusterRequest> getRequestClass() {
        return ClusterRequest.class;
    }
}
