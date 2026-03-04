package com.sequenceiq.mock.config;

import java.security.KeyPair;
import java.security.cert.X509Certificate;

import jakarta.annotation.Nullable;
import jakarta.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import com.sequenceiq.cloudbreak.certificate.PkiUtil;
import com.sequenceiq.cloudbreak.common.base64.Base64Util;

@Configuration
public class CcmV2Config {

    @Value("${grpc.ccmv2.jumpgate.relay.host:}")
    private String jumpgateRelayHost;

    @Value("${grpc.ccmv2.jumpgate.relay.port:55080}")
    private Integer jumpgateRelayPort;

    private String certificateBase64;

    private String privateKey;

    @PostConstruct
    protected void init() {
        // Generate mock certificate and private key
        KeyPair keyPair = PkiUtil.generateKeypair();
        X509Certificate certificate = PkiUtil.cert(keyPair, "127.0.0.1", keyPair);
        certificateBase64 = Base64Util.encode(PkiUtil.convert(certificate));
        privateKey = PkiUtil.convert(keyPair.getPrivate());
    }

    public String getJumpgateRelayHost() {
        return jumpgateRelayHost;
    }

    @Nullable
    public Integer getJumpgateRelayPort() {
        return jumpgateRelayPort;
    }

    public String getCertificateBase64() {
        return certificateBase64;
    }

    public String getPrivateKey() {
        return privateKey;
    }
}
