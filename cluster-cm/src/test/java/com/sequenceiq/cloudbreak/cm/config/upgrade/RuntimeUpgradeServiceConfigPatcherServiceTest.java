package com.sequenceiq.cloudbreak.cm.config.upgrade;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import com.cloudera.api.swagger.client.ApiClient;
import com.sequenceiq.cloudbreak.cloud.model.ClouderaManagerProduct;
import com.sequenceiq.cloudbreak.common.exception.CloudbreakServiceException;
import com.sequenceiq.cloudbreak.dto.StackDtoDelegate;

class RuntimeUpgradeServiceConfigPatcherServiceTest {

    private final ApiClient apiClient = mock(ApiClient.class);

    private final StackDtoDelegate stack = mock(StackDtoDelegate.class);

    private final Set<ClouderaManagerProduct> products = Set.of(new ClouderaManagerProduct().withName("CFM").withVersion("4.12.0.20000-231"));

    private final RuntimeUpgradeServiceConfigPatcher neededPatcher = mock(RuntimeUpgradeServiceConfigPatcher.class);

    private final RuntimeUpgradeServiceConfigPatcher notNeededPatcher = mock(RuntimeUpgradeServiceConfigPatcher.class);

    private final RuntimeUpgradeServiceConfigPatcherService underTest = new RuntimeUpgradeServiceConfigPatcherService();

    @Test
    void testOnlyNeededPatchersAreApplied() {
        ReflectionTestUtils.setField(underTest, "patchers", List.of(neededPatcher, notNeededPatcher));
        when(neededPatcher.isPatchNeeded(stack, products)).thenReturn(true);
        when(notNeededPatcher.isPatchNeeded(stack, products)).thenReturn(false);

        underTest.patchServiceConfigs(apiClient, stack, products);

        verify(neededPatcher).patchServiceConfig(apiClient, stack, products);
        verify(notNeededPatcher, never()).patchServiceConfig(apiClient, stack, products);
    }

    @Test
    void testPatcherFailurePropagates() {
        ReflectionTestUtils.setField(underTest, "patchers", List.of(neededPatcher, notNeededPatcher));
        when(neededPatcher.isPatchNeeded(stack, products)).thenReturn(true);
        doThrow(new CloudbreakServiceException("failed")).when(neededPatcher).patchServiceConfig(apiClient, stack, products);

        assertThrows(CloudbreakServiceException.class, () -> underTest.patchServiceConfigs(apiClient, stack, products));

        verify(notNeededPatcher, never()).isPatchNeeded(stack, products);
    }
}
