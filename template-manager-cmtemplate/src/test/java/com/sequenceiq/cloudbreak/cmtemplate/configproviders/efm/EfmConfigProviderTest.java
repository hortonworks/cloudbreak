package com.sequenceiq.cloudbreak.cmtemplate.configproviders.efm;

import static com.sequenceiq.cloudbreak.cmtemplate.CMRepositoryVersionUtil.CLOUDERA_STACK_VERSION_7_2_17;
import static com.sequenceiq.cloudbreak.cmtemplate.CMRepositoryVersionUtil.CLOUDERA_STACK_VERSION_7_2_18;
import static com.sequenceiq.cloudbreak.cmtemplate.configproviders.efm.EfmConfigProvider.EFM_ADMIN_GROUP_IDENTITIES;
import static com.sequenceiq.cloudbreak.cmtemplate.configproviders.efm.EfmConfigProvider.EFM_ADMIN_IDENTITIES;
import static java.util.Map.entry;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.cloudera.api.swagger.model.ApiClusterTemplateConfig;
import com.cloudera.thunderhead.service.usermanagement.UserManagementProto.User;
import com.sequenceiq.cloudbreak.auth.altus.GrpcUmsClient;
import com.sequenceiq.cloudbreak.auth.altus.UmsVirtualGroupRight;
import com.sequenceiq.cloudbreak.auth.altus.VirtualGroupService;
import com.sequenceiq.cloudbreak.cloud.model.ClouderaManagerRepo;
import com.sequenceiq.cloudbreak.cmtemplate.CmTemplateProcessor;
import com.sequenceiq.cloudbreak.cmtemplate.configproviders.ConfigTestUtil;
import com.sequenceiq.cloudbreak.common.type.Versioned;
import com.sequenceiq.cloudbreak.template.TemplatePreparationObject;
import com.sequenceiq.cloudbreak.template.model.GeneralClusterConfigs;
import com.sequenceiq.cloudbreak.template.views.BlueprintView;

@ExtendWith(MockitoExtension.class)
public class EfmConfigProviderTest {

    private static final String ADMIN_GROUP = "adminGroup";

    private static final String ADMIN_USER = "adminUser";

    private static final String USER_CRN = "crn:cdp:cloudbreak:us-west-1:accountId-3:user:resource";

    private static final String CLUSTER_DEFINITION_CRN = "crn:cdp:cloudbreak:us-west-1:accountId-3:clusterdefinition:resource";

    @Mock
    private CmTemplateProcessor cmTemplate;

    @Mock
    private GrpcUmsClient umsClient;

    @Mock
    private VirtualGroupService virtualGroupService;

    @InjectMocks
    private EfmConfigProvider underTest;

    @Test
    public void testInitialAdminConfig7217() {
        User user = mock(User.class);
        when(user.getWorkloadUsername()).thenReturn(ADMIN_USER);
        when(umsClient.getUserDetails(eq(USER_CRN))).thenReturn(user);

        List<ApiClusterTemplateConfig> result = underTest.getRoleConfigs(
            EfmRoles.EFM_SERVER, cmTemplate, templatePreparationObject(CLOUDERA_STACK_VERSION_7_2_17, USER_CRN));

        Map<String, String> configToValue = ConfigTestUtil.getConfigNameToValueMap(result);

        verify(virtualGroupService, never()).createOrGetVirtualGroup(any(), any());
        assertThat(configToValue).containsOnly(entry(EFM_ADMIN_IDENTITIES, ADMIN_USER));
    }

    @Test
    public void testInitialAdminConfig7217NonUserCrn() {
        List<ApiClusterTemplateConfig> result = underTest.getRoleConfigs(
            EfmRoles.EFM_SERVER, cmTemplate, templatePreparationObject(CLOUDERA_STACK_VERSION_7_2_17, CLUSTER_DEFINITION_CRN));

        Map<String, String> configToValue = ConfigTestUtil.getConfigNameToValueMap(result);

        verify(virtualGroupService, never()).createOrGetVirtualGroup(any(), any());
        verify(umsClient, never()).getUserDetails(any());
        assertThat(configToValue).isEmpty();
    }

    @Test
    public void testInitialAdminConfig7218() {
        User user = mock(User.class);
        when(user.getWorkloadUsername()).thenReturn(ADMIN_USER);
        when(umsClient.getUserDetails(eq(USER_CRN))).thenReturn(user);

        when(virtualGroupService.createOrGetVirtualGroup(any(), eq(UmsVirtualGroupRight.EFM_ADMIN))).thenReturn(ADMIN_GROUP);

        List<ApiClusterTemplateConfig> result = underTest.getRoleConfigs(
            EfmRoles.EFM_SERVER, cmTemplate, templatePreparationObject(CLOUDERA_STACK_VERSION_7_2_18, USER_CRN));

        Map<String, String> configToValue = ConfigTestUtil.getConfigNameToValueMap(result);

        assertThat(configToValue).containsOnly(
            entry(EFM_ADMIN_GROUP_IDENTITIES, ADMIN_GROUP),
            entry(EFM_ADMIN_IDENTITIES, ADMIN_USER));
    }

    @Test
    public void testInitialAdminConfig7218NonUserCrn() {
        when(virtualGroupService.createOrGetVirtualGroup(any(), eq(UmsVirtualGroupRight.EFM_ADMIN))).thenReturn(ADMIN_GROUP);

        List<ApiClusterTemplateConfig> result = underTest.getRoleConfigs(
            EfmRoles.EFM_SERVER, cmTemplate, templatePreparationObject(CLOUDERA_STACK_VERSION_7_2_18, CLUSTER_DEFINITION_CRN));

        Map<String, String> configToValue = ConfigTestUtil.getConfigNameToValueMap(result);

        verify(umsClient, never()).getUserDetails(any());
        assertThat(configToValue).containsOnly(entry(EFM_ADMIN_GROUP_IDENTITIES, ADMIN_GROUP));
    }

    private TemplatePreparationObject templatePreparationObject(Versioned versioned, String workloadUserCrn) {
        GeneralClusterConfigs generalClusterConfigs = mock(GeneralClusterConfigs.class);
        when(generalClusterConfigs.getCreatorWorkloadUserCrn()).thenReturn(workloadUserCrn);

        return new TemplatePreparationObject.Builder()
            .withGeneralClusterConfigs(generalClusterConfigs)
            .withBlueprintView(getMockBlueprintView(versioned.getVersion()))
            .withProductDetails(generateCmRepo(versioned), null)
            .build();
    }

    private BlueprintView getMockBlueprintView(String version) {
        BlueprintView blueprintView = mock(BlueprintView.class);

        CmTemplateProcessor templateProcessor = mock(CmTemplateProcessor.class);
        when(templateProcessor.getStackVersion()).thenReturn(version);
        when(blueprintView.getProcessor()).thenReturn(templateProcessor);

        return blueprintView;
    }

    private ClouderaManagerRepo generateCmRepo(Versioned version) {
        return new ClouderaManagerRepo()
            .withBaseUrl("baseurl")
            .withGpgKeyUrl("gpgurl")
            .withPredefined(true)
            .withVersion(version.getVersion());
    }
}
