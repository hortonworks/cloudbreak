package com.sequenceiq.cloudbreak.cmtemplate.configproviders;

import static com.sequenceiq.cloudbreak.TestUtil.rdsConfigWithoutCluster;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.cloudera.api.swagger.model.ApiClusterTemplateConfig;
import com.sequenceiq.cloudbreak.api.endpoint.v4.database.base.DatabaseType;
import com.sequenceiq.cloudbreak.cmtemplate.CmTemplateProcessor;
import com.sequenceiq.cloudbreak.domain.RdsSslMode;
import com.sequenceiq.cloudbreak.domain.view.RdsConfigWithoutCluster;
import com.sequenceiq.cloudbreak.template.TemplatePreparationObject;
import com.sequenceiq.cloudbreak.template.filesystem.TemplateCoreTestUtil;
import com.sequenceiq.cloudbreak.template.views.RdsView;

@ExtendWith(MockitoExtension.class)
class AbstractRdsRoleConfigProviderTest {

    private static final String SSL_CERTS_FILE_PATH = "/foo/bar.pem";

    private AbstractRdsRoleConfigProvider subject;

    @Mock
    private CmTemplateProcessor templateProcessor;

    @Mock
    private TemplatePreparationObject source;

    @BeforeEach
    void setup() {
        subject = new AbstractRdsRoleConfigProvider() {

            @Override
            public String dbUserKey() {
                return "user";
            }

            @Override
            public String dbPasswordKey() {
                return "pass";
            }

            @Override
            public DatabaseType dbType() {
                return DatabaseType.RANGER;
            }

            @Override
            protected List<ApiClusterTemplateConfig> getRoleConfigs(String roleType, CmTemplateProcessor templateProcessor, TemplatePreparationObject source) {
                return List.of();
            }

            @Override
            public String getServiceType() {
                return "service";
            }

            @Override
            public List<String> getRoleTypes() {
                return List.of("role1", "role2");
            }
        };
    }

    @Test
    void configurationNeededIfRdsConfigAndRoleBothPresent() {
        RdsConfigWithoutCluster rdsConfig = rdsConfigWithoutCluster(DatabaseType.RANGER, RdsSslMode.DISABLED);
        RdsView rdsView = TemplateCoreTestUtil.rdsViewProvider().getRdsView(rdsConfig, "AWS", false);
        when(source.getRdsView(DatabaseType.RANGER)).thenReturn(rdsView);
        when(templateProcessor.isRoleTypePresentInService(subject.getServiceType(), subject.getRoleTypes())).thenReturn(Boolean.TRUE);

        assertThat(subject.isConfigurationNeeded(templateProcessor, source)).isTrue();
    }

    @Test
    void configurationNotNeededIfRoleAbsent() {
        RdsConfigWithoutCluster rdsConfig = rdsConfigWithoutCluster(DatabaseType.RANGER, RdsSslMode.DISABLED);
        RdsView rdsView = TemplateCoreTestUtil.rdsViewProvider().getRdsView(rdsConfig, "AWS", false);
        when(source.getRdsView(DatabaseType.RANGER)).thenReturn(rdsView);
        when(templateProcessor.isRoleTypePresentInService(subject.getServiceType(), subject.getRoleTypes())).thenReturn(Boolean.FALSE);

        assertThat(subject.isConfigurationNeeded(templateProcessor, source)).isFalse();
    }

    @Test
    void configurationNotNeededIfRdsConfigAbsent() {
        when(source.getRdsView(DatabaseType.RANGER)).thenReturn(null);

        assertThat(subject.isConfigurationNeeded(templateProcessor, source)).isFalse();
    }

    @Test
    void getRdsConfigTestWhenRdsConfigPresent() {
        RdsConfigWithoutCluster rdsConfig = rdsConfigWithoutCluster(DatabaseType.RANGER, RdsSslMode.DISABLED);
        RdsView rdsView = TemplateCoreTestUtil.rdsViewProvider().getRdsView(rdsConfig, "AWS", false);
        when(source.getRdsView(DatabaseType.RANGER)).thenReturn(rdsView);

        assertThat(subject.getRdsView(source)).isSameAs(rdsView);
    }

    @Test
    void getRdsConfigTestWhenRdsConfigAbsent() {
        when(source.getRdsView(DatabaseType.RANGER)).thenReturn(null);

        assertThat(subject.getRdsView(source)).isNull();
    }

    @Test
    void getRdsViewTest() {
        RdsConfigWithoutCluster rdsConfig = rdsConfigWithoutCluster(DatabaseType.RANGER, RdsSslMode.DISABLED);
        RdsView rdsView = TemplateCoreTestUtil.rdsViewProvider().getRdsView(rdsConfig, "AWS", false);
        when(source.getRdsView(DatabaseType.RANGER)).thenReturn(rdsView);
        when(source.getRdsSslCertificateFilePath()).thenReturn(SSL_CERTS_FILE_PATH);

        RdsView rdsViewTest = subject.getRdsView(source);

        assertThat(rdsViewTest).isNotNull();
        assertThat(rdsViewTest.getSslCertificateFilePath()).isEqualTo(SSL_CERTS_FILE_PATH);
    }

}
