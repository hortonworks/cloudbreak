package com.sequenceiq.it.cloudbreak;

import static spark.Spark.after;
import static spark.Spark.before;
import static spark.Spark.secure;
import static spark.Spark.stop;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ResourceLoader;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;

import com.google.gson.Gson;
import com.sequenceiq.it.verification.Call;
import com.sequenceiq.it.verification.Verification;

import spark.Response;

public abstract class AbstractMockIntegrationTest extends AbstractCloudbreakIntegrationTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractMockIntegrationTest.class);

    private Gson gson = new Gson();
    private Map<Call, Response> requestResponseMap;

    @Inject
    private ResourceLoader resourceLoader;

    @BeforeClass
    public void configMockServer() {
        requestResponseMap = new HashMap<>();
        File keystoreFile = createTempFileFromClasspath("/keystore_server");
        secure(keystoreFile.getPath(), "secret", null, null);
    }

    public Gson gson() {
        return gson;
    }

    protected void initSpark() {
        before((req, res) -> res.type("application/json"));
        after((request, response) -> requestResponseMap.put(Call.fromRequest(request), response));
    }

    @AfterClass
    public void breakDown() {
        stop();
    }

    public Verification verify(String path, String httpMethod) {
        return new Verification(path, httpMethod, requestResponseMap, false);
    }

    public Verification verifyRegexpPath(String regexpPath, String httpMethod) {
        return new Verification(regexpPath, httpMethod, requestResponseMap, true);
    }

    protected String responseFromJsonFile(String path) {
        try (InputStream inputStream = resourceLoader.getResource("/mockresponse/" + path).getInputStream()) {
            return IOUtils.toString(inputStream);
        } catch (IOException e) {
            LOGGER.error("can't read file from path", e);
            return "";
        }
    }
}
