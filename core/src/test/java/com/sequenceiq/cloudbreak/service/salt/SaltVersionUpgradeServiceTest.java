package com.sequenceiq.cloudbreak.service.salt;

import static com.sequenceiq.cloudbreak.rotation.CloudbreakSecretType.SALT_MASTER_KEY_PAIR;
import static com.sequenceiq.cloudbreak.rotation.CloudbreakSecretType.SALT_SIGN_KEY_PAIR;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;

import org.apache.commons.codec.binary.Base64;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.certificate.PkiUtil;
import com.sequenceiq.cloudbreak.cloud.model.Image;
import com.sequenceiq.cloudbreak.cloud.model.catalog.ImagePackageVersion;
import com.sequenceiq.cloudbreak.common.exception.BadRequestException;
import com.sequenceiq.cloudbreak.common.json.Json;
import com.sequenceiq.cloudbreak.core.CloudbreakImageNotFoundException;
import com.sequenceiq.cloudbreak.domain.SaltSecurityConfig;
import com.sequenceiq.cloudbreak.domain.SecurityConfig;
import com.sequenceiq.cloudbreak.dto.StackDto;
import com.sequenceiq.cloudbreak.rotation.flow.chain.SecretRotationFlowChainTriggerEvent;
import com.sequenceiq.cloudbreak.service.image.ImageService;
import com.sequenceiq.cloudbreak.service.stack.StackDtoService;
import com.sequenceiq.cloudbreak.view.InstanceMetadataView;

@ExtendWith(MockitoExtension.class)
class SaltVersionUpgradeServiceTest {

    private static final Long STACK_ID = 1L;

    @InjectMocks
    private SaltVersionUpgradeService underTest;

    @Mock
    private ImageService imageService;

    @Mock
    private StackDtoService stackDtoService;

    @Mock
    private StackDto stackDto;

    @Test
    void testanyGatewayInstanceHasOutdatedSaltVersionsWhenTheyMatch() throws CloudbreakImageNotFoundException {
        when(imageService.getImage(STACK_ID)).thenReturn(getImage("3001.8"));
        StackDto stackDto = stack(List.of(instanceMetadata("1", "3001.8")));
        assertDoesNotThrow(() -> underTest.validateSaltVersion(stackDto));
    }

    @Test
    void testanyGatewayInstanceHasOutdatedSaltVersionsWhenTheyDontMatch() throws CloudbreakImageNotFoundException {
        when(imageService.getImage(STACK_ID)).thenReturn(getImage("3006.9"));
        StackDto stackDto = stack(List.of(instanceMetadata("1", "3001.8")));
        BadRequestException badRequestException = assertThrows(BadRequestException.class, () -> underTest.validateSaltVersion(stackDto));
        assertEquals("Salt package version is outdated on the gateway node(s). Please repair the gateway node(s) [1] first!",
                badRequestException.getMessage());
    }

    @Test
    void testHasOutdatedSaltVersionsWhenImageSaltVersionIsBlankOnGateways() throws CloudbreakImageNotFoundException {
        when(imageService.getImage(STACK_ID)).thenReturn(getImage(""));
        StackDto stackDto = stack(List.of(instanceMetadata("1", "3001.8")));
        assertDoesNotThrow(() -> underTest.validateSaltVersion(stackDto));
    }

    @Test
    void testHasOutdatedSaltVersionsWhenGatewaySaltVersionIsBlankOnGateways() throws CloudbreakImageNotFoundException {
        when(imageService.getImage(STACK_ID)).thenReturn(getImage("3006.9"));
        StackDto stackDto = stack(List.of(instanceMetadata("1", "")));
        assertDoesNotThrow(() -> underTest.validateSaltVersion(stackDto));
    }

    @Test
    void testHasOutdatedSaltVersionsWhenGatewaySaltVersionIsNullOnGateways() throws CloudbreakImageNotFoundException {
        when(imageService.getImage(STACK_ID)).thenReturn(getImage("3006.9"));
        StackDto stackDto = stack(List.of(instanceMetadata("1", null)));
        assertDoesNotThrow(() -> underTest.validateSaltVersion(stackDto));
    }

    @Test
    void testanyGatewayInstanceHasOutdatedSaltVersionsWhenGatewayImageJsonThrowsException() throws CloudbreakImageNotFoundException {
        when(imageService.getImage(STACK_ID)).thenReturn(getImage("3006.9"));
        InstanceMetadataView instanceMetadata = instanceMetadata("1", "3001.8");
        when(instanceMetadata.getImage()).thenReturn(Json.silent("invalid"));
        StackDto stackDto = stack(List.of(instanceMetadata));
        assertDoesNotThrow(() -> underTest.validateSaltVersion(stackDto));
    }

