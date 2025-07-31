package com.sequenceiq.environment.encryptionprofile.v1.controller;

import static com.sequenceiq.environment.encryptionprofile.EncryptionProfileTestConstants.ACCOUNT_ID;
import static com.sequenceiq.environment.encryptionprofile.EncryptionProfileTestConstants.ENCRYPTION_PROFILE_CRN;
import static com.sequenceiq.environment.encryptionprofile.EncryptionProfileTestConstants.NAME;
import static com.sequenceiq.environment.encryptionprofile.EncryptionProfileTestConstants.USER_CRN;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.ForbiddenException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.authorization.resource.AuthorizationResourceAction;
import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.cloudbreak.event.ResourceEvent;
import com.sequenceiq.cloudbreak.tls.DefaultEncryptionProfileProvider;
import com.sequenceiq.common.api.encryptionprofile.TlsVersion;
import com.sequenceiq.environment.api.v1.encryptionprofile.model.CipherSuitesByTlsVersionResponse;
import com.sequenceiq.environment.api.v1.encryptionprofile.model.EncryptionProfileRequest;
import com.sequenceiq.environment.api.v1.encryptionprofile.model.EncryptionProfileResponse;
import com.sequenceiq.environment.api.v1.encryptionprofile.model.EncryptionProfileResponses;
import com.sequenceiq.environment.api.v1.encryptionprofile.model.TlsVersionResponse;
import com.sequenceiq.environment.authorization.EncryptionProfileFiltering;
import com.sequenceiq.environment.encryptionprofile.domain.EncryptionProfile;
import com.sequenceiq.environment.encryptionprofile.service.EncryptionProfileService;
import com.sequenceiq.environment.encryptionprofile.v1.converter.EncryptionProfileRequestToEncryptionProfileConverter;
import com.sequenceiq.environment.encryptionprofile.v1.converter.EncryptionProfileToEncryptionProfileResponseConverter;
import com.sequenceiq.notification.NotificationService;

@ExtendWith(MockitoExtension.class)
public class EncryptionProfileControllerTest {

    @Mock
    private EncryptionProfileService encryptionProfileService;

    @Mock
    private NotificationService notificationService;

    @Mock
    private EntitlementService entitlementService;

    @Mock
    private EncryptionProfileFiltering encryptionProfileFiltering;

    @Mock
    private EncryptionProfileRequestToEncryptionProfileConverter requestConverter;

    @Mock
    private EncryptionProfileToEncryptionProfileResponseConverter responseConverter;

    @Mock
    private DefaultEncryptionProfileProvider defaultEncryptionProfileProvider;

    @InjectMocks
    private EncryptionProfileController controller;

    @BeforeEach
    public void setUp() throws Exception {
        controller = new EncryptionProfileController(encryptionProfileService,
                requestConverter,
                responseConverter,
                encryptionProfileFiltering,
                entitlementService);
        // Inject mock NotificationService into the superclass via reflection
        Field field = controller.getClass().getSuperclass().getDeclaredField("notificationService");
        field.setAccessible(true);
        field.set(controller, notificationService);
    }

    @Test
    public void testCreate() {
        when(entitlementService.isConfigureEncryptionProfileEnabled(anyString())).thenReturn(true);
        EncryptionProfileRequest request = new EncryptionProfileRequest();
        request.setTlsVersions(Set.of(TlsVersion.TLS_1_2));
        EncryptionProfile profile = new EncryptionProfile();
        EncryptionProfileResponse expectedResponse = new EncryptionProfileResponse();

        when(requestConverter.convert(request)).thenReturn(profile);
        when(encryptionProfileService.create(eq(profile), anyString(), anyString())).thenReturn(profile);
        when(responseConverter.convert(profile)).thenReturn(expectedResponse);

        EncryptionProfileResponse actualResponse = ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> controller.create(request));

        assertThat(actualResponse).isEqualTo(expectedResponse);

