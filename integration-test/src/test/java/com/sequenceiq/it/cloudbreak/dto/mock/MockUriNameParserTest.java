package com.sequenceiq.it.cloudbreak.dto.mock;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import com.sequenceiq.it.cloudbreak.dto.mock.endpoint.ClouderaManagerEndpoints;
import com.sequenceiq.it.cloudbreak.dto.mock.endpoint.ClouderaManagerEndpoints.CmV31Api.ClustersByClusterName.HostTemplates.ByHostTemplateName.CommandsApplyHostTemplate;
import com.sequenceiq.it.cloudbreak.dto.mock.endpoint.SpiEndpoints;

public class MockUriNameParserTest {

    @Test
    public void testClustersByClusterNameHostTemplatesByHostTemplateNameCommandsApplyHostTemplate() {
        MockUriNameParser underTest = new MockUriNameParser(CommandsApplyHostTemplate.class);
        MockUriParameters parameters = underTest.getParameters();
        assertEquals("/{mockUuid}/api/v31/clusters/{clusterName}/hostTemplates/{hostTemplateName}/commands/applyHostTemplate", parameters.getUri());
    }

    @Test
    public void testGetParametersWhenHasByPart() {
        MockUriNameParser underTest = new MockUriNameParser(ClouderaManagerEndpoints.CmV31Api.ClustersByClusterName.Hosts.class);
        MockUriParameters parameters = underTest.getParameters();
        assertEquals("/{mockUuid}/api/v31/clusters/{clusterName}/hosts", parameters.getUri());
    }

    @Test
    public void testGetParametersWhenRegisterPublicKey() {
        MockUriNameParser underTest = new MockUriNameParser(SpiEndpoints.Spi.RegisterPublicKey.class);
        MockUriParameters parameters = underTest.getParameters();
        assertEquals("/spi/register_public_key", parameters.getUri());
    }

    @Test
    public void testGetParametersWhenSpiWithMock() {
        MockUriNameParser underTest = new MockUriNameParser(SpiEndpoints.SpiWithMockUuid.CloudMetadataStatuses.class);
        MockUriParameters parameters = underTest.getParameters();
        assertEquals("/{mockUuid}/spi/cloud_metadata_statuses", parameters.getUri());
    }
}
