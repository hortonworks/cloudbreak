package com.sequenceiq.freeipa.converter.instance;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.instance.InstanceLifeCycle;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.instance.InstanceMetaDataResponse;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.instance.InstanceMetadataType;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.instance.InstanceStatus;
import com.sequenceiq.freeipa.entity.InstanceGroup;
import com.sequenceiq.freeipa.entity.InstanceMetaData;

public class InstanceMetaDataToInstanceMetaDataResponseConverterTest {

    private static final String PRIVATE_IP = "10.1.2.3";

    private static final String PUBLIC_IP = "1.2.3.4";

    private static final Integer SSH_PORT = 5678;

    private static final String INSTANCE_ID = "instanceId";

    private static final String DISCOVERY_FQDN = "host.foo.org";

    private static final String INSTANCE_GROUP = "instanceGroup";

    private static final String SUBNET_ID = "subnetId";

    private static final String AVAILABILITY_ZONE = "availabilityZone";

    private static final InstanceStatus INSTANCE_STATUS = InstanceStatus.CREATED;

    private static final InstanceMetadataType INSTANCE_METADATA_TYPE = InstanceMetadataType.GATEWAY;

    private static final InstanceLifeCycle LIFE_CYCLE = InstanceLifeCycle.NORMAL;

    private InstanceMetaDataToInstanceMetaDataResponseConverter underTest;

    @BeforeEach
    void setUp() {
        underTest = new InstanceMetaDataToInstanceMetaDataResponseConverter();
    }

    static Object[][] convertTestDataProvider() {
        return new Object[][]{
                // testCaseName publicIp privateIp publicIpExpected
                {"publicIp=null, privateIp=null", null, null, null},
                {"publicIp=PUBLIC_IP, privateIp=null", PUBLIC_IP, null, PUBLIC_IP},
                {"publicIp=null, privateIp=PRIVATE_IP", null, PRIVATE_IP, "N/A"},
                {"publicIp=PUBLIC_IP, privateIp=PRIVATE_IP", PUBLIC_IP, PRIVATE_IP, PUBLIC_IP},
        };
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("convertTestDataProvider")
    void convertTest(String testCaseName, String publicIp, String privateIp, String publicIpExpected) {
        InstanceMetaData instanceMetaData = createInstanceMetaData(publicIp, privateIp);

        InstanceMetaDataResponse result = underTest.convert(instanceMetaData);

        assertThat(result).isNotNull();
        assertThat(result.getPrivateIp()).isEqualTo(privateIp);
        assertThat(result.getPublicIp()).isEqualTo(publicIpExpected);
        assertThat(result.getSshPort()).isEqualTo(SSH_PORT);
        assertThat(result.getInstanceId()).isEqualTo(INSTANCE_ID);
        assertThat(result.getDiscoveryFQDN()).isEqualTo(DISCOVERY_FQDN);
        assertThat(result.getInstanceGroup()).isEqualTo(INSTANCE_GROUP);
        assertThat(result.getSubnetId()).isEqualTo(SUBNET_ID);
        assertThat(result.getAvailabilityZone()).isEqualTo(AVAILABILITY_ZONE);
        assertThat(result.getInstanceStatus()).isEqualTo(INSTANCE_STATUS);
        assertThat(result.getInstanceType()).isEqualTo(INSTANCE_METADATA_TYPE);
        assertThat(result.getLifeCycle()).isEqualTo(LIFE_CYCLE);
    }

    private InstanceMetaData createInstanceMetaData(String publicIp, String privateIp) {
        InstanceGroup instanceGroup = new InstanceGroup();
        instanceGroup.setGroupName(INSTANCE_GROUP);

        InstanceMetaData result = new InstanceMetaData();
        result.setPrivateIp(privateIp);
        result.setPublicIp(publicIp);
        result.setSshPort(SSH_PORT);
        result.setInstanceId(INSTANCE_ID);
        result.setDiscoveryFQDN(DISCOVERY_FQDN);
        result.setInstanceGroup(instanceGroup);
        result.setSubnetId(SUBNET_ID);
        result.setAvailabilityZone(AVAILABILITY_ZONE);
        result.setInstanceStatus(INSTANCE_STATUS);
        result.setInstanceMetadataType(INSTANCE_METADATA_TYPE);
        result.setLifeCycle(LIFE_CYCLE);
        return result;
    }

}