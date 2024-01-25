package com.sequenceiq.cloudbreak.cloud.azure.validator;


import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

class AzureHostEncryptionValidatorTest {

    private AzureHostEncryptionValidator azureHostEncryptionValidator = new AzureHostEncryptionValidator();

    @Test
    void testIsVmSupportedWhenSupported() {
        Map<String, Boolean> hostEncryptionSupport = new HashMap<>();
        hostEncryptionSupport.put("VirtualMachineType1", true);

        boolean result = azureHostEncryptionValidator.isVmSupported("VirtualMachineType1", hostEncryptionSupport);

        assertTrue(result, "Virtual machine should be supported");
    }

    @Test
    void testIsVmSupportedWhenNotSupported() {
        Map<String, Boolean> hostEncryptionSupport = new HashMap<>();
        hostEncryptionSupport.put("VirtualMachineType1", false);

        boolean result = azureHostEncryptionValidator.isVmSupported("VirtualMachineType1", hostEncryptionSupport);

        assertFalse(result, "Virtual machine should not be supported");
    }

    @Test
    void testIsVmSupportedWhenMapIsNull() {
        boolean result = azureHostEncryptionValidator.isVmSupported("VirtualMachineType1", null);

        assertFalse(result, "Virtual machine should not be supported when map is null");
    }

    @Test
    void testIsVmSupportedWhenVmTypeNotInMap() {
        Map<String, Boolean> hostEncryptionSupport = new HashMap<>();
        hostEncryptionSupport.put("AnotherVirtualMachineType", true);

        boolean result = azureHostEncryptionValidator.isVmSupported("VirtualMachineType1", hostEncryptionSupport);

        assertFalse(result, "Virtual machine should not be supported when not in the map");
    }

    @Test
    void testIsVmSupportedWhenVmTypeIsNull() {
        Map<String, Boolean> hostEncryptionSupport = new HashMap<>();

        boolean result = azureHostEncryptionValidator.isVmSupported(null, hostEncryptionSupport);

        assertFalse(result, "Virtual machine should not be supported when vmType is null");
    }
}