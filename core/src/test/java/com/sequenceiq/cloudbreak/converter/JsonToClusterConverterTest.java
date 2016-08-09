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

import com.sequenceiq.cloudbreak.api.model.ClusterRequest;
import com.sequenceiq.cloudbreak.api.model.FileSystemRequest;
import com.sequenceiq.cloudbreak.domain.AmbariStackDetails;
import com.sequenceiq.cloudbreak.domain.Cluster;
import com.sequenceiq.cloudbreak.domain.FileSystem;

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
        assertAllFieldsNotNull(result, Arrays.asList("stack", "blueprint", "creationStarted", "creationFinished", "upSince", "statusReason", "ambariIp",
                "ambariStackDetails", "fileSystem", "sssdConfig", "certDir", "rdsConfig", "attributes"));
    }

    @Test
    public void testConvertWithAmbariStackDetails() {
        // GIVEN
        ClusterRequest clusterRequest = getRequest("stack/cluster-with-ambari-stack-details.json");
        given(conversionService.convert(clusterRequest.getAmbariStackDetails(), AmbariStackDetails.class)).willReturn(new AmbariStackDetails());
        // WHEN
        Cluster result = underTest.convert(clusterRequest);
        // THEN
        assertAllFieldsNotNull(result, Arrays.asList("stack", "blueprint", "creationStarted", "creationFinished", "upSince", "statusReason", "ambariIp",
                "fileSystem", "sssdConfig", "certDir", "rdsConfig", "attributes"));
    }

    @Test
    public void testConvertWithFileSystemDetails() {
        // GIVEN
        given(conversionService.convert(any(FileSystemRequest.class), any(Class.class))).willReturn(new FileSystem());
        // WHEN
        Cluster result = underTest.convert(getRequest("stack/cluster-with-file-system.json"));
        // THEN
        assertAllFieldsNotNull(result, Arrays.asList("stack", "blueprint", "creationStarted", "creationFinished", "upSince", "statusReason", "ambariIp",
                "ambariStackDetails", "sssdConfig", "certDir", "rdsConfig", "attributes"));
    }

    @Override
    public Class<ClusterRequest> getRequestClass() {
        return ClusterRequest.class;
    }
}
