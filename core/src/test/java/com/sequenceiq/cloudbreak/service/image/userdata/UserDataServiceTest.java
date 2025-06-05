package com.sequenceiq.cloudbreak.service.image.userdata;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import org.apache.commons.codec.binary.Base64;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.task.AsyncTaskExecutor;

import com.sequenceiq.cloudbreak.ccm.cloudinit.CcmConnectivityParameters;
import com.sequenceiq.cloudbreak.certificate.PkiUtil;
import com.sequenceiq.cloudbreak.cloud.PlatformParameters;
import com.sequenceiq.cloudbreak.cloud.model.Image;
import com.sequenceiq.cloudbreak.cloud.model.Platform;
import com.sequenceiq.cloudbreak.cloud.model.Variant;
import com.sequenceiq.cloudbreak.domain.SaltSecurityConfig;
import com.sequenceiq.cloudbreak.domain.SecurityConfig;
import com.sequenceiq.cloudbreak.domain.StackAuthentication;
import com.sequenceiq.cloudbreak.domain.Userdata;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.repository.UserdataRepository;
import com.sequenceiq.cloudbreak.service.environment.EnvironmentService;
import com.sequenceiq.cloudbreak.service.image.ImageService;
import com.sequenceiq.cloudbreak.service.proxy.ProxyConfigDtoService;
import com.sequenceiq.cloudbreak.service.proxy.ProxyConfigUserDataReplacer;
import com.sequenceiq.cloudbreak.service.secret.service.SecretService;
import com.sequenceiq.cloudbreak.service.securityconfig.SecurityConfigService;
import com.sequenceiq.cloudbreak.service.stack.StackService;
import com.sequenceiq.cloudbreak.workspace.model.Tenant;
import com.sequenceiq.cloudbreak.workspace.model.Workspace;
import com.sequenceiq.common.api.type.InstanceGroupType;
import com.sequenceiq.environment.api.v1.environment.model.response.DetailedEnvironmentResponse;

@ExtendWith(MockitoExtension.class)
public class UserDataServiceTest {

    private static final String TEST_RESOURCE_ID = "aa8997d3-527d-4e7f-af8a-7f7cd10eb8f7";

    private static final String TEST_CLUSTER_CRN = String.format("crn:cdp:datahub:us-west-1:e7b1345f-4ae1-4594-9113-fc91f22ef8bd:cluster:%s", TEST_RESOURCE_ID);

    private static final long STACK_ID = 1L;

    private static final String ENV_CRN = "env-crn";

    @InjectMocks
    private UserDataService underTest;

    @Mock
    private UserdataRepository userdataRepository;

    @Mock
    private SecretService secretService;

    @Mock
    private ImageService imageService;

    @Mock
    private StackService stackService;

    @Mock
    private ProxyConfigUserDataReplacer proxyConfigUserDataReplacer;

    @Mock
    private Stack stack;

    @Mock
    private Workspace workspace;

    @Mock
    private Tenant tenant;

    @Mock
    private Image image;

    @Mock
    private UserDataBuilder userDataBuilder;

    @Mock
    private SecurityConfigService securityConfigService;

    @Mock
    private CcmUserDataService ccmUserDataService;

    @Mock
    private ProxyConfigDtoService proxyConfigDtoService;

    @Mock
    private EnvironmentService environmentClientService;

    @Mock
    private AsyncTaskExecutor intermediateBuilderExecutor;

    @Captor
    private ArgumentCaptor<Map<InstanceGroupType, String>> imageCaptor;

    @Captor
    private ArgumentCaptor<Userdata> userdataCaptor;

    @BeforeEach
    void setUp() throws Exception {
        lenient().when(tenant.getName()).thenReturn("tenant");
        lenient().when(workspace.getTenant()).thenReturn(tenant);
        lenient().when(stack.getWorkspace()).thenReturn(workspace);
        lenient().when(stackService.get(any())).thenReturn(stack);
        lenient().when(stackService.getByIdWithLists(STACK_ID)).thenReturn(stack);
        lenient().when(imageService.getImage(STACK_ID)).thenReturn(image);
        lenient().when(stack.getEnvironmentCrn()).thenReturn(ENV_CRN);
    }

    @Test
    void updateJumpgateFlagOnly() {
        Userdata userdata = new Userdata();
        userdata.setGatewayUserdata("export FLAG=foo\nexport IS_CCM_ENABLED=true\nexport IS_CCM_V2_ENABLED=false\nexport IS_CCM_V2_JUMPGATE_ENABLED=false\n" +
                "export OTHER_FLAG=bar");
        when(userdataRepository.findByStackId(any())).thenReturn(Optional.of(userdata));

        underTest.updateJumpgateFlagOnly(STACK_ID);

        verify(userdataRepository, times(1)).save(userdataCaptor.capture());
        assertThat(userdataCaptor.getValue().getGatewayUserdata())
                .isEqualTo("export FLAG=foo\nexport IS_CCM_ENABLED=false\nexport IS_CCM_V2_ENABLED=true\nexport IS_CCM_V2_JUMPGATE_ENABLED=true\n" +
                        "export OTHER_FLAG=bar");
    }

