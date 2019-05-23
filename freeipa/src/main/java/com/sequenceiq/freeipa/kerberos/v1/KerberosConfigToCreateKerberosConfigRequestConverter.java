package com.sequenceiq.freeipa.kerberos.v1;

import org.apache.logging.log4j.util.Strings;
import org.springframework.stereotype.Component;

import com.sequenceiq.freeipa.api.v1.kerberos.model.create.ActiveDirectoryKerberosDescriptor;
import com.sequenceiq.freeipa.api.v1.kerberos.model.create.CreateKerberosConfigRequest;
import com.sequenceiq.freeipa.api.v1.kerberos.model.create.FreeIPAKerberosDescriptor;
import com.sequenceiq.freeipa.api.v1.kerberos.model.create.KerberosDescriptorBase;
import com.sequenceiq.freeipa.api.v1.kerberos.model.create.MITKerberosDescriptor;
import com.sequenceiq.freeipa.kerberos.KerberosConfig;

@Component
public class KerberosConfigToCreateKerberosConfigRequestConverter {

    private static final String EMPTY_JSON = "{}";

    private static final String EMPTY_DESCRIPTOR_JSON = "{\"kerberos-env\":{\"properties\":{\"kdc_type\":\"kdc-type\",\"kdc_hosts\":\"kdc-host-value\","
            + "\"admin_server_host\":\"admin-server-host-value\",\"realm\":\"realm-value\"}}}";

    private static final String FAKE_REALM_POSTFIX = "realm";

    private static final String FAKE_ADMIN_POSTFIX = "admin";

    private static final String FAKE_PRINCIPAL_POSTFIX = "principal";

    private static final String FAKE_PASSWORD_POSTFIX = "password";

    public CreateKerberosConfigRequest convert(KerberosConfig source) {
        CreateKerberosConfigRequest request = new CreateKerberosConfigRequest();
        request.setName(source.getName());
        request.setDescription(source.getDescription());
        request.setEnvironmentId(source.getEnvironmentId());
        switch (source.getType()) {
            case ACTIVE_DIRECTORY:
                request.setActiveDirectory(getActiveDirectory(source));
                break;
            case MIT:
                request.setMit(getMit(source));
                break;
            case FREEIPA:
                request.setFreeIpa(getFreeIPA(source));
                break;
            default:

        }
        return request;
    }

    private FreeIPAKerberosDescriptor getFreeIPA(KerberosConfig source) {
        FreeIPAKerberosDescriptor freeIPA = new FreeIPAKerberosDescriptor();
        fillRequestWithCommonFields(freeIPA, source);
        freeIPA.setAdminUrl(source.getAdminUrl());
        freeIPA.setRealm(getFakeSecretIfNotNull(source.getRealm(), FAKE_REALM_POSTFIX));
        freeIPA.setUrl(source.getUrl());
        return freeIPA;
    }

    private MITKerberosDescriptor getMit(KerberosConfig source) {
        MITKerberosDescriptor mit = new MITKerberosDescriptor();
        fillRequestWithCommonFields(mit, source);
        mit.setAdminUrl(source.getAdminUrl());
        mit.setPrincipal(getFakeSecretIfNotNull(source.getPrincipal(), FAKE_PRINCIPAL_POSTFIX));
        mit.setRealm(getFakeSecretIfNotNull(source.getRealm(), FAKE_REALM_POSTFIX));
        mit.setUrl(source.getUrl());
        return mit;
    }

    private ActiveDirectoryKerberosDescriptor getActiveDirectory(KerberosConfig source) {
        ActiveDirectoryKerberosDescriptor activeDirectory = new ActiveDirectoryKerberosDescriptor();
        fillRequestWithCommonFields(activeDirectory, source);
        activeDirectory.setAdminUrl(source.getAdminUrl());
        activeDirectory.setContainerDn(source.getContainerDn());
        activeDirectory.setLdapUrl(source.getLdapUrl());
        activeDirectory.setPrincipal(getFakeSecretIfNotNull(source.getPrincipal(), FAKE_PRINCIPAL_POSTFIX));
        activeDirectory.setRealm(getFakeSecretIfNotNull(source.getRealm(), FAKE_REALM_POSTFIX));
        activeDirectory.setUrl(source.getUrl());
        return activeDirectory;
    }

    private void fillRequestWithCommonFields(KerberosDescriptorBase target, KerberosConfig source) {
        target.setAdmin(getFakeSecretIfNotNull(source.getAdmin(), FAKE_ADMIN_POSTFIX));
        target.setDomain(source.getDomain());
        target.setNameServers(source.getNameServers());
        target.setPassword(getFakeSecretIfNotNull(source.getPassword(), FAKE_PASSWORD_POSTFIX));
        target.setTcpAllowed(source.isTcpAllowed());
        target.setVerifyKdcTrust(source.getVerifyKdcTrust());
    }

    private String getFakeSecretIfNotNull(String value, String postfix) {
        if (Strings.isNotBlank(value)) {
            String postString = postfix != null ? postfix : "secret";
            return "fake-" + postString;
        }
        return value;
    }
}
