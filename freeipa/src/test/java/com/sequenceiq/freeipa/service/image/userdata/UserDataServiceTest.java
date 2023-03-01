package com.sequenceiq.freeipa.service.image.userdata;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.assertj.core.api.AbstractStringAssert;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.task.AsyncTaskExecutor;

import com.sequenceiq.cloudbreak.service.proxy.ProxyConfigDtoService;
import com.sequenceiq.cloudbreak.service.proxy.ProxyConfigUserDataReplacer;
import com.sequenceiq.cloudbreak.service.secret.service.SecretService;
import com.sequenceiq.freeipa.entity.ImageEntity;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.service.CredentialService;
import com.sequenceiq.freeipa.service.client.CachedEnvironmentClientService;
import com.sequenceiq.freeipa.service.cloud.PlatformParameterService;
import com.sequenceiq.freeipa.service.image.ImageService;
import com.sequenceiq.freeipa.service.stack.StackService;

@ExtendWith(MockitoExtension.class)
class UserDataServiceTest {

    private static final long STACK_ID = 123L;

    private static final String ENV_CRN = "env-crn";

    @Mock
    private UserDataBuilder userDataBuilder;

    @Mock
    private PlatformParameterService platformParameterService;

    @Mock
    private AsyncTaskExecutor intermediateBuilderExecutor;

    @Mock
    private CredentialService credentialService;

    @Mock
    private ProxyConfigDtoService proxyConfigDtoService;

    @Mock
    private StackService stackService;

    @Mock
    private ImageService imageService;

    @Mock
    private CcmUserDataService ccmUserDataService;

    @Mock
    private CachedEnvironmentClientService environmentClientService;

    @Mock
    private ProxyConfigUserDataReplacer proxyConfigUserDataReplacer;

    @InjectMocks
    private UserDataService underTest;

    @Captor
    private ArgumentCaptor<ImageEntity> imageCaptor;

    @Mock
    private Stack stack;

    @Mock
    private SecretService secretService;

    @BeforeEach
    void setUp() {
        lenient().when(stackService.getStackById(STACK_ID)).thenReturn(stack);
        lenient().when(stack.getEnvironmentCrn()).thenReturn(ENV_CRN);
    }

    @Test
    void updateJumpgateFlagOnly() {
        setUserData("export FLAG=foo\nexport IS_CCM_ENABLED=true\nexport IS_CCM_V2_ENABLED=false\n" +
                "export IS_CCM_V2_JUMPGATE_ENABLED=false\nexport OTHER_FLAG=bar");

        underTest.updateJumpgateFlagOnly(STACK_ID);

        assertThatUserData().isEqualTo("export FLAG=foo\nexport IS_CCM_ENABLED=false\nexport IS_CCM_V2_ENABLED=true\n" +
                "export IS_CCM_V2_JUMPGATE_ENABLED=true\nexport OTHER_FLAG=bar");
    }

    @Test
    void updateProxyConfig() {
        String userData = "userdata";
        setUserData(userData);
        String replacedUserData = "replaced-userdata";
        when(proxyConfigUserDataReplacer.replaceProxyConfigInUserDataByEnvCrn(userData, ENV_CRN)).thenReturn(replacedUserData);

        underTest.updateProxyConfig(STACK_ID);

        assertThatUserData().isEqualTo(replacedUserData);
    }

    private void setUserData(String userData) {
        ImageEntity image = new ImageEntity();
        image.setUserdata(userData);
        image.setGatewayUserdata(userData);
        when(imageService.getByStackId(STACK_ID)).thenReturn(image);
    }

    private AbstractStringAssert<?> assertThatUserData() {
        verify(imageService).save(imageCaptor.capture());
        return assertThat(imageCaptor.getValue().getGatewayUserdata());
    }
}
