package com.sequenceiq.cloudbreak.cm.config;

import static com.sequenceiq.cloudbreak.cmtemplate.configproviders.flink.FlinkConfigProviderUtils.RELEASE_NAME_CONF_NAME;
import static com.sequenceiq.cloudbreak.cmtemplate.configproviders.flink.FlinkConfigProviderUtils.RELEASE_NAME_CONF_VALUE;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.cloudera.api.swagger.client.ApiClient;
import com.sequenceiq.cloudbreak.cloud.model.ClouderaManagerProduct;
import com.sequenceiq.cloudbreak.cluster.service.ClouderaManagerProductsProvider;
import com.sequenceiq.cloudbreak.cm.ClouderaManagerConfigService;
import com.sequenceiq.cloudbreak.cmtemplate.CmTemplateService;
import com.sequenceiq.cloudbreak.common.exception.CloudbreakServiceException;
import com.sequenceiq.cloudbreak.domain.Blueprint;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.service.CloudbreakException;

@ExtendWith(MockitoExtension.class)
class ClouderaManagerFlinkConfigurationServiceTest {

    private static final String FLINK_SERVICE_TYPE = "SQL_STREAM_BUILDER";

    @InjectMocks
    private ClouderaManagerFlinkConfigurationService underTest;

    @Mock
    private CmTemplateService cmTemplateService;

    @Mock
    private ClouderaManagerConfigService clouderaManagerConfigService;

    @Mock
    private ClouderaManagerProductsProvider clouderaManagerProductsProvider;

    @Mock
    private ApiClient apiClient;

    @Mock
    private Set<ClouderaManagerProduct> products;

    @Test
    void testAddServiceConfigurationIfNecessaryShouldSetTheConfiguration() throws CloudbreakException {
        Stack stack = createStack();

        when(clouderaManagerProductsProvider.getCdhProduct(products)).thenReturn(Optional.of(createCdhProduct("7.3.1")));
        when(cmTemplateService.isServiceTypePresent(FLINK_SERVICE_TYPE, stack.getBlueprintJsonText())).thenReturn(true);

        underTest.addServiceConfigurationIfNecessary(apiClient, stack, products);

        verify(clouderaManagerConfigService).modifyServiceConfig(apiClient, stack.getName(), FLINK_SERVICE_TYPE,
                Map.of(RELEASE_NAME_CONF_NAME, RELEASE_NAME_CONF_VALUE));
    }

    @Test
    void testAddServiceConfigurationIfNecessaryShouldNotSetTheConfigurationWhenTheRuntimeVersionIsLower() {
        Stack stack = createStack();

        when(clouderaManagerProductsProvider.getCdhProduct(products)).thenReturn(Optional.of(createCdhProduct("7.3.0")));

        underTest.addServiceConfigurationIfNecessary(apiClient, stack, products);

        verifyNoInteractions(cmTemplateService, clouderaManagerConfigService);
    }

    @Test
    void testAddServiceConfigurationIfNecessaryShouldNotSetTheConfigurationWhenTheRuntimeVersionIsHigher() {
        Stack stack = createStack();

        when(clouderaManagerProductsProvider.getCdhProduct(products)).thenReturn(Optional.of(createCdhProduct("7.3.2")));

        underTest.addServiceConfigurationIfNecessary(apiClient, stack, products);

        verifyNoInteractions(cmTemplateService, clouderaManagerConfigService);
    }

    @Test
    void testAddServiceConfigurationIfNecessaryShouldNotSetTheConfigurationWhenTheFlinkServiceIsNotPresent() {
        Stack stack = createStack();

        when(clouderaManagerProductsProvider.getCdhProduct(products)).thenReturn(Optional.of(createCdhProduct("7.3.1")));
        when(cmTemplateService.isServiceTypePresent(FLINK_SERVICE_TYPE, stack.getBlueprintJsonText())).thenReturn(false);

        underTest.addServiceConfigurationIfNecessary(apiClient, stack, products);

        verifyNoInteractions(clouderaManagerConfigService);
    }

    @Test
    void testAddServiceConfigurationIfNecessaryShouldThrowExceptionWhenTryingToSetTheConfig() throws CloudbreakException {
        Stack stack = createStack();

        when(clouderaManagerProductsProvider.getCdhProduct(products)).thenReturn(Optional.of(createCdhProduct("7.3.1")));
        when(cmTemplateService.isServiceTypePresent(FLINK_SERVICE_TYPE, stack.getBlueprintJsonText())).thenReturn(true);
        doThrow(CloudbreakException.class).when(clouderaManagerConfigService).modifyServiceConfig(apiClient, stack.getName(), FLINK_SERVICE_TYPE,
                Map.of(RELEASE_NAME_CONF_NAME, RELEASE_NAME_CONF_VALUE));

        assertThrows(CloudbreakServiceException.class, () -> underTest.addServiceConfigurationIfNecessary(apiClient, stack, products));

        verify(clouderaManagerConfigService).modifyServiceConfig(apiClient, stack.getName(), FLINK_SERVICE_TYPE,
                Map.of(RELEASE_NAME_CONF_NAME, RELEASE_NAME_CONF_VALUE));
    }

    private Stack createStack() {
        Blueprint blueprint = new Blueprint();
        blueprint.setBlueprintText("{}");
        Cluster cluster = new Cluster();
        cluster.setBlueprint(blueprint);
        Stack stack = new Stack();
        stack.setName("stack-name");
        stack.setCluster(cluster);
        return stack;
    }

    private ClouderaManagerProduct createCdhProduct(String version) {
        return new ClouderaManagerProduct().withVersion(version);
    }

}