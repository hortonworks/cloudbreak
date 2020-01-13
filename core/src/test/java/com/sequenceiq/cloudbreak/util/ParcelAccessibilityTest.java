package com.sequenceiq.cloudbreak.util;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.apache.http.Header;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpHead;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.junit.jupiter.api.Test;
import org.yaml.snakeyaml.Yaml;

import com.sequenceiq.cloudbreak.cloud.model.component.DefaultCDHInfo;
import com.sequenceiq.cloudbreak.common.json.Json;

public class ParcelAccessibilityTest {

    @Test
    public void testParcelAccessibility() throws URISyntaxException, IOException {
        URL url = ParcelAccessibilityTest.class.getResource("/application.yml");
        if (url.toString().endsWith("test/resources/application.yml")) {
            throw new RuntimeException("We should validate the main/resources application.yml");
        }
        Path resPath = Paths.get(url.toURI());
        String appYml = new String(Files.readAllBytes(resPath), "UTF8");
        Yaml yaml = new Yaml();
        Map<String, Object> load = yaml.load(appYml);
        List<DefaultCDHInfo> defaultCDHInfo = getCsds(load);
        defaultCDHInfo.stream()
                .filter(s -> !s.getParcels().isEmpty())
                .flatMap(s -> s.getParcels().stream())
                .flatMap(s -> s.getCsd().stream())
                .forEach(this::validateUrl);
    }

    private List<DefaultCDHInfo> getCsds(Map<String, Object> load) {
        Map<String, Object> entries = load;
        for (String s : Arrays.asList("cb", "cdh", "entries")) {
            entries = (Map<String, Object>) entries.get(s);
        }
        List<DefaultCDHInfo> defaultCDHInfos = new ArrayList<>();
        entries.forEach((key, value) -> {
            DefaultCDHInfo defaultCDHInfo = Json.silent(value).getSilent(DefaultCDHInfo.class);
            defaultCDHInfo.getParcels();
            defaultCDHInfos.add(defaultCDHInfo);
        });
        return defaultCDHInfos;
    }

    private void validateUrl(String url) {
        try (CloseableHttpClient client = HttpClientBuilder
                .create()
                .useSystemProperties()
                .build()) {
            URI target = new URL(url).toURI();
            CloseableHttpResponse execute = client.execute(new HttpHead(target));
            if (execute.getStatusLine().getStatusCode() != 200) {
                throw new RuntimeException("CSD cannot find on the " + url);
            }
            int length = getContentLength(execute);
            if (length <= 0) {
                throw new RuntimeException("CSD has not length: " + url);
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private int getContentLength(CloseableHttpResponse execute) {
        Optional<Header> first = Arrays.stream(execute.getAllHeaders()).filter(s -> "content-length".equalsIgnoreCase(s.getName())).findFirst();
        return first.map(header -> Integer.parseInt(header.getValue())).orElse(0);
    }
}
