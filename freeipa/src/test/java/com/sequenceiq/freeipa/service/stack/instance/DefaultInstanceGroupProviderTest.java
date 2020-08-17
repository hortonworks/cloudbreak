package com.sequenceiq.freeipa.service.stack.instance;

import static com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider.INTERNAL_ACTOR_CRN;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.cloudbreak.cloud.model.InstanceTemplate;
import com.sequenceiq.cloudbreak.cloud.model.instance.AwsInstanceTemplate;
import com.sequenceiq.cloudbreak.cloud.model.instance.AzureInstanceGroupParameters;
import com.sequenceiq.cloudbreak.common.converter.MissingResourceNameGenerator;
import com.sequenceiq.cloudbreak.common.json.Json;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.common.api.type.EncryptionType;
import com.sequenceiq.freeipa.entity.Template;
import com.sequenceiq.freeipa.service.DefaultRootVolumeSizeProvider;

import net.sf.json.JSONObject;

@ExtendWith(MockitoExtension.class)
class DefaultInstanceGroupProviderTest {

    private static final String ACCOUNT_ID = "accountId";

    private static final String STACK_NM = "stack";

    private static final String IG_NAME = "instanceGroup";

    private static final String AVAILABILITY_SET = "availabilitySet";

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
        assertThat(attributes.<Object>getValue(AwsInstanceTemplate.EBS_ENCRYPTION_ENABLED)).isEqualTo(Boolean.TRUE);
        assertThat(attributes.<Object>getValue(AwsInstanceTemplate.FAST_EBS_ENCRYPTION_ENABLED)).isEqualTo(Boolean.FALSE);
        assertThat(attributes.<Object>getValue(InstanceTemplate.VOLUME_ENCRYPTION_KEY_TYPE)).isEqualTo(EncryptionType.DEFAULT.name());
    }

    @Test
    void createDefaultTemplateTestVolumeEncryptionAddedWhenAwsAndEntitledAndFastEncryption() {
        when(entitlementService.freeIpaDlEbsEncryptionEnabled(INTERNAL_ACTOR_CRN, ACCOUNT_ID)).thenReturn(true);
        when(entitlementService.fastEbsEncryptionEnabled(INTERNAL_ACTOR_CRN, ACCOUNT_ID)).thenReturn(true);

        Template result = underTest.createDefaultTemplate(CloudPlatform.AWS, ACCOUNT_ID);

        assertThat(result).isNotNull();
        Json attributes = result.getAttributes();
        assertThat(attributes).isNotNull();
        assertThat(attributes.<Object>getValue(AwsInstanceTemplate.EBS_ENCRYPTION_ENABLED)).isEqualTo(Boolean.TRUE);
        assertThat(attributes.<Object>getValue(AwsInstanceTemplate.FAST_EBS_ENCRYPTION_ENABLED)).isEqualTo(Boolean.TRUE);
        assertThat(attributes.<Object>getValue(InstanceTemplate.VOLUME_ENCRYPTION_KEY_TYPE)).isEqualTo(EncryptionType.DEFAULT.name());
    }

    @Test
    void createInstanceGroupAttributesTestAvailabilitySetsAddedWhenAzure() {
        Json attributes = underTest.createAttributes(CloudPlatform.AZURE, STACK_NM, IG_NAME);

        assertThat(attributes).isNotNull();
        JSONObject availabilitySet = attributes.getValue(AVAILABILITY_SET);

        assertThat(availabilitySet.getInt(AzureInstanceGroupParameters.FAULT_DOMAIN_COUNT)).isEqualTo(2);
        assertThat(availabilitySet.getInt(AzureInstanceGroupParameters.UPDATE_DOMAIN_COUNT)).isEqualTo(20);
        assertThat(availabilitySet.getString(AzureInstanceGroupParameters.NAME)).isEqualTo(String.format("%s-%s-as", STACK_NM, IG_NAME));
    }

    @Test
    void createInstanceGroupAttributesTestEmptyWhenAws() {
        Json attributes = underTest.createAttributes(CloudPlatform.AWS, STACK_NM, IG_NAME);

        assertThat(attributes).isNull();
    }

}