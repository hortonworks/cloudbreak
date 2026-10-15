package com.sequenceiq.cloudbreak.cm.polling;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;

import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;

class ClouderaManagerPollingTimeoutProviderTest {

    @Test
    void testGetDefaultTimeoutForRealPlatform() {
        long actual = ClouderaManagerPollingTimeoutProvider.getDefaultTimeout(CloudPlatform.AZURE.name());
        assertEquals(TimeUnit.HOURS.toSeconds(1), actual);
    }

    @Test
    void testGetDefaultTimeoutForMock() {
        long actual = ClouderaManagerPollingTimeoutProvider.getDefaultTimeout(CloudPlatform.MOCK.name());
        assertEquals(TimeUnit.MINUTES.toSeconds(1), actual);
    }

    @Test
    void testGetTemplateInstallationTimeoutForPrewarmedImage() {
        long actual = ClouderaManagerPollingTimeoutProvider.getTemplateInstallationTimeout(CloudPlatform.AZURE.name(), true);
        assertEquals(TimeUnit.HOURS.toSeconds(1), actual);
    }

    @Test
    void testGetTemplateInstallationTimeoutForBaseImage() {
        long actual = ClouderaManagerPollingTimeoutProvider.getTemplateInstallationTimeout(CloudPlatform.AZURE.name(), false);
        assertEquals(TimeUnit.HOURS.toSeconds(3), actual);
    }

    @Test
    void testGetTemplateInstallationTimeoutForAwsBaseImage() {
        long actual = ClouderaManagerPollingTimeoutProvider.getTemplateInstallationTimeout(CloudPlatform.AWS.name(), false);
        assertEquals(TimeUnit.HOURS.toSeconds(3), actual);
    }

    @Test
    void testGetTemplateInstallationTimeoutForMockPrewarmed() {
        long actual = ClouderaManagerPollingTimeoutProvider.getTemplateInstallationTimeout(CloudPlatform.MOCK.name(), true);
        assertEquals(TimeUnit.MINUTES.toSeconds(1), actual);
    }

    @Test
    void testGetTemplateInstallationTimeoutForMockBaseImage() {
        long actual = ClouderaManagerPollingTimeoutProvider.getTemplateInstallationTimeout(CloudPlatform.MOCK.name(), false);
        assertEquals(TimeUnit.MINUTES.toSeconds(1), actual);
    }
}
