package com.sequenceiq.it.cloudbreak.newway;

import static com.sequenceiq.it.cloudbreak.newway.ImageCatalogEntity.IMAGE_CATALOG_URL;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.EmptyStackException;
import java.util.HashSet;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Function;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;

import com.google.gson.Gson;
import com.sequenceiq.it.IntegrationTestContext;
import com.sequenceiq.it.cloudbreak.newway.log.Log;
import com.sequenceiq.it.cloudbreak.newway.mock.DefaultModel;
import com.sequenceiq.it.cloudbreak.newway.mock.ImageCatalogServiceMock;
import com.sequenceiq.it.cloudbreak.newway.mock.MockModel;
import com.sequenceiq.it.spark.ITResponse;
import com.sequenceiq.it.verification.Verification;

public class Mock extends SparkMockEntity {
    public static final int DEFAULT_PORT = 9444;

    public static final String MOCK_SERVER = "MOCK_SERVER";

    public static final String CLOUDBREAK_SERVER_ROOT = "CLOUDBREAK_SERVER_ROOT";

    public static final String HTTPS = "https://";

    private static final Logger LOGGER = LoggerFactory.getLogger(Mock.class);

    private static final String MOCKRESPONSE = "/mockresponse/";

    private static final Gson GSON = new Gson();

    private static final java.util.Stack<Mock> MOCKS = new java.util.Stack<>();

    private static final Set<Mock> USED_MOCKS = new HashSet<>();

    protected Mock(String id, String hostname, int port) {
        super(id, hostname, port);
    }

    protected Mock(String hostname, int port) {
        this(MOCK_SERVER, hostname, port);
    }

    public static Function<IntegrationTestContext, Mock> getTestContextMock(String key) {
        return testContext -> testContext.getContextParam(key, Mock.class);
    }

    public static Function<IntegrationTestContext, Mock> getTestContextMock() {
        return getTestContextMock(MOCK_SERVER);
    }

    public static Mock isCreated() {
        return isCreated(new DefaultModel());
    }

    public static Mock isCreated(MockModel model) {
        Mock mock = getAMock();
        mock.setCreationStrategy(new StrategyWithModel(model));
        return mock;
    }

    public static Entity imageCatalogServiceIsStarted() {
        Mock mock = getAMock();
        mock.setCreationStrategy(Mock::startImageCatalogInGiven);
        return mock;
    }

    private static void startImageCatalogInGiven(IntegrationTestContext integrationTestContext, Entity entity) {
        Mock mock = (Mock) entity;
        mock.initSparkService();
        ImageCatalogServiceMock imageCatalogServiceMock = new ImageCatalogServiceMock(mock.getSparkService());
        imageCatalogServiceMock.mockImageCatalogResponse(integrationTestContext.getContextParam(CLOUDBREAK_SERVER_ROOT));
        String imageCatalogAddress = String.join("", HTTPS, mock.getHostname(), ":", Integer.toString(mock.getPort()), ITResponse.IMAGE_CATALOG);

        integrationTestContext.putContextParam(IMAGE_CATALOG_URL, imageCatalogAddress);
    }

    public static Gson gson() {
        return GSON;
    }

    public static String responseFromJsonFile(String path) {
        try (InputStream inputStream = ITResponse.class.getResourceAsStream(MOCKRESPONSE + path)) {
            return IOUtils.toString(inputStream);
        } catch (IOException e) {
            LOGGER.error("can't read file from path", e);
            return "";
        }
    }

    public static File createTempFileFromClasspath(String file) {
        try {
            InputStream sshPemInputStream = new ClassPathResource(file).getInputStream();
            File tempKeystoreFile = File.createTempFile(file, ".tmp");
            try (OutputStream outputStream = new FileOutputStream(tempKeystoreFile)) {
                IOUtils.copy(sshPemInputStream, outputStream);
            } catch (IOException e) {
                LOGGER.error("can't write " + file, e);
            }
            return tempKeystoreFile;
        } catch (IOException e) {
            throw new RuntimeException(file + " not found", e);
        }
    }

    public static Action<Mock> deleteStack() {
        return new Action<>(getTestContextMock(), Mock::deleteStack);
    }

    public static Action<Mock> deleteCredential() {
        return new Action<>(getTestContextMock(), Mock::deleteCredential);
    }

    public static Action<Mock> delete() {
        return new Action<>(getTestContextMock(), Mock::deleteMock);
    }

    private static void deleteMock(IntegrationTestContext integrationTestContext, Entity entity) {
        Mock mock = (Mock) entity;
        freeAMock(mock);
    }

    protected static void deleteStack(IntegrationTestContext integrationTestContext, Entity entity) {
        Mock mock = (Mock) entity;
        CloudbreakClient client;
        String stackName;
        client = integrationTestContext.getContextParam(CloudbreakClient.CLOUDBREAK_CLIENT,
                CloudbreakClient.class);
        stackName = mock.getModel().getClusterName();
        Log.log(" delete by mock: " + stackName);
        client.getCloudbreakClient().stackV2Endpoint()
                .deletePrivate(stackName, false, false);
    }

    protected static void deleteCredential(IntegrationTestContext integrationTestContext, Entity entity) {
        Mock mock = (Mock) entity;
        CloudbreakClient client;
        String credentialName;
        client = integrationTestContext.getContextParam(CloudbreakClient.CLOUDBREAK_CLIENT,
                CloudbreakClient.class);
        credentialName = mock.getCredentialName();
        Log.log(" delete by mock: " + credentialName);
        client.getCloudbreakClient().credentialEndpoint().deletePrivate(credentialName);
    }

    public static void setup(String hostName, int threadCount) {
        for (int i = 0; i <= threadCount; i++) {
            Mock mock = new Mock(hostName, DEFAULT_PORT + i);
            MOCKS.push(mock);
        }
    }

    public static synchronized Mock getAMock() {
        try {
            Mock mock = MOCKS.pop();
            USED_MOCKS.add(mock);
            return mock;
        } catch (EmptyStackException e) {
            LOGGER.warn("Mock store is empty", e);
            return null;
        }
    }

    public static synchronized void freeAMock(Mock mock) {
        USED_MOCKS.remove(mock);
        MOCKS.push(mock);
    }

    public static void shutdown() {
        MOCKS.stream().forEach(mock -> mock.stop());
        MOCKS.clear();
        USED_MOCKS.stream().forEach(mock -> mock.stop());
        USED_MOCKS.clear();
    }

    public static Assertion<Mock> assertCalls(Verification... verifications) {
        return new AssertMock(verifications);
    }

    public static Assertion<Mock> assertThis(BiConsumer<Mock, IntegrationTestContext> check) {
        return new Assertion<>(getTestContextMock(), check);
    }

    static class StrategyWithModel implements Strategy {

        private final MockModel model;

        StrategyWithModel(MockModel model) {
            this.model = model;
        }

        @Override
        public void doAction(IntegrationTestContext integrationTestContext, Entity entity) throws Exception {
            Mock mock = (Mock) entity;

            MockModel model = this.model;
            mock.setAndStart(model);

            integrationTestContext.putContextParam("MOCK_PORT", Integer.valueOf(mock.getPort()));
        }

    }
}
