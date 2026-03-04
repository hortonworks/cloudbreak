package com.sequenceiq.mock.grpc.service;

import static com.sequenceiq.cloudbreak.util.NullUtil.getIfNotNullOtherwise;
import static java.lang.String.format;

import java.nio.charset.StandardCharsets;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.Formatter;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.Mac;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import jakarta.annotation.Nullable;
import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.cloudera.thunderhead.service.clusterconnectivitymanagementv2.ClusterConnectivityManagementV2Grpc;
import com.cloudera.thunderhead.service.clusterconnectivitymanagementv2.ClusterConnectivityManagementV2Proto;
import com.google.common.base.Charsets;
import com.sequenceiq.cloudbreak.auth.crn.Crn;
import com.sequenceiq.cloudbreak.common.base64.Base64Util;
import com.sequenceiq.cloudbreak.util.RandomUtil;
import com.sequenceiq.mock.config.CcmV2Config;

import io.grpc.stub.StreamObserver;

@Component
public class CcmV2Service extends ClusterConnectivityManagementV2Grpc.ClusterConnectivityManagementV2ImplBase {

    private static final Logger LOGGER = LoggerFactory.getLogger(CcmV2Service.class);

    private static final String MACHINE_USER_ACCESS_KEY_ID = Base64Util.encode("ccmv2-mock-machine-user-access-key-id");

    private static final String MACHINE_USER_SECRET_ACCESS_KEY = Base64Util.encode("m-machine-user-secret-access-key");

    private static final int KEY_ID_SNIPPED_LENGTH_FOR_PRIVATE_KEY_ENCRYPTION = 16;

    private static final int KEY_ID_SNIPPED_LENGTH_FOR_ENCIPHERED_ACCESS_KEY = 32;

    private static final int RANDOM_IV_LENGTH = 16;

    private static final byte[] IV = "ClouderaCloudera".getBytes(Charsets.UTF_8);

    @Inject
    private CcmV2Config ccmV2Config;

    private final Map<String, ClusterConnectivityManagementV2Proto.InvertingProxyAgent> agents = new ConcurrentHashMap();

    @Override
    public void createOrGetInvertingProxy(ClusterConnectivityManagementV2Proto.CreateOrGetInvertingProxyRequest request,
            StreamObserver<ClusterConnectivityManagementV2Proto.CreateOrGetInvertingProxyResponse> responseObserver) {
        responseObserver.onNext(ClusterConnectivityManagementV2Proto.CreateOrGetInvertingProxyResponse.getDefaultInstance().toBuilder()
                .setInvertingProxy(ClusterConnectivityManagementV2Proto.InvertingProxy.getDefaultInstance().toBuilder()
                        .setStatus(ClusterConnectivityManagementV2Proto.InvertingProxy.Status.READY)
                        .setHostname(getHostname(ccmV2Config.getJumpgateRelayHost(), ccmV2Config.getJumpgateRelayPort()))
                        .setCertificate(getIfNotNullOtherwise(ccmV2Config.getCertificateBase64(), ""))
                        .setCaCertificate(getIfNotNullOtherwise(ccmV2Config.getCertificateBase64(), ""))
                        .build())
                .build());
        responseObserver.onCompleted();
    }

