package com.sequenceiq.it.cloudbreak.testcase.mock;

import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.inject.Inject;

import org.apache.http.Header;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpHead;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.testng.annotations.Test;

import com.sequenceiq.cloudbreak.api.endpoint.v4.util.responses.ClouderaManagerStackDescriptorV4Response;
import com.sequenceiq.it.cloudbreak.CloudbreakClient;
import com.sequenceiq.it.cloudbreak.client.UtilTestClient;
import com.sequenceiq.it.cloudbreak.context.Description;
import com.sequenceiq.it.cloudbreak.context.MockedTestContext;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.util.StackMatrixTestDto;
import com.sequenceiq.it.cloudbreak.testcase.AbstractIntegrationTest;

public class ParcelAccessibilityTest extends AbstractIntegrationTest {

    @Inject
    private UtilTestClient utilTestClient;

    protected void setupTest(TestContext testContext) {
        createDefaultUser(testContext);
        initializeDefaultBlueprints(testContext);
    }

    @Test(dataProvider = TEST_CONTEXT_WITH_MOCK)
    @Description(
            given = "stack matrix",
            when = "list all supported stack combinations",
            then = "validate urls are still available"
    )
    public void testParcelAvailability(MockedTestContext testContext) throws IOException {
        testContext
                .given(StackMatrixTestDto.class)
                .when(utilTestClient.stackMatrixV4())
                .then(this::validateParcelUrls)
                .validate();
    }

    private StackMatrixTestDto validateParcelUrls(TestContext tc, StackMatrixTestDto entity, CloudbreakClient cc) {
        for (Map.Entry<String, ClouderaManagerStackDescriptorV4Response> entry : entity.getResponse().getCdh().entrySet()) {
            entry.getValue().getRepository().getStack().entrySet().stream()
                    .filter(s -> !s.getValue().isEmpty())
                    .map(s -> s.getValue() + "manifest.json")
                    .filter(s -> s.startsWith("http"))
                    .forEach(this::validateUrl);
            entry.getValue().getClouderaManager().getRepository().entrySet().stream()
                    .filter(s -> !s.getValue().getBaseUrl().isEmpty())
                    .map(s -> s.getValue().getBaseUrl() + "cloudera-manager.repo")
                    .filter(s -> s.startsWith("http"))
                    .forEach(this::validateUrl);
            entry.getValue().getProducts().stream()
                    .forEach(product -> validateUrl(product.getParcel()));
            entry.getValue().getProducts().stream()
                    .map(s -> s.getCsd())
                    .flatMap(List::stream)
                    .forEach(csd -> validateUrl(csd));
        }
        return entity;
    }

    private void validateUrl(String url) {
        try (CloseableHttpClient client = HttpClientBuilder
                .create()
                .useSystemProperties()
                .build()) {
            URI target = new URL(url).toURI();
            CloseableHttpResponse execute = client.execute(new HttpHead(target));
            if (execute.getStatusLine().getStatusCode() != 200) {
                throw new IllegalArgumentException(
                        String.format("URL %s did not respond with 200 OK. It might be an obsolete entry in application.yml. " +
                                "If you hadn't made any changes to them, it's possible that it got outdated. " +
                                "Please check if there any update avaiable.", url));
            }
            int length = getContentLength(execute);
            if (length <= 0) {
                throw new IllegalArgumentException(
                        String.format("URL %s was reachable but the file had a Context of 0 bytes. It might be an obsolete entry in application.yml " +
                                "If you hadn't made any changes to them, it's possible that it got outdated. " +
                                "Please check if there any update avaiable.", url));
            }
        } catch (UnknownHostException e) {
            throw new IllegalArgumentException(String.format("Cloudera network is not reachable: %s", url));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private int getContentLength(CloseableHttpResponse execute) {
        Optional<Header> first = Arrays.stream(execute.getAllHeaders()).filter(s -> "content-length".equalsIgnoreCase(s.getName())).findFirst();
        return first.map(header -> Integer.parseInt(header.getValue())).orElse(0);
    }

}
