package com.sequenceiq.it.cloudbreak.newway.cloud;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.parameter.network.AwsNetworkV4Parameters;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.cluster.ambari.AmbariV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.network.NetworkV4Request;
import com.sequenceiq.it.cloudbreak.newway.TestParameter;
import com.sequenceiq.it.cloudbreak.newway.entity.kerberos.KerberosTestDto;

public class AwsKerberos {
    public static final String KERBEROS_CLOUDY = "kerberos-cloudy";

    public static final String USERNAME = "cloudy";

    public static final String PRINCIPAL = USERNAME + "@DEV.DPS.SITE";

    public static final String DEV_DPS_SITE = "dev.dps.site";

    public static final String LDAP_URL = "ldaps://" + DEV_DPS_SITE;

    public static final String DEV_DPS_SITE1 = "DEV.DPS.SITE";

    public static final String CONTAINER_DN = "integrationtest.activedirectory.container";

    public static final boolean TCP_ALLOWED = true;

    public static final boolean VERIFY_KDC_TRUST = false;

    public static final String AD_VPC = "integrationtest.activedirectory.vpc";

    public static final String AD_SUBNET = "integrationtest.activedirectory.subnet";

    public static final String AD_PASSWORD = "integrationtest.activedirectory.password";

    private AwsKerberos() {
    }

    public static NetworkV4Request getNetworkV2RequestForKerberosAws(TestParameter testParameter) {
        NetworkV4Request network = new NetworkV4Request();
        AwsNetworkV4Parameters params = new AwsNetworkV4Parameters();
        params.setVpcId(testParameter.get(AD_VPC));
        params.setSubnetId(testParameter.get(AD_SUBNET));
        network.setAws(params);
        return network;
    }

    public static AmbariV4Request getAmbariV2Request(CloudProvider cloudProvider, String clusterDefinitionName, TestParameter testParameter) {
        AmbariV4Request ambariRequest = cloudProvider.ambariRequestWithBlueprintName(clusterDefinitionName);
        ambariRequest.setUserName(USERNAME);
        ambariRequest.setPassword(testParameter.get(AD_PASSWORD));
        return ambariRequest;
    }

    public static KerberosTestDto kerberosOnAws(TestParameter testParameter) {
        return new KerberosTestDto(null);
    }
}