        verify(requestConverter).convert(request);
        verify(encryptionProfileService).create(eq(profile), anyString(), anyString());
        verify(responseConverter).convert(profile);
        verify(notificationService).send(eq(ResourceEvent.ENCRYPTION_PROFILE_CREATED), any(), Optional.ofNullable(any()));
    }

    @Test
    public void testGetByName() {
        EncryptionProfile profile = new EncryptionProfile();
        EncryptionProfileResponse expectedResponse = new EncryptionProfileResponse();

        when(encryptionProfileService.getByNameAndAccountId(eq(NAME), eq(ACCOUNT_ID)))
                .thenReturn(profile);
        when(responseConverter.convert(profile)).thenReturn(expectedResponse);

        EncryptionProfileResponse actualResponse = ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> controller.getByName(NAME));

        assertThat(actualResponse).isEqualTo(expectedResponse);

        verify(encryptionProfileService).getByNameAndAccountId(eq(NAME), eq(ACCOUNT_ID));
        verify(responseConverter).convert(profile);
    }

    @Test
    public void testGetByCrn() {
        EncryptionProfile profile = new EncryptionProfile();
        EncryptionProfileResponse expectedResponse = new EncryptionProfileResponse();

        when(encryptionProfileService.getByCrn(eq(ENCRYPTION_PROFILE_CRN))).thenReturn(profile);
        when(responseConverter.convert(profile)).thenReturn(expectedResponse);

        EncryptionProfileResponse actualResponse = ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> controller.getByCrn(ENCRYPTION_PROFILE_CRN));

        assertThat(actualResponse).isEqualTo(expectedResponse);

        verify(encryptionProfileService).getByCrn(eq(ENCRYPTION_PROFILE_CRN));
        verify(responseConverter).convert(profile);
    }

    @Test
    public void testList() {
        EncryptionProfileResponse response1 = new EncryptionProfileResponse();
        EncryptionProfileResponse response2 = new EncryptionProfileResponse();
        EncryptionProfileResponses expectedResponses = new EncryptionProfileResponses(new HashSet<>(Arrays.asList(response1, response2)));

        when(encryptionProfileFiltering.filterResources(any(), any(), any()))
                .thenReturn(expectedResponses);

        EncryptionProfileResponses actualResponses = ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> controller.list());

        assertThat(actualResponses).isEqualTo(expectedResponses);

        verify(encryptionProfileFiltering).filterResources(
                any(),
                eq(AuthorizationResourceAction.DESCRIBE_ENCRYPTION_PROFILE),
                eq(Collections.emptyMap()));
    }

    @Test
    public void testDeleteByName() {
        when(entitlementService.isConfigureEncryptionProfileEnabled(anyString())).thenReturn(true);
        EncryptionProfile profile = new EncryptionProfile();
        EncryptionProfileResponse expectedResponse = new EncryptionProfileResponse();

        when(encryptionProfileService.deleteByNameAndAccountId(eq(NAME), eq(ACCOUNT_ID)))
                .thenReturn(profile);
        when(responseConverter.convert(profile)).thenReturn(expectedResponse);

        EncryptionProfileResponse actualResponse = ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> controller.deleteByName(NAME));

        assertThat(actualResponse).isEqualTo(expectedResponse);

        verify(encryptionProfileService).deleteByNameAndAccountId(eq(NAME), eq(ACCOUNT_ID));
        verify(responseConverter).convert(profile);
        verify(notificationService).send(eq(ResourceEvent.ENCRYPTION_PROFILE_DELETED), any(), Optional.ofNullable(any()));
    }

    @Test
    public void testDeleteByCrn() {
        when(entitlementService.isConfigureEncryptionProfileEnabled(anyString())).thenReturn(true);

        EncryptionProfile profile = new EncryptionProfile();
        EncryptionProfileResponse expectedResponse = new EncryptionProfileResponse();

        when(encryptionProfileService.deleteByResourceCrn(eq(ENCRYPTION_PROFILE_CRN))).thenReturn(profile);
        when(responseConverter.convert(profile)).thenReturn(expectedResponse);

        EncryptionProfileResponse actualResponse = ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> controller.deleteByCrn(ENCRYPTION_PROFILE_CRN));

        assertThat(actualResponse).isEqualTo(expectedResponse);

        verify(encryptionProfileService).deleteByResourceCrn(eq(ENCRYPTION_PROFILE_CRN));
        verify(responseConverter).convert(profile);
        verify(notificationService).send(eq(ResourceEvent.ENCRYPTION_PROFILE_DELETED), any(), Optional.ofNullable(any()));
    }

    @Test
    public void testVerifyEncryptionProfileEntitlementForAllEndpoints() {

        when(entitlementService.isConfigureEncryptionProfileEnabled(anyString())).thenReturn(false);

        assertThatThrownBy(() ->
            ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> controller.create(new EncryptionProfileRequest())))
                .isInstanceOf(ForbiddenException.class)
                .hasMessageContaining("Encryption profile operations are not enabled for account: cloudbreak");

        assertThatThrownBy(() ->
                ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> controller.deleteByName("test-name")))
                .isInstanceOf(ForbiddenException.class)
                .hasMessageContaining("Encryption profile operations are not enabled for account: cloudbreak");

        assertThatThrownBy(() ->
                ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> controller.deleteByCrn("test-crn")))
                .isInstanceOf(ForbiddenException.class)
                .hasMessageContaining("Encryption profile operations are not enabled for account: cloudbreak");
    }

    @Test
    public void testListCiphersByTlsVersion() {
        when(entitlementService.isConfigureEncryptionProfileEnabled(anyString())).thenReturn(true);
        when(encryptionProfileService.listCiphersByTlsVersion()).thenReturn(Set.of(
                new TlsVersionResponse(TlsVersion.TLS_1_2.getVersion(),
                        Set.of("TLS_ECDHE_PSK_WITH_AES_128_CCM_8_SHA256",
                                "TLS_ECCPWD_WITH_AES_128_CCM_SHA256",
                                "TLS_ECDHE_RSA_WITH_CAMELLIA_128_GCM_SHA256",
                                "TLS_ECDHE_PSK_WITH_AES_128_CCM_SHA256",
                                "TLS_AES_128_CCM_8_SHA256", "TLS_AES_128_CCM_SHA256"),
                        Set.of("TLS_AES_256_GCM_SHA384", "TLS_AES_128_GCM_SHA256",
                                "TLS_ECDHE_ECDSA_WITH_AES_128_GCM_SHA256",
                                "TLS_ECDHE_PSK_WITH_CHACHA20_POLY1305_SHA256",
                                "TLS_ECDHE_PSK_WITH_AES_256_GCM_SHA384",
                                "TLS_ECDHE_ECDSA_WITH_CAMELLIA_128_GCM_SHA256",
                                "TLS_ECDHE_ECDSA_WITH_ARIA_256_GCM_SHA384",
                                "TLS_ECDHE_ECDSA_WITH_ARIA_128_GCM_SHA256",
                                "TLS_ECDHE_ECDSA_WITH_AES_256_GCM_SHA384",
                                "TLS_ECDHE_PSK_WITH_AES_128_GCM_SHA256",
                                "TLS_ECDHE_ECDSA_WITH_CHACHA20_POLY1305_SHA256",
                                "TLS_ECDHE_ECDSA_WITH_CAMELLIA_256_GCM_SHA384",
                                "TLS_ECCPWD_WITH_AES_128_GCM_SHA256",
                                "TLS_ECCPWD_WITH_AES_256_GCM_SHA384",
                                "TLS_CHACHA20_POLY1305_SHA256"
                        )
                ),
                new TlsVersionResponse(TlsVersion.TLS_1_3.getVersion(), Set.of(), Set.of())
        ));

        CipherSuitesByTlsVersionResponse result = ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> controller.listCiphersByTlsVersion());

        Set<TlsVersionResponse> tlsVersions = result.getTlsVersions();
        Set<String> versions = tlsVersions.stream().map(TlsVersionResponse::getTlsVersion).collect(Collectors.toSet());
        Set<String> ciphers = tlsVersions.stream()
                .flatMap(version -> version.getCipherSuites().stream())
                .collect(Collectors.toSet());
        Set<String> recommended = tlsVersions.stream()
                .flatMap(version -> version.getRecommended().stream())
                .collect(Collectors.toSet());

        assertTrue(versions.containsAll(List.of(TlsVersion.TLS_1_2.getVersion(), TlsVersion.TLS_1_3.getVersion())), result.toString());
        assertTrue(ciphers.containsAll(List.of("TLS_ECDHE_PSK_WITH_AES_128_CCM_8_SHA256", "TLS_ECCPWD_WITH_AES_128_CCM_SHA256",
                "TLS_ECDHE_RSA_WITH_CAMELLIA_128_GCM_SHA256", "TLS_ECDHE_PSK_WITH_AES_128_CCM_SHA256", "TLS_AES_128_CCM_8_SHA256", "TLS_AES_128_CCM_SHA256")));
        assertTrue(recommended.containsAll(List.of("TLS_AES_256_GCM_SHA384", "TLS_AES_128_GCM_SHA256",
                "TLS_ECDHE_ECDSA_WITH_AES_128_GCM_SHA256", "TLS_ECDHE_PSK_WITH_CHACHA20_POLY1305_SHA256", "TLS_ECDHE_PSK_WITH_AES_256_GCM_SHA384",
                "TLS_ECDHE_ECDSA_WITH_CAMELLIA_128_GCM_SHA256", "TLS_ECDHE_ECDSA_WITH_ARIA_256_GCM_SHA384", "TLS_ECDHE_ECDSA_WITH_ARIA_128_GCM_SHA256",
                "TLS_ECDHE_ECDSA_WITH_AES_256_GCM_SHA384", "TLS_ECDHE_PSK_WITH_AES_128_GCM_SHA256", "TLS_ECDHE_ECDSA_WITH_CHACHA20_POLY1305_SHA256",
                "TLS_ECDHE_ECDSA_WITH_CAMELLIA_256_GCM_SHA384", "TLS_ECCPWD_WITH_AES_128_GCM_SHA256", "TLS_ECCPWD_WITH_AES_256_GCM_SHA384",
                "TLS_CHACHA20_POLY1305_SHA256")));
    }

    @Test
    public void testCreateTls13OnlyShouldThrowBadRequestException() {
        EncryptionProfileRequest request = new EncryptionProfileRequest();
        request.setTlsVersions(Set.of(TlsVersion.TLS_1_3));

        when(entitlementService.isConfigureEncryptionProfileEnabled(anyString())).thenReturn(true);

        assertThatThrownBy(() ->
                ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> controller.create(request)))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("TLS 1.3 only is not supported yet. Use TLSv1.2 and TLSv1.3 or TLSv1.2 only");
    }
}
