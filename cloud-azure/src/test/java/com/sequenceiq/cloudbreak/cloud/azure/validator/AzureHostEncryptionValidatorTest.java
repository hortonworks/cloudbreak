package com.sequenceiq.cloudbreak.cloud.azure.validator;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sequenceiq.cloudbreak.cloud.azure.AzureVmCapabilities;

public class AzureHostEncryptionValidatorTest {

    private AzureVmCapabilities azureVmCapabilities = mock(AzureVmCapabilities.class);

    private AzureHostEncryptionValidator underTest = new AzureHostEncryptionValidator();

    private final String vmType = "vmType";

    private Map<String, AzureVmCapabilities> capabilitiesMap;

    @BeforeEach
    public void setUp() {
        capabilitiesMap = new HashMap<>();
    }

    @Test
    public void testIsVmSupportedEncryptionAtHostSupported() {
        capabilitiesMap.put(vmType, azureVmCapabilities);
        when(azureVmCapabilities.isEncryptionAtHostSupported()).thenReturn(true);

        assertTrue(underTest.isVmSupported(vmType, capabilitiesMap));
    }

    @Test
    public void testIsVmSupportedEncryptionAtHostNotSupported() {
        capabilitiesMap.put(vmType, azureVmCapabilities);
        when(azureVmCapabilities.isEncryptionAtHostSupported()).thenReturn(false);

        assertFalse(underTest.isVmSupported(vmType, capabilitiesMap));
    }

    @Test
    public void testIsVmSupportedCapabilitiesNotAvailable() {
        assertFalse(underTest.isVmSupported(vmType, capabilitiesMap));
    }
}
