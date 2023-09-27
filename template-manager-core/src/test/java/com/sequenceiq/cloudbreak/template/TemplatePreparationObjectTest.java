package com.sequenceiq.cloudbreak.template;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.StackType;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.cloudbreak.domain.stack.cluster.host.HostGroup;

class TemplatePreparationObjectTest {

    private static final String SSL_CERTS_FILE_PATH = "/foo/bar.pem";

    private static final String TEST_PLATFORM_VARIANT = "AWS_VARIANT";

    @Test
    void getRdsSslCertificateFilePathTestWhenFilePathAbsent() {
        TemplatePreparationObject tpo = new TemplatePreparationObject.Builder()
                .build();

        assertThat(tpo.getRdsSslCertificateFilePath()).isNull();
    }

    @Test
    void getRdsSslCertificateFilePathTestWhenFilePathPresent() {
        TemplatePreparationObject tpo = new TemplatePreparationObject.Builder()
                .withRdsSslCertificateFilePath(SSL_CERTS_FILE_PATH)
                .build();

        assertThat(tpo.getRdsSslCertificateFilePath()).isEqualTo(SSL_CERTS_FILE_PATH);
    }

    @Test
    void getPlatformVariantTest() {
        TemplatePreparationObject tpo = new TemplatePreparationObject.Builder()
                .withPlatformVariant(TEST_PLATFORM_VARIANT)
                .build();

        assertThat(tpo.getPlatformVariant()).isEqualTo(TEST_PLATFORM_VARIANT);
    }

    @Test
    void testBuilderWithCloudPlatform() {
        CloudPlatform cloudPlatform = CloudPlatform.AWS;
        TemplatePreparationObject.Builder builder = TemplatePreparationObject.Builder.builder();

        TemplatePreparationObject result = builder.withCloudPlatform(cloudPlatform).build();

        assertNotNull(result);
        assertEquals(cloudPlatform, result.getCloudPlatform());
    }

    @Test
    void testBuilderWithHostgroups() {
        Set<HostGroup> hostGroups = new HashSet<>();
        HostGroup hostGroup1 = new HostGroup();
        hostGroup1.setName("group1");
        hostGroups.add(hostGroup1);
        HostGroup hostGroup2 = new HostGroup();
        hostGroup2.setName("group2");
        hostGroups.add(hostGroup2);
        Set<String> ephemeralVolumeWhichMustBeProvisioned = new HashSet<>();
        ephemeralVolumeWhichMustBeProvisioned.add("ephemeral1");
        ephemeralVolumeWhichMustBeProvisioned.add("ephemeral2");

        TemplatePreparationObject.Builder builder = TemplatePreparationObject.Builder.builder();

        TemplatePreparationObject result = builder.withHostgroups(hostGroups, ephemeralVolumeWhichMustBeProvisioned).build();

        assertNotNull(result);
        assertEquals(hostGroups.size(), result.getHostgroupViews().size());
        assertTrue(result.getHostgroupViews().stream().anyMatch(view -> view.getName().equals("group1")));
        assertTrue(result.getHostgroupViews().stream().anyMatch(view -> view.getName().equals("group2")));
    }

    @Test
    void testBuilderWithCustomInputs() {
        Map<String, Object> customInputs = new HashMap<>();
        customInputs.put("key1", "value1");
        customInputs.put("key2", "value2");
        TemplatePreparationObject.Builder builder = TemplatePreparationObject.Builder.builder();

        TemplatePreparationObject result = builder.withCustomInputs(customInputs).build();

        assertNotNull(result);
        assertEquals(customInputs, result.getCustomInputs());
    }

    @Test
    void testBuilderWithStackType() {
        StackType stackType = StackType.WORKLOAD;
        TemplatePreparationObject.Builder builder = TemplatePreparationObject.Builder.builder();

        TemplatePreparationObject result = builder.withStackType(stackType).build();

        assertNotNull(result);
        assertEquals(stackType, result.getStackType());
    }

}