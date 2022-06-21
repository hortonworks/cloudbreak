package com.sequenceiq.freeipa.service.image.userdata;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.task.AsyncTaskExecutor;

import com.sequenceiq.cloudbreak.service.proxy.ProxyConfigDtoService;
import com.sequenceiq.freeipa.entity.ImageEntity;
import com.sequenceiq.freeipa.service.CredentialService;
import com.sequenceiq.freeipa.service.client.CachedEnvironmentClientService;
import com.sequenceiq.freeipa.service.cloud.PlatformParameterService;
import com.sequenceiq.freeipa.service.image.ImageService;
import com.sequenceiq.freeipa.service.stack.StackService;

@ExtendWith(MockitoExtension.class)
class UserDataServiceTest {

    private static final long STACK_ID = 123L;

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

    @InjectMocks
    private UserDataService underTest;

    @Test
    void updateJumpgateFlagOnly() {
        ImageEntity image = new ImageEntity();
        image.setUserdata("FLAG=foo\nIS_CCM_ENABLED=true\nIS_CCM_V2_ENABLED=false\nIS_CCM_V2_JUMPGATE_ENABLED=false\nOTHER_FLAG=bar");
        when(imageService.getByStackId(STACK_ID)).thenReturn(image);
        underTest.updateJumpgateFlagOnly(STACK_ID);
        ArgumentCaptor<ImageEntity> imageCaptor = ArgumentCaptor.forClass(ImageEntity.class);
        verify(imageService, times(1)).save(imageCaptor.capture());
        assertThat(imageCaptor.getValue().getUserdata())
                .isEqualTo("FLAG=foo\nIS_CCM_ENABLED=false\nIS_CCM_V2_ENABLED=true\nIS_CCM_V2_JUMPGATE_ENABLED=true\nOTHER_FLAG=bar");
    }
}
