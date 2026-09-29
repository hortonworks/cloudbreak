package com.sequenceiq.cloudbreak.orchestrator.salt;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.yaml.snakeyaml.Yaml;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hubspot.jinjava.Jinjava;
import com.hubspot.jinjava.interpret.JinjavaInterpreter;
import com.hubspot.jinjava.interpret.RenderResult;
import com.hubspot.jinjava.lib.filter.Filter;
import com.sequenceiq.cloudbreak.util.FileReaderUtils;

class FileCollectorTemplateRenderTest {

    private static final String TEMPLATE_PATH = "salt-common/salt/filecollector/template/filecollector.yaml.j2";

    private static final String SETTINGS_IMPORT_PATTERN = "(?m)^\\{%-? from .*$\\R";

    private static final String DESCRIPTION_WITH_COLON_AND_QUOTE = "CB-34678 step C: heal in place, colon \"and quotes\" test";

    private static final String CASE_NUMBER_WITH_QUOTE = "case \"12345\": urgent";

    private static final String ADDITIONAL_LOG_PATH = "/var/log/my app: v1/*.log";

    private static final String ADDITIONAL_LOG_LABEL = "my \"custom\" label: 1";

    @Test
    void renderShouldProduceValidYamlWhenDescriptionAndCaseNumberContainColonAndQuote() throws IOException {
        Map<String, Object> fileCollector = fileCollectorContext();
        fileCollector.put("description", DESCRIPTION_WITH_COLON_AND_QUOTE);
        fileCollector.put("issue", CASE_NUMBER_WITH_QUOTE);

        Map<String, Object> metadata = renderAndParseWorkspaceMetadata(fileCollector);

        assertEquals(DESCRIPTION_WITH_COLON_AND_QUOTE, metadata.get("name"));
        assertEquals(CASE_NUMBER_WITH_QUOTE, metadata.get("Case_number"));
    }

    @Test
    void renderShouldProduceValidYamlWhenAdditionalLogPathAndLabelContainColonAndQuote() throws IOException {
        Map<String, Object> fileCollector = fileCollectorContext();
        fileCollector.put("additionalLogs", List.of(Map.of("path", ADDITIONAL_LOG_PATH, "label", ADDITIONAL_LOG_LABEL)));

        Map<String, Object> collector = renderAndParseCollector(fileCollector);

        List<Map<String, Object>> files = (List<Map<String, Object>>) collector.get("files");
        assertTrue(files.stream().anyMatch(file -> ADDITIONAL_LOG_PATH.equals(file.get("path")) && ADDITIONAL_LOG_LABEL.equals(file.get("label"))),
                "The additional log entry is missing from the rendered configuration: " + files);
    }

    private Map<String, Object> renderAndParseWorkspaceMetadata(Map<String, Object> fileCollector) throws IOException {
        return (Map<String, Object>) renderAndParseCollector(fileCollector).get("additionalWorkspaceMetadata");
    }

    private Map<String, Object> renderAndParseCollector(Map<String, Object> fileCollector) throws IOException {
        String rendered = render(fileCollector);
        Map<String, Object> parsed = new Yaml().load(rendered);
        return (Map<String, Object>) parsed.get("collector");
    }

    private String render(Map<String, Object> fileCollector) throws IOException {
        // the settings.sls imports resolve pillars and grains through the salt runtime, the contexts they build are provided directly instead
        String template = FileReaderUtils.readFileFromClasspath(TEMPLATE_PATH).replaceAll(SETTINGS_IMPORT_PATTERN, "");

        Jinjava jinjava = new Jinjava();
        jinjava.getGlobalContext().registerFilter(new SaltJsonFilter());
        RenderResult renderResult = jinjava.renderForResult(template, Map.of(
                "destination", "LOCAL",
                "filecollector", fileCollector,
                "telemetry", telemetryContext(),
                "fluent", Map.of()));

        assertTrue(renderResult.getErrors().isEmpty(), "Jinja rendering failed for " + TEMPLATE_PATH + ": " + renderResult.getErrors());
        return renderResult.getOutput();
    }

    private Map<String, Object> fileCollectorContext() {
        Map<String, Object> fileCollector = new HashMap<>();
        fileCollector.put("clusterType", "DATALAKE");
        fileCollector.put("clusterVersion", "7.3.2");
        fileCollector.put("uuid", "00000000-0000-0000-0000-000000000000");
        fileCollector.put("hostname", "master0.example.site");
        fileCollector.put("accountId", "accountId");
        fileCollector.put("creatorCrn", "crn:cdp:iam:us-west-1:accountId:user:creator");
        fileCollector.put("resourceCrn", "crn:cdp:datalake:us-west-1:accountId:datalake:resource");
        fileCollector.put("environmentCrn", "crn:cdp:environments:us-west-1:accountId:environment:env");
        return fileCollector;
    }

    private Map<String, Object> telemetryContext() {
        return Map.of(
                "logs", List.of(Map.of("path", "/var/log/cloudera-scm-server/*.log", "label", "cm-server")),
                "clusterName", "test-datalake",
                "anonymizationRules", List.of());
    }

    private static class SaltJsonFilter implements Filter {

        private final ObjectMapper objectMapper = new ObjectMapper();

        @Override
        public String getName() {
            return "json";
        }

        @Override
        public Object filter(Object var, JinjavaInterpreter interpreter, String... args) {
            try {
                return objectMapper.writeValueAsString(var);
            } catch (Exception e) {
                throw new IllegalStateException("Unable to serialize value to JSON: " + var, e);
            }
        }
    }

}
