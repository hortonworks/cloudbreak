package com.sequenceiq.cloudbreak.cmtemplate.configproviders.kafka;

import com.google.common.base.Splitter;
import com.sequenceiq.cloudbreak.cloud.model.ClouderaManagerProduct;
import com.sequenceiq.cloudbreak.cloud.model.ClouderaManagerRepo;
import com.sequenceiq.cloudbreak.cmtemplate.CmTemplateProcessor;
import com.sequenceiq.cloudbreak.template.TemplatePreparationObject;
import com.sequenceiq.cloudbreak.template.views.BlueprintView;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

import static com.sequenceiq.cloudbreak.cmtemplate.configproviders.kafka.KafkaConfigProviderUtils.CdhVersionForStreaming.VERSION_7_0_2_CANNOT_DETERMINE_PATCH;
import static com.sequenceiq.cloudbreak.cmtemplate.configproviders.kafka.KafkaConfigProviderUtils.CdhVersionForStreaming.VERSION_7_0_2;
import static com.sequenceiq.cloudbreak.cmtemplate.configproviders.kafka.KafkaConfigProviderUtils.CdhVersionForStreaming.VERSION_7_0_2_2_OR_LATER;
import static com.sequenceiq.cloudbreak.cmtemplate.configproviders.kafka.KafkaConfigProviderUtils.CdhVersionForStreaming.VERSION_PRE_7_0_2;
import static java.util.stream.Collectors.toList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class KafkaConfigProviderUtilsTest {

    @Mock
    private BlueprintView blueprintView;

    @Mock
    private CmTemplateProcessor processor;

    @Test
    void getCdhPatchVersionMissingProductDetails() {
        TemplatePreparationObject source = new TemplatePreparationObject.Builder().build();
        assertThat(KafkaConfigProviderUtils.getCdhPatchVersion(source)).isEmpty();
    }

    @Test
    void getCdhPatchVersionMissingCdhProduct() {
        TemplatePreparationObject source = new TemplatePreparationObject.Builder()
                .withProductDetails(cmRepo(), products("SCHEMAREGISTRY=0.8.0.3.0.0.0-97"))
                .build();
        assertThat(KafkaConfigProviderUtils.getCdhPatchVersion(source)).isEmpty();
    }

    @Test
    void getCdhPatchVersionIrregularVersionNumberFormat() {
        TemplatePreparationObject source = new TemplatePreparationObject.Builder()
                .withProductDetails(cmRepo(), products("SCHEMAREGISTRY=0.8.0.3.0.0.0-97,CDH=123456"))
                .build();
        assertThat(KafkaConfigProviderUtils.getCdhPatchVersion(source)).isEmpty();
    }

    @Test
    void getCdhPatchVersion() {
        TemplatePreparationObject source = new TemplatePreparationObject.Builder()
                .withProductDetails(cmRepo(), products("SCHEMAREGISTRY=0.8.0.3.0.0.0-97,CDH=7.0.2-1.cdh7.0.2.p99.1672317"))
                .build();
        assertThat(KafkaConfigProviderUtils.getCdhPatchVersion(source)).isEqualTo(Optional.of(99));
    }

    @ParameterizedTest
    @MethodSource("testArgs")
    void getCdhVersionForStreaming(String cdhVersion, String cdhParcelVersion, KafkaConfigProviderUtils.CdhVersionForStreaming expectedResult) {
        cdhMainVersionIs(cdhVersion);
        TemplatePreparationObject source = templatePreparationObject(cdhParcelVersion);
        assertThat(KafkaConfigProviderUtils.getCdhVersionForStreaming(source)).isEqualTo(expectedResult);
    }

    static Stream<Arguments> testArgs() {
        return Stream.of(
                Arguments.of("7.0.1", cdhParcelVersion("7.0.1", 5), VERSION_PRE_7_0_2),
                Arguments.of("7.0.1", null, VERSION_PRE_7_0_2),
                Arguments.of("7.0.1", "irregular-12345", VERSION_PRE_7_0_2),
                Arguments.of("7.0.2", cdhParcelVersion("7.0.2", 0), VERSION_7_0_2),
                Arguments.of("7.0.2", cdhParcelVersion("7.0.2", 1), VERSION_7_0_2),
                Arguments.of("7.0.2", cdhParcelVersion("7.0.2", 1), VERSION_7_0_2),
                Arguments.of("7.0.2", cdhParcelVersion("7.0.2", 2), VERSION_7_0_2_2_OR_LATER),
                Arguments.of("7.0.2", cdhParcelVersion("7.0.2", 3), VERSION_7_0_2_2_OR_LATER),
                Arguments.of("7.0.2", null, VERSION_7_0_2_CANNOT_DETERMINE_PATCH),
                Arguments.of("7.0.2", "irregular-12345", VERSION_7_0_2_CANNOT_DETERMINE_PATCH),
                Arguments.of("7.1.0", cdhParcelVersion("7.1.0", 0), VERSION_7_0_2_2_OR_LATER),
                Arguments.of("7.1.0", null, VERSION_7_0_2_2_OR_LATER),
                Arguments.of("7.1.0", "irregular-12345", VERSION_7_0_2_2_OR_LATER)
        );
    }

    private TemplatePreparationObject templatePreparationObject(String cdhParcelVersion) {
        TemplatePreparationObject.Builder builder = new TemplatePreparationObject.Builder();
        builder.withBlueprintView(blueprintView);
        if (null != cdhParcelVersion) {
            builder.withProductDetails(cmRepo(), products("CDH=" + cdhParcelVersion));
        }
        return builder.build();
    }

    static String cdhParcelVersion(String mainVersion, Integer patchVersion) {
        return mainVersion + "-1.cdh" + mainVersion + ".p" + patchVersion + ".9999999";
    }

    static List<ClouderaManagerProduct> products(String productData) {
        Map<String, String> products = Splitter.on(",").withKeyValueSeparator("=").split(productData);
        return products.entrySet().stream()
                .map(kv -> new ClouderaManagerProduct().withName(kv.getKey()).withVersion(kv.getValue()))
                .collect(toList());
    }

    private static ClouderaManagerRepo cmRepo() {
        return new ClouderaManagerRepo();
    }

    private void cdhMainVersionIs(String version) {
        when(blueprintView.getProcessor()).thenReturn(processor);
        when(processor.getVersion()).thenReturn(Optional.ofNullable(version));
    }

}