    @Override
    public void registerAgent(ClusterConnectivityManagementV2Proto.RegisterAgentRequest request,
            StreamObserver<ClusterConnectivityManagementV2Proto.RegisterAgentResponse> responseObserver) {
        String accountId = request.getAccountId();
        String keyId = request.getKeyId();
        String environmentCrn = request.getEnvironmentCrn();
        String hmacKey = request.getAes256Parameters().getHmacKey();

        ClusterConnectivityManagementV2Proto.InvertingProxyAgent.Builder agentBuilder = ClusterConnectivityManagementV2Proto.InvertingProxyAgent.newBuilder()
                .setAgentCrn(createAgentCrn(accountId, keyId))
                .setCertificate(ccmV2Config.getCertificateBase64())
                .addCertificates(ccmV2Config.getCertificateBase64())
                .setEnvironmentCrn(environmentCrn)
                .setAccessKeyId(MACHINE_USER_ACCESS_KEY_ID)
                .setEncipheredPrivateKey(Base64Util.encode(encryptPrivateKey(keyId, ccmV2Config.getPrivateKey())));
        addEncryptedParameters(keyId, MACHINE_USER_SECRET_ACCESS_KEY, hmacKey, agentBuilder);

        ClusterConnectivityManagementV2Proto.InvertingProxyAgent agent = agentBuilder.build();
        agents.put(keyId, agent);

        responseObserver.onNext(ClusterConnectivityManagementV2Proto.RegisterAgentResponse.getDefaultInstance().toBuilder()
                .setInvertingProxyAgent(agent)
                .build());
        responseObserver.onCompleted();
    }

    @Override
    public void unregisterAgent(ClusterConnectivityManagementV2Proto.UnregisterAgentRequest request,
            StreamObserver<ClusterConnectivityManagementV2Proto.UnregisterAgentResponse> responseObserver) {
        agents.remove(request.getAgentCrn());
        responseObserver.onNext(ClusterConnectivityManagementV2Proto.UnregisterAgentResponse.getDefaultInstance());
        responseObserver.onCompleted();
    }

