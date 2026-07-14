package com.sequenceiq.mock.config;

import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.security.KeyPair;
import java.security.cert.X509Certificate;

import jakarta.annotation.Nullable;
import jakarta.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import com.google.common.base.Strings;
import com.sequenceiq.cloudbreak.certificate.PkiUtil;
import com.sequenceiq.cloudbreak.common.base64.Base64Util;

@Configuration
public class CcmV2Config {

    private static final Logger LOGGER = LoggerFactory.getLogger(CcmV2Config.class);

    private static final String TEST_HOST = "ci-cloudbreak.eng.hortonworks.com";

    private static final int HTTP_PORT = 80;

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
        return Strings.isNullOrEmpty(jumpgateRelayHost) ? tryToDetermineHostAddress() : jumpgateRelayHost;
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

    private String tryToDetermineHostAddress() {
        LOGGER.info("Trying to find out address of host running the mock-infrastructure by connecting to {}:80", TEST_HOST);
        try (DatagramSocket socket = new DatagramSocket()) {
            socket.connect(InetAddress.getByName(TEST_HOST), HTTP_PORT);
            String hostAddress = socket.getLocalAddress().getHostAddress();
            LOGGER.info("Using host address: {}", hostAddress);
            return hostAddress;
        } catch (SocketException | UnknownHostException e) {
            LOGGER.debug("Failed to determine host address of host running the mock-infrastructure", e);
            throw new RuntimeException("Failed to determine host address of host running the mock-infrastructure", e);
        }
    }
}
