package com.sequenceiq.freeipa.service.stack.instance;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.cloud.model.InstanceTemplate;
import com.sequenceiq.cloudbreak.cloud.model.instance.AwsInstanceTemplate;
import com.sequenceiq.cloudbreak.cloud.model.instance.AzureInstanceGroupParameters;
import com.sequenceiq.cloudbreak.cloud.model.instance.AzureInstanceTemplate;
import com.sequenceiq.cloudbreak.common.converter.MissingResourceNameGenerator;
import com.sequenceiq.cloudbreak.common.json.Json;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.cloudbreak.common.network.NetworkConstants;
import com.sequenceiq.common.api.type.EncryptionType;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.network.AwsNetworkParameters;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.network.NetworkRequest;
import com.sequenceiq.freeipa.entity.InstanceGroupNetwork;
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

    @InjectMocks
    private DefaultInstanceGroupProvider underTest;

    @Test
    void createDefaultTemplateTestNoVolumeEncryptionWhenAzure() {
        Template result = underTest.createDefaultTemplate(CloudPlatform.AZURE, ACCOUNT_ID, null);

        assertThat(result).isNotNull();
        assertThat(result.getAttributes()).isNull();
    }

    @Test
    void createDefaultTemplateTestVolumeEncryptionAddedWhenAzure() {
        Template result = underTest.createDefaultTemplate(CloudPlatform.AZURE, ACCOUNT_ID, "dummyDiskEncryptionSet");

        assertThat(result).isNotNull();
        Json attributes = result.getAttributes();
        assertThat(attributes).isNotNull();
        assertThat(attributes.<Object>getValue(AzureInstanceTemplate.DISK_ENCRYPTION_SET_ID)).isEqualTo("dummyDiskEncryptionSet");
        assertThat(attributes.<Object>getValue(AzureInstanceTemplate.MANAGED_DISK_ENCRYPTION_WITH_CUSTOM_KEY_ENABLED)).isEqualTo(Boolean.TRUE);
    }

    @Test
    void createDefaultTemplateTestVolumeEncryptionAddedWhenAws() {

        Template result = underTest.createDefaultTemplate(CloudPlatform.AWS, ACCOUNT_ID, null);

        assertThat(result).isNotNull();
        Json attributes = result.getAttributes();
        assertThat(attributes).isNotNull();
        assertThat(attributes.<Object>getValue(AwsInstanceTemplate.EBS_ENCRYPTION_ENABLED)).isEqualTo(Boolean.TRUE);
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

    @Test
    void createDefaultNetworkWithAwsAttributesShouldReturnWithNetworkAttributes() {
        Json json = new Json(Map.of(NetworkConstants.SUBNET_IDS, Set.of("id")));
        NetworkRequest network = new NetworkRequest();
        AwsNetworkParameters awsNetworkParameters = new AwsNetworkParameters();
        awsNetworkParameters.setSubnetId("id");
        network.setAws(awsNetworkParameters);
        InstanceGroupNetwork defaultNetwork = underTest.createDefaultNetwork(CloudPlatform.AWS, network);

        assertThat(defaultNetwork.getAttributes()).isEqualTo(json);
    }

}