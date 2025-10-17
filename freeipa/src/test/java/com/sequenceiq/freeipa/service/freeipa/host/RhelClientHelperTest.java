package com.sequenceiq.freeipa.service.freeipa.host;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import com.sequenceiq.cloudbreak.cloud.model.Image;
import com.sequenceiq.cloudbreak.common.json.Json;
import com.sequenceiq.common.model.OsType;
import com.sequenceiq.freeipa.client.FreeIpaClient;
import com.sequenceiq.freeipa.entity.InstanceMetaData;
import com.sequenceiq.freeipa.entity.Stack;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class RhelClientHelperTest {

    @InjectMocks
    private RhelClientHelper rhelClientHelper;

    @Mock
    private Stack stack;

    @Mock
    private InstanceMetaData instanceMetaDataRhel;

    @Mock
    private InstanceMetaData instanceMetaDataUbuntu;

    @Mock
    private Json imageRhelJson;

    @Mock
    private Json imageUbuntuJson;

    @Mock
    private Image imageRhel;

    @Mock
    private Image imageUbuntu;

    @Mock
    private FreeIpaClient freeIpaClient;

    @BeforeEach
    void setUp() {
        when(imageRhelJson.getValue()).thenReturn("not empty");
        when(imageUbuntuJson.getValue()).thenReturn("not empty");

        when(imageRhelJson.getUnchecked(any())).thenReturn(imageRhel);
        when(imageUbuntuJson.getUnchecked(any())).thenReturn(imageUbuntu);

        when(imageRhel.getOsType()).thenReturn("redhat8");
        when(imageUbuntu.getOsType()).thenReturn("ubuntu");

        when(instanceMetaDataRhel.getImage()).thenReturn(imageRhelJson);
        when(instanceMetaDataUbuntu.getImage()).thenReturn(imageUbuntuJson);

        when(instanceMetaDataRhel.getDiscoveryFQDN()).thenReturn("rhel-instance.example.com");
        when(instanceMetaDataUbuntu.getDiscoveryFQDN()).thenReturn("ubuntu-instance.example.com");
    }

    @Test
    void testFindRhelInstanceFindsRhel() {
        when(stack.getNotDeletedInstanceMetaDataSet())
                .thenReturn(Set.of(instanceMetaDataRhel, instanceMetaDataUbuntu));

        Optional<String> result = rhelClientHelper.findRhelInstance(stack);

        assertTrue(result.isPresent());
        assertEquals("rhel-instance.example.com", result.get());
    }

    @Test
    void testFindRhelInstanceNoRhelFound() {
        when(stack.getNotDeletedInstanceMetaDataSet())
                .thenReturn(Set.of(instanceMetaDataUbuntu));

        Optional<String> result = rhelClientHelper.findRhelInstance(stack);

        assertTrue(result.isEmpty());
    }

    @Test
    void testFindRhelInstanceExceptionHandled() {
        when(stack.getNotDeletedInstanceMetaDataSet()).thenThrow(new RuntimeException("boom"));

        Optional<String> result = rhelClientHelper.findRhelInstance(stack);

        assertTrue(result.isEmpty());
    }

    @Test
    void testIsClientConnectedToRhelTrue() {
        when(stack.getNotDeletedInstanceMetaDataSet())
                .thenReturn(Set.of(instanceMetaDataRhel));

        when(freeIpaClient.getHostname()).thenReturn("rhel-instance.example.com");

        boolean result = rhelClientHelper.isClientConnectedToRhel(stack, freeIpaClient);

        assertTrue(result);
    }

    @Test
    void testIsClientConnectedToRhelFalseDifferentHost() {
        when(stack.getNotDeletedInstanceMetaDataSet())
                .thenReturn(Set.of(instanceMetaDataRhel));

        when(freeIpaClient.getHostname()).thenReturn("different-host.example.com");

        boolean result = rhelClientHelper.isClientConnectedToRhel(stack, freeIpaClient);

        assertFalse(result);
    }

    @Test
    void testIsClientConnectedToRhelExceptionHandled() {
        when(stack.getNotDeletedInstanceMetaDataSet()).thenThrow(new RuntimeException("boom"));
        when(freeIpaClient.getHostname()).thenReturn("rhel-instance.example.com");

        boolean result = rhelClientHelper.isClientConnectedToRhel(stack, freeIpaClient);

        assertFalse(result);
    }

    @Test
    void testIsClientConnectedToSpecificOsTrue() {
        when(stack.getNotDeletedInstanceMetaDataSet())
                .thenReturn(Set.of(instanceMetaDataRhel));

        OsType osType = mock(OsType.class);
        when(osType.getOs()).thenReturn("redhat8");

        boolean result = rhelClientHelper.isClientConnectedToSpecificOs(stack, osType);

        assertTrue(result);
    }

    @Test
    void testIsClientConnectedToSpecificOsFalse() {
        when(stack.getNotDeletedInstanceMetaDataSet())
                .thenReturn(Set.of(instanceMetaDataUbuntu));

        OsType osType = mock(OsType.class);
        when(osType.getOs()).thenReturn("redhat9");

        boolean result = rhelClientHelper.isClientConnectedToSpecificOs(stack, osType);

        assertFalse(result);
    }

    @Test
    void testIsClientConnectedToSpecificOsExceptionHandled() {
        when(stack.getNotDeletedInstanceMetaDataSet()).thenThrow(new RuntimeException("boom"));

        OsType osType = mock(OsType.class);
        when(osType.getOs()).thenReturn("rhel");

        boolean result = rhelClientHelper.isClientConnectedToSpecificOs(stack, osType);

        assertFalse(result);
    }
}