    @Test
    void testanyGatewayInstanceHasOutdatedSaltVersionsWhenImageIsMissing() throws CloudbreakImageNotFoundException {
        when(imageService.getImage(STACK_ID)).thenThrow(new CloudbreakImageNotFoundException("error"));
        StackDto stackDto = stack(List.of(instanceMetadata("1", "3001.8")));
        BadRequestException badRequestException = assertThrows(BadRequestException.class, () -> underTest.validateSaltVersion(stackDto));
        assertEquals("Image not found", badRequestException.getMessage());
    }

    @Test
    void testHasOutdatedSaltVersionsWhenImageAndGatewaySaltVersionsOnGatewaysAreBlank() throws CloudbreakImageNotFoundException {
        when(imageService.getImage(STACK_ID)).thenReturn(getImage(""));
        StackDto stackDto = stack(List.of(instanceMetadata("1", "")));
        assertDoesNotThrow(() -> underTest.validateSaltVersion(stackDto));
    }

    @Test
    void testGetSaltSecretRotationTriggerEventWhenSaltMasterPrivateKeyNotExistsAndLegacySaltSignPublicKeyExists() {
        SecurityConfig securityConfig = new SecurityConfig();
        SaltSecurityConfig saltSecurityConfig = new SaltSecurityConfig();
        saltSecurityConfig.setSaltSignPublicKey(new String(Base64.encodeBase64("ssh-rsa".getBytes())));
        securityConfig.setSaltSecurityConfig(saltSecurityConfig);
        when(stackDto.getSecurityConfig()).thenReturn(securityConfig);
        when(stackDto.getAllAvailableGatewayInstances()).thenReturn(List.of(mock(InstanceMetadataView.class), mock(InstanceMetadataView.class)));
        when(stackDtoService.getByIdWithoutResources(eq(STACK_ID))).thenReturn(stackDto);
        List<SecretRotationFlowChainTriggerEvent> triggerEvent = underTest.getSaltSecretRotationTriggerEvent(STACK_ID);
        assertThat(triggerEvent.get(0).getSecretTypes()).containsExactlyInAnyOrder(SALT_MASTER_KEY_PAIR, SALT_SIGN_KEY_PAIR);
        assertNull(triggerEvent.get(0).getExecutionType());
        assertNull(triggerEvent.get(0).getAdditionalProperties());
    }

    @Test
    void testGetSaltSecretRotationTriggerEventWhenSaltMasterPrivateKeyExistsAndLegacySaltSignPublicKeyNotExists() {
        SecurityConfig securityConfig = new SecurityConfig();
        SaltSecurityConfig saltSecurityConfig = new SaltSecurityConfig();
        saltSecurityConfig.setSaltMasterPrivateKey(PkiUtil.generatePemPrivateKeyInBase64());
        securityConfig.setSaltSecurityConfig(saltSecurityConfig);
        when(stackDto.getSecurityConfig()).thenReturn(securityConfig);
        when(stackDto.getAllAvailableGatewayInstances()).thenReturn(List.of(mock(InstanceMetadataView.class), mock(InstanceMetadataView.class)));
        when(stackDtoService.getByIdWithoutResources(eq(STACK_ID))).thenReturn(stackDto);
        List<SecretRotationFlowChainTriggerEvent> triggerEvent = underTest.getSaltSecretRotationTriggerEvent(STACK_ID);
        assertTrue(triggerEvent.isEmpty());
    }

    private StackDto stack(List<InstanceMetadataView> instances) {
        StackDto stackDto = mock(StackDto.class);
        lenient().when(stackDto.getId()).thenReturn(STACK_ID);
        lenient().when(stackDto.getNotTerminatedAndNotZombieGatewayInstanceMetadata()).thenReturn(instances);
        return stackDto;
    }

    private InstanceMetadataView instanceMetadata(String instanceId, String saltVersion) {
        InstanceMetadataView instanceMetadata = mock(InstanceMetadataView.class);
        lenient().when(instanceMetadata.getInstanceId()).thenReturn(instanceId);
        lenient().when(instanceMetadata.getImage()).thenReturn(Json.silent(getImage(saltVersion)));
        return instanceMetadata;
    }

    private Image getImage(String saltVersion) {
        Map<String, String> packageVersions = saltVersion != null ? Map.of(ImagePackageVersion.SALT.getKey(), saltVersion) : Map.of();
        return new Image("image", null, null, null, null, null, null, null,
                packageVersions, null, null);
    }

}