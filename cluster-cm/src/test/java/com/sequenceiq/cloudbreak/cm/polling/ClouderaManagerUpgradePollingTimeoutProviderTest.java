package com.sequenceiq.cloudbreak.cm.polling;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.cloudbreak.dto.StackDtoDelegate;
import com.sequenceiq.cloudbreak.view.InstanceMetadataView;

@ExtendWith(MockitoExtension.class)
class ClouderaManagerUpgradePollingTimeoutProviderTest {

    private final ClouderaManagerUpgradePollingTimeoutProvider underTest = new ClouderaManagerUpgradePollingTimeoutProvider();

    @Test
    public void testGetParcelDownloadTimeout() {
        long actual = underTest.getParcelDownloadTimeout(CloudPlatform.AZURE.name());
        assertEquals(7200, actual);
    }

    @Test
    public void testGetParcelDownloadTimeoutWhenTheProviderIsMock() {
        long actual = underTest.getParcelDownloadTimeout(CloudPlatform.MOCK.name());
        assertEquals(60, actual);
    }

    @Test
    public void testGetParcelDistributeTimeout() {
        long actual = underTest.getParcelDistributeTimeout(CloudPlatform.AZURE.name());
        assertEquals(7200, actual);
    }

    @Test
    public void testGetParcelDistributeTimeoutWhenTheProviderIsMock() {
        long actual = underTest.getParcelDistributeTimeout(CloudPlatform.MOCK.name());
        assertEquals(60, actual);
    }

    @Test
    public void testGetCdhUpgradeTimeoutWhenTheRollingUpgradeEnabledAndTheCloudProviderIsMock() {
        long actual = underTest.getCdhUpgradeTimeout(createStack(40, CloudPlatform.MOCK), true);
        assertEquals(60, actual);
    }

    @Test
    public void testGetCdhUpgradeTimeoutWhenTheRollingUpgradeEnabledWith40Nodes() {
        long actual = underTest.getCdhUpgradeTimeout(createStack(40, CloudPlatform.AZURE), true);
        assertEquals(13800, actual);
    }

    @Test
    public void testGetCdhUpgradeTimeoutWhenTheRollingUpgradeEnabledWith10Nodes() {
        long actual = underTest.getCdhUpgradeTimeout(createStack(10, CloudPlatform.AZURE), true);
        assertEquals(4800, actual);
    }

    @Test
    public void testGetCdhUpgradeTimeoutWhenTheRollingUpgradeEnabledWith0Nodes() {
        long actual = underTest.getCdhUpgradeTimeout(createStack(0, CloudPlatform.AZURE), true);
        assertEquals(1800, actual);
    }

    @Test
    public void testGetCdhUpgradeTimeoutWhenTheRollingUpgradeDisabled() {
        long actual = underTest.getCdhUpgradeTimeout(createStack(40, CloudPlatform.AZURE), false);
        assertEquals(3600, actual);
    }

    private StackDtoDelegate createStack(int numberOfInstances, CloudPlatform cloudPlatform) {
        StackDtoDelegate stack = mock(StackDtoDelegate.class);
        List<InstanceMetadataView> instances = mock(ArrayList.class);
        when(stack.getCloudPlatform()).thenReturn(cloudPlatform.name());
        lenient().when(stack.getAllAvailableInstances()).thenReturn(instances);
        lenient().when(instances.size()).thenReturn(numberOfInstances);
        return stack;
    }

}