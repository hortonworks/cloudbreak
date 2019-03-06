package com.sequenceiq.cloudbreak.converter.v4.kerberos;

import java.util.stream.Collectors;

import org.apache.commons.codec.binary.Base64;
import org.apache.logging.log4j.util.Strings;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.kerberos.requests.ActiveDirectoryKerberosDescriptor;
import com.sequenceiq.cloudbreak.api.endpoint.v4.kerberos.requests.AmbariKerberosDescriptor;
import com.sequenceiq.cloudbreak.api.endpoint.v4.kerberos.requests.FreeIPAKerberosDescriptor;
import com.sequenceiq.cloudbreak.api.endpoint.v4.kerberos.requests.KerberosTypeBase;
import com.sequenceiq.cloudbreak.api.endpoint.v4.kerberos.requests.KerberosV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.kerberos.requests.MITKerberosDescriptor;
import com.sequenceiq.cloudbreak.converter.AbstractConversionServiceAwareConverter;
import com.sequenceiq.cloudbreak.domain.KerberosConfig;
import com.sequenceiq.cloudbreak.domain.view.CompactView;

@Component
public class KerberosConfigToKerberosV4RequestConverter extends AbstractConversionServiceAwareConverter<KerberosConfig, KerberosV4Request> {

    private static final String EMPTY_JSON = "{}";

    private static final String EMPTY_DESCRIPTOR_JSON = "{\"kerberos-env\":{\"properties\":{\"kdc_type\":\"kdc-type\",\"kdc_hosts\":\"kdc-host-value\","
            + "\"admin_server_host\":\"admin-server-host-value\",\"realm\":\"realm-value\"}}}";

    private static final String FAKE_REALM_POSTFIX = "realm";

    private static final String FAKE_ADMIN_POSTFIX = "admin";

    private static final String FAKE_PRINCIPAL_POSTFIX = "principal";

    private static final String FAKE_PASSWORD_POSTFIX = "password";

    @Override
    public KerberosV4Request convert(KerberosConfig source) {
        KerberosV4Request request = new KerberosV4Request();
        request.setName(source.getName());
        request.setDescription(source.getDescription());
        request.setEnvironments(source.getEnvironments().stream()
                .map(CompactView::getName).collect(Collectors.toSet()));
        switch (source.getType()) {
            case ACTIVE_DIRECTORY:
                request.setActiveDirectory(getActiveDirectory(source));
                break;
            case AMBARI_DESCRIPTOR:
                request.setAmbariDescriptor(getCustom(source));
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

    private AmbariKerberosDescriptor getCustom(KerberosConfig source) {
        AmbariKerberosDescriptor custom = new AmbariKerberosDescriptor();
        fillRequestWithCommonFields(custom, source);
        custom.setDescriptor(Base64.encodeBase64String(EMPTY_DESCRIPTOR_JSON.getBytes()));
        custom.setKrb5Conf(Base64.encodeBase64String(EMPTY_JSON.getBytes()));
        custom.setPrincipal(getFakeSecretIfNotNull(source.getPrincipal(), FAKE_PRINCIPAL_POSTFIX));
        return custom;
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

    private void fillRequestWithCommonFields(KerberosTypeBase target, KerberosConfig source) {
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