    @Override
    public void listAgents(ClusterConnectivityManagementV2Proto.ListAgentsRequest request,
            StreamObserver<ClusterConnectivityManagementV2Proto.ListAgentsResponse> responseObserver) {
        ClusterConnectivityManagementV2Proto.ListAgentsResponse response =
                ClusterConnectivityManagementV2Proto.ListAgentsResponse.newBuilder()
                        .addAllAgents(agents.values())
                        .build();
        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    @Override
    public void createAgentAccessKeyPair(ClusterConnectivityManagementV2Proto.CreateAgentAccessKeyPairRequest request,
            StreamObserver<ClusterConnectivityManagementV2Proto.CreateAgentAccessKeyPairResponse> responseObserver) {
        String accountId = request.getAccountId();
        String agentCrn = request.getAgentCrn();
        String keyId = Crn.fromString(agentCrn).getResource();

        ClusterConnectivityManagementV2Proto.InvertingProxyAgent agent =
                agents.getOrDefault(agentCrn, ClusterConnectivityManagementV2Proto.InvertingProxyAgent.newBuilder()
                        .setAgentCrn(createAgentCrn(accountId, keyId))
                        .setCertificate(ccmV2Config.getCertificateBase64())
                        .build());

        responseObserver.onNext(ClusterConnectivityManagementV2Proto.CreateAgentAccessKeyPairResponse.getDefaultInstance().toBuilder()
                .setInvertingProxyAgent(agent)
                .build());
        responseObserver.onCompleted();
    }

    @Override
    public void deactivateAgentAccessKeyPair(ClusterConnectivityManagementV2Proto.DeactivateAgentAccessKeyPairRequest request,
            StreamObserver<ClusterConnectivityManagementV2Proto.DeactivateAgentAccessKeyPairResponse> responseObserver) {
        // Do nothing
        responseObserver.onNext(ClusterConnectivityManagementV2Proto.DeactivateAgentAccessKeyPairResponse.getDefaultInstance());
        responseObserver.onCompleted();
    }

    private static String getHostname(String host, @Nullable Integer port) {
        return port != null ? String.format("%s:%s", host, port) : host;
    }

    private String createAgentCrn(String accountId, String keyId) {
        return Crn.builder()
                .setPartition(Crn.Partition.CDP)
                .setService(Crn.Service.CCMV2)
                .setRegion(Crn.Region.US_WEST_1)
                .setAccountId(accountId)
                .setResourceType(Crn.ResourceType.AGENT)
                .setResource(keyId)
                .build().toString();
    }

    @SuppressWarnings("checkstyle:RegexpSingleline")
    private byte[] encryptPrivateKey(String key, String privateKey) {
        try {
            String snippedKey = key.substring(0, KEY_ID_SNIPPED_LENGTH_FOR_PRIVATE_KEY_ENCRYPTION);
            byte[] snippedKeyBytes = snippedKey.getBytes(Charsets.UTF_8);
            byte[] privateKeyInBytes = privateKey.getBytes(Charsets.UTF_8);
            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            SecretKeySpec keySpec = new SecretKeySpec(snippedKeyBytes, "AES");
            IvParameterSpec ivParameterSpec = new IvParameterSpec(IV);
            cipher.init(Cipher.ENCRYPT_MODE, keySpec, ivParameterSpec);
            return cipher.doFinal(privateKeyInBytes);
        } catch (NoSuchAlgorithmException | NoSuchPaddingException | IllegalBlockSizeException |
                 InvalidKeyException | InvalidAlgorithmParameterException | BadPaddingException e) {
            LOGGER.error(format("Problem encrypting key: %s", e.getMessage()));
        }
        return new byte[0];
    }

    @SuppressWarnings("checkstyle:RegexpSingleline")
    private void addEncryptedParameters(String keyId, String privateKey, String hmacKey,
            ClusterConnectivityManagementV2Proto.InvertingProxyAgent.Builder agentBuilder) {
        try {
            String snippedKey = keyId.substring(0, KEY_ID_SNIPPED_LENGTH_FOR_ENCIPHERED_ACCESS_KEY);
            byte[] snippedKeyBytes = snippedKey.getBytes(Charsets.UTF_8);
            byte[] privateKeyInBytes = privateKey.getBytes(Charsets.UTF_8);
            String aesCbcPadding = "AES/CBC/PKCS5Padding";
            Cipher cipher = Cipher.getInstance(aesCbcPadding);
            String algorithm = "AES";
            SecretKeySpec keySpec = new SecretKeySpec(snippedKeyBytes, algorithm);

            byte[] bytes = new byte[RANDOM_IV_LENGTH];
            RandomUtil.getRandom().nextBytes(bytes);
            Base64.Encoder encoder = Base64.getEncoder().withoutPadding();
            String ivStr = encoder.encodeToString(bytes);
            byte[] iv = ivStr.substring(0, RANDOM_IV_LENGTH).getBytes(StandardCharsets.UTF_8);
            IvParameterSpec ivParameterSpec = new IvParameterSpec(iv);

            cipher.init(Cipher.ENCRYPT_MODE, keySpec, ivParameterSpec);
            byte[] encryptedKeyInBytes = cipher.doFinal(privateKeyInBytes);
            String base64EncodedEncryptedPrivateKey = Base64Util.encode(encryptedKeyInBytes);
            String hmacForTheEncryptedPrivateKey = calculateMacForEncryptedKey(hmacKey, base64EncodedEncryptedPrivateKey);

            agentBuilder.setInitialisationVector(ivStr);
            agentBuilder.setEncipheredAccessKey(base64EncodedEncryptedPrivateKey);
            agentBuilder.setHmacForPrivateKey(hmacForTheEncryptedPrivateKey);
        } catch (NoSuchAlgorithmException | NoSuchPaddingException | IllegalBlockSizeException |
                 InvalidKeyException | InvalidAlgorithmParameterException | BadPaddingException e) {
            LOGGER.error(format("Problem encrypting key: %s", e.getMessage()));
        }
    }

    private String calculateMacForEncryptedKey(String key, String data) throws NoSuchAlgorithmException, InvalidKeyException {
        SecretKeySpec secretKeySpec = new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "HmacSHA512");
        Mac mac = Mac.getInstance("HmacSHA512");
        mac.init(secretKeySpec);
        return toHexString(mac.doFinal(data.getBytes(StandardCharsets.UTF_8)));
    }

    private static String toHexString(byte[] bytes) {
        Formatter formatter = new Formatter();
        for (byte b : bytes) {
            formatter.format("%02x", b);
        }
        return formatter.toString();
    }
}
