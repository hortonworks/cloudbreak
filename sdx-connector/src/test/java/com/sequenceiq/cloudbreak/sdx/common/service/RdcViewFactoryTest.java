package com.sequenceiq.cloudbreak.sdx.common.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.sdx.RdcView;
import com.sequenceiq.cloudbreak.util.FileReaderUtils;

@ExtendWith(MockitoExtension.class)
class RdcViewFactoryTest {

    private static final String SDX_CRN = "crn:cdp:datalake:us-west-1:default:datalake:d3b8df82-878d-4395-94b1-2e355217446d";

    @InjectMocks
    private RdcViewFactory underTest;

    @Test
    void createWithoutRdc() {
        RdcView rdcView = underTest.create(SDX_CRN, Optional.empty());

        assertThat(rdcView.getStackCrn()).isEqualTo(SDX_CRN);
        assertThat(rdcView.getRemoteDataContext()).isEmpty();
        assertEmptyConfigs(rdcView);
    }

    @Test
    void createFromEmptyRdc() {
        String rdc = "{}";
        RdcView rdcView = underTest.create(SDX_CRN, Optional.of(rdc));

        assertThat(rdcView.getStackCrn()).isEqualTo(SDX_CRN);
        assertThat(rdcView.getRemoteDataContext()).hasValue(rdc);
        assertEmptyConfigs(rdcView);
    }

    @Test
    void createFromValidRdc() throws IOException {
        String rdc = FileReaderUtils.readFileFromClasspath("com/sequenceiq/cloudbreak/sdx/common/service/rdc.json");
        RdcView rdcView = underTest.create(SDX_CRN, Optional.of(rdc));

        assertThat(rdcView.getStackCrn()).isEqualTo(SDX_CRN);
        assertThat(rdcView.getRemoteDataContext()).hasValue(rdc);
        assertThat(rdcView.getEndpoints("ZOOKEEPER", "SERVER")).containsAll(List.of(
                "https://pub-ak-aws3-dl-master0.pub-ak-a.xcu2-8y8x.wl.cloudera.site:2182",
                "https://pub-ak-aws3-dl-auxiliary0.pub-ak-a.xcu2-8y8x.wl.cloudera.site:2182"
        ));
        assertThat(rdcView.getServiceConfigs("KAFKA"))
                .containsExactlyInAnyOrderEntriesOf(Map.of(
                        "ranger_service", "ranger",
                        "zookeeper_service", "zookeeper",
                        "kerberos.auth.enable", "true"
                ));
        assertThat(rdcView.getServiceConfig("SOLR", "zookeeper_znode")).isEqualTo("/solr-infra");
        assertThat(rdcView.getRoleConfigs("any", "any")).isEmpty();
    }

    private static void assertEmptyConfigs(RdcView rdcView) {
        assertThat(rdcView.getEndpoints("any", "any")).isEmpty();
        assertThat(rdcView.getServiceConfigs("any")).isEmpty();
        assertThat(rdcView.getServiceConfig("any", "any")).isNull();
        assertThat(rdcView.getRoleConfigs("any", "any")).isEmpty();
    }
}