    @Test
    void updateProxyConfig() {
        String gwUserData = "gwUserData";
        Userdata userdata = new Userdata();
        userdata.setGatewayUserdata(gwUserData);
        when(userdataRepository.findByStackId(any())).thenReturn(Optional.of(userdata));
        String replacedGwUserData = "replacedGwUserData";
        when(proxyConfigUserDataReplacer.replaceProxyConfigInUserDataByEnvCrn(gwUserData, ENV_CRN)).thenReturn(replacedGwUserData);

        underTest.updateProxyConfig(STACK_ID);

        verify(proxyConfigUserDataReplacer).replaceProxyConfigInUserDataByEnvCrn(gwUserData, ENV_CRN);
        verify(userdataRepository).save(userdataCaptor.capture());
        assertThat(userdataCaptor.getValue().getGatewayUserdata())
                .isEqualTo(replacedGwUserData);
    }

    @Test
    void createUserData() throws ExecutionException, InterruptedException {
        CcmConnectivityParameters ccmConnectivityParameters = mock(CcmConnectivityParameters.class);
        DetailedEnvironmentResponse detailedEnvironmentResponse = mock(DetailedEnvironmentResponse.class);
        Future<PlatformParameters> platformParametersFuture = mock(Future.class);
        PlatformParameters platformParameters = mock(PlatformParameters.class);
        SecurityConfig securityConfig = new SecurityConfig();
        SaltSecurityConfig saltSecurityConfig = new SaltSecurityConfig();
        saltSecurityConfig.setSaltBootPassword("saltBootPassword");
        saltSecurityConfig.setSaltBootSignPrivateKey("cbPrivKey");
        securityConfig.setSaltSecurityConfig(saltSecurityConfig);
        securityConfig.setClientCert("cbCert");
        StackAuthentication stackAuthentication = new StackAuthentication();
        stackAuthentication.setLoginUserName("loginUserName");
        Map<InstanceGroupType, String> userdataMap = Map.of(InstanceGroupType.GATEWAY, "gwUserdata", InstanceGroupType.CORE, "coreUserdata");
        when(intermediateBuilderExecutor.submit(any(Callable.class))).thenReturn(platformParametersFuture);
        when(platformParametersFuture.get()).thenReturn(platformParameters);
        when(securityConfigService.initSaltSecurityConfigs(stack)).thenReturn(securityConfig);
        when(stack.getStackAuthentication()).thenReturn(stackAuthentication);
        when(stack.getCloudPlatform()).thenReturn("AWS");
        when(stack.getPlatformVariant()).thenReturn("AWS");
        when(ccmUserDataService.fetchAndSaveCcmParameters(stack)).thenReturn(ccmConnectivityParameters);
        when(proxyConfigDtoService.getByEnvironmentCrn(ENV_CRN)).thenReturn(Optional.empty());
        when(environmentClientService.getByCrn(ENV_CRN)).thenReturn(detailedEnvironmentResponse);
        when(userDataBuilder.buildUserData(any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any())).thenReturn(userdataMap);

        try (
                MockedStatic<Base64> base64Mock = mockStatic(Base64.class);
                MockedStatic<PkiUtil> pkiUtilMock = mockStatic(PkiUtil.class)
        ) {
            base64Mock.when(() -> Base64.decodeBase64(anyString())).thenReturn(new byte[0]);
            pkiUtilMock.when(() -> PkiUtil.getPublicKeyDer(any())).thenReturn("cbSshKeyDer".getBytes());
            underTest.createUserData(STACK_ID);
        }

        verify(userDataBuilder, times(1)).buildUserData(
                eq(Platform.platform("AWS")),
                eq(Variant.variant("AWS")),
                eq("cbSshKeyDer".getBytes()),
                eq("loginUserName"),
                eq(platformParameters),
                eq("saltBootPassword"),
                eq("cbCert"),
                eq(ccmConnectivityParameters),
                isNull(),
                eq(detailedEnvironmentResponse),
                eq(STACK_ID));
        verify(userdataRepository, times(1)).save(userdataCaptor.capture());
        Userdata capturedUserData = userdataCaptor.getValue();
        assertEquals("tenant", capturedUserData.getAccountId());
        assertEquals("gwUserdata", capturedUserData.getGatewayUserdata());
        assertEquals("coreUserdata", capturedUserData.getCoreUserdata());
        assertEquals(stack, capturedUserData.getStack());
    }
}
