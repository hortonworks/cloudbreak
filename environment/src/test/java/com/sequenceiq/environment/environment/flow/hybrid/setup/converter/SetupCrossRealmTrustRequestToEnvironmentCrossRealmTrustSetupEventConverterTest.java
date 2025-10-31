package com.sequenceiq.environment.environment.flow.hybrid.setup.converter;

import static com.sequenceiq.environment.environment.flow.hybrid.setup.event.EnvironmentCrossRealmTrustSetupStateSelectors.TRUST_SETUP_VALIDATION_EVENT;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.common.type.KdcType;
import com.sequenceiq.environment.api.v1.environment.model.request.SetupCrossRealmTrustRequest;
import com.sequenceiq.environment.api.v2.environment.model.request.SetupCrossRealmTrustV2ActiveDirectoryRequest;
import com.sequenceiq.environment.api.v2.environment.model.request.SetupCrossRealmTrustV2KdcServerRequest;
import com.sequenceiq.environment.api.v2.environment.model.request.SetupCrossRealmTrustV2MitRequest;
import com.sequenceiq.environment.api.v2.environment.model.request.SetupCrossRealmTrustV2Request;
import com.sequenceiq.environment.environment.flow.hybrid.setup.event.EnvironmentCrossRealmTrustSetupEvent;

@ExtendWith(MockitoExtension.class)
class SetupCrossRealmTrustRequestToEnvironmentCrossRealmTrustSetupEventConverterTest {

    private static final long ENV_ID = 1L;

    private static final String ACCOUNT_ID = "acc";

    private static final String ENV_NAME = "env";

    private static final String ENV_CRN = "crn";

    private static final String REMOTE_ENV_CRN = "remote-env-crn";

    private static final String REALM = "realm.com";

    private static final String FQDN = "fqdn";

    private static final String IP = "1.1.1.1";

    private static final String DNS_IP = "8.8.8.8";

    private static final String TRUST_SECRET = "secret";

    @InjectMocks
    private SetupCrossRealmTrustRequestToEnvironmentCrossRealmTrustSetupEventConverter underTest;

    @BeforeEach
    void setUp() {
    }

    @Test
    void convertV1() {
        SetupCrossRealmTrustRequest request = new SetupCrossRealmTrustRequest();
        request.setRealm(REALM);
        request.setIp(IP);
        request.setFqdn(FQDN);
        request.setRemoteEnvironmentCrn(REMOTE_ENV_CRN);
        request.setTrustSecret(TRUST_SECRET);

        EnvironmentCrossRealmTrustSetupEvent result = underTest.convertV1(ENV_ID, ACCOUNT_ID, ENV_NAME, ENV_CRN, request);

        vaidateV1(result);
    }

    @Test
    void convertV2Ad() {
        SetupCrossRealmTrustV2Request request = crossRealmTrustV2Request();
        SetupCrossRealmTrustV2ActiveDirectoryRequest ad = new SetupCrossRealmTrustV2ActiveDirectoryRequest();
        ad.setRealm(REALM);
        ad.setServers(List.of(createServer()));
        request.setAd(ad);

        EnvironmentCrossRealmTrustSetupEvent result = underTest.convertV2(ENV_ID, ACCOUNT_ID, ENV_NAME, ENV_CRN, request);

        vaidateV2(result, KdcType.ACTIVE_DIRECTORY);
    }

    @Test
    void convertV2Mit() {
        SetupCrossRealmTrustV2Request request = crossRealmTrustV2Request();
        SetupCrossRealmTrustV2MitRequest mit = new SetupCrossRealmTrustV2MitRequest();
        mit.setRealm(REALM);
        mit.setServers(List.of(createServer()));
        request.setMit(mit);

        EnvironmentCrossRealmTrustSetupEvent result = underTest.convertV2(ENV_ID, ACCOUNT_ID, ENV_NAME, ENV_CRN, request);

        vaidateV2(result, KdcType.MIT);
    }

    private SetupCrossRealmTrustV2Request crossRealmTrustV2Request() {
        SetupCrossRealmTrustV2Request request = new SetupCrossRealmTrustV2Request();
        request.setDnsServerIps(List.of(DNS_IP));
        request.setRemoteEnvironmentCrn(REMOTE_ENV_CRN);
        request.setTrustSecret(TRUST_SECRET);
        return request;
    }

    private SetupCrossRealmTrustV2KdcServerRequest createServer() {
        SetupCrossRealmTrustV2KdcServerRequest server = new SetupCrossRealmTrustV2KdcServerRequest();
        server.setFqdn(FQDN);
        server.setIp(IP);
        return server;
    }

    private void vaidateV1(EnvironmentCrossRealmTrustSetupEvent result) {
        validate(result, KdcType.ACTIVE_DIRECTORY, IP);
    }

    private void vaidateV2(EnvironmentCrossRealmTrustSetupEvent result, KdcType kdcType) {
        validate(result, kdcType, DNS_IP);
    }

    private void validate(EnvironmentCrossRealmTrustSetupEvent result, KdcType kdcType, String dnsIp) {
        assertThat(result)
                .returns(TRUST_SETUP_VALIDATION_EVENT.selector(), EnvironmentCrossRealmTrustSetupEvent::getSelector)
                .returns(ENV_ID, EnvironmentCrossRealmTrustSetupEvent::getResourceId)
                .returns(ENV_NAME, EnvironmentCrossRealmTrustSetupEvent::getResourceName)
                .returns(ENV_CRN, EnvironmentCrossRealmTrustSetupEvent::getResourceCrn)
                .returns(kdcType, EnvironmentCrossRealmTrustSetupEvent::getKdcType)
                .returns(REALM, EnvironmentCrossRealmTrustSetupEvent::getKdcRealm)
                .returns(FQDN, EnvironmentCrossRealmTrustSetupEvent::getKdcFqdn)
                .returns(ACCOUNT_ID, EnvironmentCrossRealmTrustSetupEvent::getAccountId)
                .returns(IP, EnvironmentCrossRealmTrustSetupEvent::getKdcIp)
                .returns(dnsIp, EnvironmentCrossRealmTrustSetupEvent::getDnsIp)
                .returns(REMOTE_ENV_CRN, EnvironmentCrossRealmTrustSetupEvent::getRemoteEnvironmentCrn)
                .returns(TRUST_SECRET, EnvironmentCrossRealmTrustSetupEvent::getTrustSecret);
    }

}
