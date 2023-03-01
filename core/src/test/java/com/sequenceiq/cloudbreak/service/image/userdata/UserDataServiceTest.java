package com.sequenceiq.cloudbreak.service.image.userdata;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Map;
import java.util.Optional;

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
import com.sequenceiq.cloudbreak.domain.Userdata;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.repository.UserdataRepository;
import com.sequenceiq.cloudbreak.service.image.ImageService;
import com.sequenceiq.cloudbreak.service.proxy.ProxyConfigUserDataReplacer;
import com.sequenceiq.cloudbreak.service.secret.service.SecretService;
import com.sequenceiq.cloudbreak.service.stack.StackService;
import com.sequenceiq.cloudbreak.workspace.model.Tenant;
import com.sequenceiq.cloudbreak.workspace.model.Workspace;
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
    void updateJumpgateFlagOnly() throws CloudbreakImageNotFoundException {
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
    void updateProxyConfig() throws CloudbreakImageNotFoundException {
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
}
