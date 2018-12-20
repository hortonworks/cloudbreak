package com.sequenceiq.it.cloudbreak.newway.cloud;

import java.util.Map;

import com.sequenceiq.cloudbreak.api.model.kerberos.ActiveDirectoryKerberosDescriptor;
import com.sequenceiq.cloudbreak.api.model.kerberos.KerberosRequest;
import com.sequenceiq.cloudbreak.api.model.v2.AmbariV2Request;
import com.sequenceiq.cloudbreak.api.model.v2.NetworkV2Request;
import com.sequenceiq.it.cloudbreak.newway.Kerberos;
import com.sequenceiq.it.cloudbreak.newway.KerberosEntity;
import com.sequenceiq.it.cloudbreak.newway.TestParameter;

public class AwsKerberos {
    public static final String KERBEROS_CLOUDY = "kerberos-cloudy";

    public static final String PASSWORD = "DpsTestAdmin18!";

    public static final String USERNAME = "cloudy";

    public static final String PRINCIPAL = USERNAME + "@DEV.DPS.SITE";

    public static final String DEV_DPS_SITE = "dev.dps.site";

    public static final String LDAP_URL = "ldaps://" + DEV_DPS_SITE;

    public static final String DEV_DPS_SITE1 = "DEV.DPS.SITE";

    public static final String CONTAINER_DN = "OU=HDPKerb1,OU=DEV,DC=DEV,DC=DPS,DC=SITE";

    public static final boolean TCP_ALLOWED = true;

    public static final boolean VERIFY_KDC_TRUST = false;

    public static final String VPC = "vpc-061a56128550745bf";

    public static final String SUBNET = "subnet-028163b035f712628";

    public static final String AD_PASSWORD = "integrationtest.activedirectory.password";

    private AwsKerberos() {
    }

    public static NetworkV2Request getNetworkV2RequestForKerberosAws() {
        NetworkV2Request network = new NetworkV2Request();
        network.setParameters(Map.of("vpcId", VPC,
                "subnetId", SUBNET));
        return network;
    }

    public static AmbariV2Request getAmbariV2RequestForAwsKerberos(CloudProvider cloudProvider, String blueprintName, TestParameter testParameter) {
        AmbariV2Request ambariRequest = cloudProvider.ambariRequestWithBlueprintName(blueprintName);
        ambariRequest.setUserName(USERNAME);
        ambariRequest.setPassword(testParameter.get(AD_PASSWORD));
        ambariRequest.setKerberosConfigName(KERBEROS_CLOUDY);
        return ambariRequest;
    }

    public static KerberosEntity kerberosOnAws() {
        return Kerberos.isCreated()
                .withRequest(getKerberosRequest()).withName(KERBEROS_CLOUDY);
    }

    private static KerberosRequest getKerberosRequest() {
        KerberosRequest kerberosRequest = new KerberosRequest();
        ActiveDirectoryKerberosDescriptor activeDirectoryKerberosDescriptor = new ActiveDirectoryKerberosDescriptor();
        activeDirectoryKerberosDescriptor.setUrl(DEV_DPS_SITE);
        activeDirectoryKerberosDescriptor.setAdminUrl(DEV_DPS_SITE);
        activeDirectoryKerberosDescriptor.setRealm(DEV_DPS_SITE1);
        activeDirectoryKerberosDescriptor.setLdapUrl(LDAP_URL);
        activeDirectoryKerberosDescriptor.setContainerDn(CONTAINER_DN);
        activeDirectoryKerberosDescriptor.setPassword(PASSWORD);
        activeDirectoryKerberosDescriptor.setPrincipal(PRINCIPAL);
        activeDirectoryKerberosDescriptor.setDomain(DEV_DPS_SITE);
        activeDirectoryKerberosDescriptor.setTcpAllowed(TCP_ALLOWED);
        activeDirectoryKerberosDescriptor.setVerifyKdcTrust(VERIFY_KDC_TRUST);
        kerberosRequest.setActiveDirectory(activeDirectoryKerberosDescriptor);
        kerberosRequest.setName(KERBEROS_CLOUDY);
        return kerberosRequest;
    }
}
