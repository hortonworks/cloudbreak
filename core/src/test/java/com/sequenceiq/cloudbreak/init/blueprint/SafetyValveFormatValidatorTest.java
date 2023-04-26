package com.sequenceiq.cloudbreak.init.blueprint;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.DirectoryFileFilter;
import org.apache.commons.io.filefilter.RegexFileFilter;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.junit.jupiter.MockitoExtension;

import com.fasterxml.jackson.databind.JsonNode;
import com.sequenceiq.cloudbreak.cmtemplate.CmTemplateProcessor;
import com.sequenceiq.cloudbreak.cmtemplate.utils.BlueprintUtils;
import com.sequenceiq.cloudbreak.cmtemplate.validation.SafetyValveValidator;
import com.sequenceiq.cloudbreak.json.JsonHelper;

@ExtendWith(MockitoExtension.class)
public class SafetyValveFormatValidatorTest {

    private BlueprintUtils blueprintUtils;

    private JsonHelper jsonHelper;

    private SafetyValveValidator safetyValveValidator;

    @BeforeEach
    public void setUp() throws Exception {
        blueprintUtils = new BlueprintUtils();
        jsonHelper = new JsonHelper();
        safetyValveValidator = new SafetyValveValidator();
    }

    private static Stream<Arguments> provideArguments() throws IOException {
        Map<String, List<String>> allTemplates = getTemplates();
        List<Arguments> arguments = new ArrayList<>();
        allTemplates.entrySet().stream().forEach(entry -> {
            entry.getValue().stream().forEach(value -> {
                arguments.add(Arguments.of(String.format("Validate template %s for version %s", value, entry.getKey()), entry.getKey(), value));
            });
        });
        return arguments.stream();
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("provideArguments")
    public void testSafetyValves(String testCaseName, String version, String template) throws Exception {
        JsonNode jsonNode = null;
        try {
            jsonNode = jsonHelper.createJsonFromString(
                    blueprintUtils.readDefaultBlueprintFromFile(version, new String[]{"desc", template}));
        } catch (IOException e) {
            Assertions.fail(String.format("Unable to read template, %s", e.getMessage()));
        }
        String bluePrintText = jsonNode.get("blueprint").toString();
        new CmTemplateProcessor(bluePrintText).getSafetyValves().forEach(safetyValve -> {
            try {
                safetyValveValidator.validate(safetyValve);
            } catch (Exception ex) {
                Assertions.fail(ex.getMessage());
            }
        });
    }

    private static Map<String, List<String>> getTemplates() throws IOException {
        Map<String, List<String>> templates = new HashMap<>();
        String defaultTemplateDir = "src/main/resources/defaults/blueprints";
        File file = new File(defaultTemplateDir);
        Collection<File> files = FileUtils.listFiles(
                file,
                new RegexFileFilter("^(.*.bp)"),
                DirectoryFileFilter.DIRECTORY
        );
        for (File template : files) {
            String [] tokens = template.getAbsolutePath().split("/");
            if (tokens.length >= 2) {
                templates.putIfAbsent(tokens[tokens.length - 2], new ArrayList<>());
                templates.get(tokens[tokens.length - 2]).add(StringUtils.substringBeforeLast(tokens[tokens.length - 1], "."));
            }
        }
        return templates;
    }
}
