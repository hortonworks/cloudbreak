package com.sequenceiq.cloudbreak.service.image.userdata;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.cloud.model.Image;
import com.sequenceiq.cloudbreak.core.CloudbreakImageNotFoundException;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.service.image.ImageService;
import com.sequenceiq.cloudbreak.service.proxy.ProxyConfigUserDataReplacer;
import com.sequenceiq.cloudbreak.service.stack.StackService;
import com.sequenceiq.common.api.type.InstanceGroupType;

@ExtendWith(MockitoExtension.class)
public class UserDataServiceTest {

    private static final String TEST_RESOURCE_ID = "aa8997d3-527d-4e7f-af8a-7f7cd10eb8f7";

    private static final String TEST_CLUSTER_CRN = String.format("crn:cdp:datahub:us-west-1:e7b1345f-4ae1-4594-9113-fc91f22ef8bd:cluster:%s", TEST_RESOURCE_ID);

    private static final long STACK_ID = 1L;

    private static final String ENV_CRN = "env-crn";

    @InjectMocks
    private UserDataService underTest;

    @Mock
    private ImageService imageService;

    @Mock
    private StackService stackService;

    @Mock
    private ProxyConfigUserDataReplacer proxyConfigUserDataReplacer;

    @Mock
    private Stack stack;

    @Mock
    private Image image;

    @Captor
    private ArgumentCaptor<Map<InstanceGroupType, String>> imageCaptor;

    @BeforeEach
    void setUp() throws Exception {
        lenient().when(stackService.getByIdWithLists(STACK_ID)).thenReturn(stack);
        lenient().when(imageService.getImage(STACK_ID)).thenReturn(image);
        lenient().when(stack.getEnvironmentCrn()).thenReturn(ENV_CRN);
    }

    @Test
    void updateJumpgateFlagOnly() throws CloudbreakImageNotFoundException {
        when(image.getUserdata()).thenReturn(Map.of(InstanceGroupType.GATEWAY,
                "export FLAG=foo\nexport IS_CCM_ENABLED=true\nexport IS_CCM_V2_ENABLED=false\nexport IS_CCM_V2_JUMPGATE_ENABLED=false\n" +
                        "export OTHER_FLAG=bar"));

        underTest.updateJumpgateFlagOnly(STACK_ID);

        verify(imageService, times(1)).decorateImageWithUserDataForStack(any(), imageCaptor.capture());
        assertThat(imageCaptor.getValue().get(InstanceGroupType.GATEWAY))
                .isEqualTo("export FLAG=foo\nexport IS_CCM_ENABLED=false\nexport IS_CCM_V2_ENABLED=true\nexport IS_CCM_V2_JUMPGATE_ENABLED=true\n" +
                        "export OTHER_FLAG=bar");
    }

    @Test
    void updateProxyConfig() throws CloudbreakImageNotFoundException {
        String gwUserData = "gwUserData";
        Map<InstanceGroupType, String> userData = Map.of(InstanceGroupType.GATEWAY, gwUserData);
        when(image.getUserdata()).thenReturn(userData);
        String replacedGwUserData = "replacedGwUserData";
        when(proxyConfigUserDataReplacer.replaceProxyConfigInUserDataByEnvCrn(gwUserData, ENV_CRN)).thenReturn(replacedGwUserData);

        underTest.updateProxyConfig(STACK_ID);

        verify(proxyConfigUserDataReplacer).replaceProxyConfigInUserDataByEnvCrn(gwUserData, ENV_CRN);
        verify(imageService).decorateImageWithUserDataForStack(any(), imageCaptor.capture());
        assertThat(imageCaptor.getValue().get(InstanceGroupType.GATEWAY))
                .isEqualTo(replacedGwUserData);
    }
}
