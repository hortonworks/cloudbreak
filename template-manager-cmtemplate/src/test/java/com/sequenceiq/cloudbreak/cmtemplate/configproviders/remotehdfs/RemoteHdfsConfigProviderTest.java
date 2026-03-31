package com.sequenceiq.cloudbreak.cmtemplate.configproviders.remotehdfs;

import static com.sequenceiq.cloudbreak.cmtemplate.configproviders.ConfigUtils.getSafetyValveProperty;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.cmtemplate.configproviders.hdfs.HdfsConfigHelper;
import com.sequenceiq.cloudbreak.dto.TrustView;
import com.sequenceiq.cloudbreak.template.TemplatePreparationObject;

@ExtendWith(MockitoExtension.class)
class RemoteHdfsConfigProviderTest {

    @Mock
    private HdfsConfigHelper hdfsConfigHelper;

    @InjectMocks
    private RemoteHdfsConfigProvider underTest;

    @Mock
    private TemplatePreparationObject source;

    @BeforeEach
    void setUp() {
    }

    @Test
    void populateRemoteHdfsPropertiesForStubDfs() {
        TrustView trustView = mock(TrustView.class);
        when(source.getTrustView()).thenReturn(Optional.of(trustView));
        when(trustView.realm()).thenReturn("realmX");

        when(source.getDatalakeView()).thenReturn(Optional.of(mock()));

        when(hdfsConfigHelper.getNameService(any())).thenReturn("ns1");
        when(hdfsConfigHelper.getNameServiceConfigSafetyValveValue(any())).thenReturn("<NameServiceConfigSafetyValveValue>");

        StringBuilder hdfsCoreSiteSafetyValveValue = new StringBuilder();
        underTest.populateRemoteHdfsPropertiesForStubDfs(source, hdfsCoreSiteSafetyValveValue);

        assertThat(hdfsCoreSiteSafetyValveValue.toString())
                .contains("<NameServiceConfigSafetyValveValue>")
                .contains(getSafetyValveProperty("dfs.namenode.kerberos.principal", "hdfs/_HOST@REALMX"))
                .contains(getSafetyValveProperty("dfs.nameservices", "ns1"));
    }

}
