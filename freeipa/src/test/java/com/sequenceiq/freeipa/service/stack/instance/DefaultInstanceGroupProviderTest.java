package com.sequenceiq.freeipa.service.stack.instance;

import static com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider.INTERNAL_ACTOR_CRN;
import static com.sequenceiq.freeipa.service.stack.instance.DefaultInstanceGroupProvider.ATTRIBUTE_VOLUME_ENCRYPTED;
import static com.sequenceiq.freeipa.service.stack.instance.DefaultInstanceGroupProvider.ATTRIBUTE_VOLUME_ENCRYPTION_TYPE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.cloudbreak.common.converter.MissingResourceNameGenerator;
import com.sequenceiq.cloudbreak.common.json.Json;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.common.api.type.EncryptionType;
import com.sequenceiq.freeipa.entity.Template;
import com.sequenceiq.freeipa.service.DefaultRootVolumeSizeProvider;

@ExtendWith(MockitoExtension.class)
class DefaultInstanceGroupProviderTest {

    private static final String ACCOUNT_ID = "accountId";

    @Mock
    private MissingResourceNameGenerator missingResourceNameGenerator;

    @Mock
    private DefaultRootVolumeSizeProvider defaultRootVolumeSizeProvider;

    @Mock
    private DefaultInstanceTypeProvider defaultInstanceTypeProvider;

    @Mock
    private EntitlementService entitlementService;

    @InjectMocks
    private DefaultInstanceGroupProvider underTest;

    @Test
    void createDefaultTemplateTestNoVolumeEncryptionWhenAzure() {
        Template result = underTest.createDefaultTemplate(CloudPlatform.AZURE, ACCOUNT_ID);

        assertThat(result).isNotNull();
        assertThat(result.getAttributes()).isNull();
    }

    @Test
    void createDefaultTemplateTestNoVolumeEncryptionWhenAwsAndNotEntitled() {
        when(entitlementService.freeIpaDlEbsEncryptionEnabled(INTERNAL_ACTOR_CRN, ACCOUNT_ID)).thenReturn(false);

        Template result = underTest.createDefaultTemplate(CloudPlatform.AWS, ACCOUNT_ID);

        assertThat(result).isNotNull();
        assertThat(result.getAttributes()).isNull();
    }

    @Test
    void createDefaultTemplateTestVolumeEncryptionAddedWhenAwsAndEntitled() {
        when(entitlementService.freeIpaDlEbsEncryptionEnabled(INTERNAL_ACTOR_CRN, ACCOUNT_ID)).thenReturn(true);

        Template result = underTest.createDefaultTemplate(CloudPlatform.AWS, ACCOUNT_ID);

        assertThat(result).isNotNull();
        Json attributes = result.getAttributes();
        assertThat(attributes).isNotNull();
        assertThat(attributes.<Object>getValue(ATTRIBUTE_VOLUME_ENCRYPTED)).isEqualTo(Boolean.TRUE);
        assertThat(attributes.<Object>getValue(ATTRIBUTE_VOLUME_ENCRYPTION_TYPE)).isEqualTo(EncryptionType.DEFAULT.name());
    }

}