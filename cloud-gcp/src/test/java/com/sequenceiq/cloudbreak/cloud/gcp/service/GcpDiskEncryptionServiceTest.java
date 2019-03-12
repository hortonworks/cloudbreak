package com.sequenceiq.cloudbreak.cloud.gcp.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.apache.http.HttpHost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.InputStreamEntity;
import org.apache.http.localserver.LocalServerTestBase;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.MockitoAnnotations;
import org.springframework.test.util.ReflectionTestUtils;

import com.google.api.services.compute.model.Disk;
import com.sequenceiq.cloudbreak.cloud.model.InstanceStatus;
import com.sequenceiq.cloudbreak.cloud.model.InstanceTemplate;
import com.sequenceiq.cloudbreak.service.CloudbreakServiceException;

public class GcpDiskEncryptionServiceTest extends LocalServerTestBase {

    @InjectMocks
    private GcpDiskEncryptionService underTest;

    @BeforeEach
    @Override
    public void setUp() throws Exception {
        super.setUp();
        serverBootstrap.registerHandler("/google/cert",
                (request, response, context) -> response.setEntity(new InputStreamEntity(readPublicKeyPemFile(), ContentType.APPLICATION_OCTET_STREAM)));
        serverBootstrap.registerHandler("/google/cert/invalid",
                (request, response, context) -> {
                    String pemFile = "-----BEGIN CERTIFICATE-----\nmxbV98vjuW6lMTn7t+DZN95f6IJn9AOnhw==\n-----END CERTIFICATE-----";
                    response.setEntity(new InputStreamEntity(new ByteArrayInputStream(pemFile.getBytes(StandardCharsets.UTF_8)),
                            ContentType.APPLICATION_OCTET_STREAM));
                });
        MockitoAnnotations.initMocks(this);
    }

    @AfterEach
    @Override
    public void shutDown() {
        if (server != null) {
            server.shutdown(100, TimeUnit.MILLISECONDS);
            server = null;
        }
    }

    @Test
    public void rawKey() {
        String encryptionKey = "Hello World";

        doTestRawKey("Hello World", "pZGm1Av0IEBKARczz7exkNYsZb8LzaMrV7J32a2fFG4=",
                Map.of("type", "CUSTOM", "keyEncryptionMethod", "RAW", "key", "Hello World"));
        doTestRawKey("", "47DEQpj8HBSa+/TImW+5JCeuQeRkm5NMpJWZG3hSuFU=",
                Map.of("type", "CUSTOM", "keyEncryptionMethod", "RAW", "key", ""));
    }

    private void doTestRawKey(String encryptionKey, String expectedEncodedKey, Map<String, Object> params) {
        InstanceTemplate instanceTemplate = new InstanceTemplate("flavor", "name", 0L, List.of(), InstanceStatus.CREATE_REQUESTED,
                params, 0L, "cb-centos66-amb200-2015-05-25");
        Disk disk = new Disk();
        underTest.addEncryptionKeyToDisk(instanceTemplate, disk);

        assertNotNull(disk.getDiskEncryptionKey());
        assertEquals(expectedEncodedKey, disk.getDiskEncryptionKey().getRawKey());
    }

    @Test
    public void rsaEncryptedKey() throws Exception {
        final HttpHost target = start();
        final String baseURL = "http://localhost:" + target.getPort() + "/google/cert";
        ReflectionTestUtils.setField(underTest, "googlePublicCertUrl", baseURL);

        String encryptionKey = "Hello World";

        InstanceTemplate instanceTemplate = createRsaInstanceTemplate();
        Disk disk = new Disk();
        underTest.addEncryptionKeyToDisk(instanceTemplate, disk);

        assertNotNull(disk.getDiskEncryptionKey());
        assertNotNull(disk.getDiskEncryptionKey().getRsaEncryptedKey());
        assertFalse(disk.getDiskEncryptionKey().getRsaEncryptedKey().isEmpty());
    }

    @Test
    public void rsaEncryptedKeyWithRestException() {
        final String baseURL = "http://localhost:0000/google/cert";
        ReflectionTestUtils.setField(underTest, "googlePublicCertUrl", baseURL);

        String encryptionKey = "Hello World";

        InstanceTemplate template = createRsaInstanceTemplate();
        Disk disk = new Disk();
        Throwable exception = assertThrows(CloudbreakServiceException.class, () -> underTest.addEncryptionKeyToDisk(template, disk));
        assertEquals("Failed to connect to URI: http://localhost:0000/google/cert", exception.getMessage());
    }

    @Test
    public void rsaEncryptedKeyWithInvalidUrl() throws Exception {
        final HttpHost target = start();
        final String baseURL = "http://localhost:" + target.getPort() + "/google/derp";
        ReflectionTestUtils.setField(underTest, "googlePublicCertUrl", baseURL);

        String encryptionKey = "Hello World";

        InstanceTemplate template = createRsaInstanceTemplate();
        Disk disk = new Disk();
        Throwable exception = assertThrows(CloudbreakServiceException.class, () -> underTest.addEncryptionKeyToDisk(template, disk));
        assertEquals("GET request to [http://localhost:" + target.getPort() + "/google/derp] failed with status code: 501", exception.getMessage());
    }

    @Test
    public void rsaEncryptedKeyWithInvalidCertificate() throws Exception {
        final HttpHost target = start();
        final String baseURL = "http://localhost:" + target.getPort() + "/google/cert/invalid";
        ReflectionTestUtils.setField(underTest, "googlePublicCertUrl", baseURL);

        String encryptionKey = "Hello World";

        InstanceTemplate template = createRsaInstanceTemplate();
        Disk disk = new Disk();
        Throwable exception = assertThrows(CloudbreakServiceException.class, () -> underTest.addEncryptionKeyToDisk(template, disk));
        assertEquals("Failed to get public key from certificate", exception.getMessage());
    }

    private InstanceTemplate createRsaInstanceTemplate() {
        Map<String, Object> parameters = Map.of("type", "CUSTOM", "keyEncryptionMethod", "RSA", "key", "Hello World");
        return new InstanceTemplate("flavor", "name", 0L, List.of(), InstanceStatus.CREATE_REQUESTED,
                parameters, 0L, "cb-centos66-amb200-2015-05-25");
    }

    private InputStream readPublicKeyPemFile() {
        return GcpDiskEncryptionServiceTest.class.getResourceAsStream("/encryption/google-cloud-csek-ingress.pem");
    }
}