package com.sequenceiq.it.cloudbreak.newway.cloud;

import java.util.Map;

import com.sequenceiq.cloudbreak.api.endpoint.v4.kerberos.requests.ActiveDirectoryKerberosDescriptor;
import com.sequenceiq.cloudbreak.api.endpoint.v4.kerberos.requests.KerberosV4Request;
import com.sequenceiq.cloudbreak.api.model.v2.AmbariV2Request;
import com.sequenceiq.cloudbreak.api.model.v2.NetworkV2Request;
import com.sequenceiq.it.cloudbreak.newway.Kerberos;
import com.sequenceiq.it.cloudbreak.newway.KerberosEntity;
import com.sequenceiq.it.cloudbreak.newway.TestParameter;

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

    public static NetworkV2Request getNetworkV2RequestForKerberosAws(TestParameter testParameter) {
        NetworkV2Request network = new NetworkV2Request();
        network.setParameters(Map.of("vpcId", testParameter.get(AD_VPC),
                "subnetId", testParameter.get(AD_SUBNET)));
        return network;
    }

    public static AmbariV2Request getAmbariV2RequestForAwsKerberos(CloudProvider cloudProvider, String blueprintName, TestParameter testParameter) {
        AmbariV2Request ambariRequest = cloudProvider.ambariRequestWithBlueprintName(blueprintName);
        ambariRequest.setUserName(USERNAME);
        ambariRequest.setPassword(testParameter.get(AD_PASSWORD));
        ambariRequest.setKerberosConfigName(KERBEROS_CLOUDY);
        return ambariRequest;
    }

    public static KerberosEntity kerberosOnAws(TestParameter testParameter) {
        return Kerberos.isCreated()
                .withRequest(getKerberosRequest(testParameter)).withName(KERBEROS_CLOUDY);
    }

    private static KerberosV4Request getKerberosRequest(TestParameter testParameter) {
        KerberosV4Request kerberosV4Request = new KerberosV4Request();
        ActiveDirectoryKerberosDescriptor activeDirectoryKerberosDescriptor = new ActiveDirectoryKerberosDescriptor();
        activeDirectoryKerberosDescriptor.setUrl(DEV_DPS_SITE);
        activeDirectoryKerberosDescriptor.setAdminUrl(DEV_DPS_SITE);
        activeDirectoryKerberosDescriptor.setRealm(DEV_DPS_SITE1);
        activeDirectoryKerberosDescriptor.setLdapUrl(LDAP_URL);
        activeDirectoryKerberosDescriptor.setContainerDn(testParameter.get(CONTAINER_DN));
        activeDirectoryKerberosDescriptor.setPassword(testParameter.get(AD_PASSWORD));
        activeDirectoryKerberosDescriptor.setPrincipal(PRINCIPAL);
        activeDirectoryKerberosDescriptor.setDomain(DEV_DPS_SITE);
        activeDirectoryKerberosDescriptor.setTcpAllowed(TCP_ALLOWED);
        activeDirectoryKerberosDescriptor.setVerifyKdcTrust(VERIFY_KDC_TRUST);
        kerberosV4Request.setActiveDirectory(activeDirectoryKerberosDescriptor);
        kerberosV4Request.setName(KERBEROS_CLOUDY);
        return kerberosV4Request;
    }
}
