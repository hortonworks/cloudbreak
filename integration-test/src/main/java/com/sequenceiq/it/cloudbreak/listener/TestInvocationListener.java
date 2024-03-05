package com.sequenceiq.it.cloudbreak.listener;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.apache.commons.io.FileUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.IInvokedMethod;
import org.testng.IInvokedMethodListener;
import org.testng.ITestResult;

import com.sequenceiq.cloudbreak.common.json.JsonUtil;
import com.sequenceiq.it.cloudbreak.context.CompareByOrder;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.CloudbreakTestDto;
import com.sequenceiq.it.cloudbreak.dto.environment.EnvironmentTestDto;
import com.sequenceiq.it.cloudbreak.dto.freeipa.FreeIpaTestDto;
import com.sequenceiq.it.cloudbreak.dto.sdx.SdxInternalTestDto;

public class TestInvocationListener implements IInvokedMethodListener {
    private static final Logger LOGGER = LoggerFactory.getLogger(TestInvocationListener.class);

    @Override
    public void beforeInvocation(IInvokedMethod invokedMethod, ITestResult testResult) {
        LOGGER.info("Before Invocation of: " + invokedMethod.getTestMethod().getMethodName()
                + " with parameters: " + Arrays.toString(testResult.getParameters()));
        TestContext testContext;
        Object[] parameters = testResult.getParameters();
        if (parameters == null || parameters.length == 0) {
            LOGGER.warn("Test context could not be found because parameters array is empty in test result.");
            return;
        }
        try {
            testContext = (TestContext) parameters[0];
        } catch (ClassCastException e) {
            LOGGER.warn("Test context could not be casted from test result parameters.");
            return;
        }
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(Paths.get(""))) {
            for (Path path : stream) {
                String fileName = path.getFileName().toString();
                if (fileName.contains("resource_names_" + testContext.getTestMethodName().orElseGet(this::getDefaultFileNameTag) + ".json")) {
                    Map<String, String> resourceNameMap =
                            JsonUtil.readValue(FileUtils.readFileToString(Paths.get(fileName).toFile(), Charset.defaultCharset()), Map.class);
                    resourceNameMap.computeIfPresent(EnvironmentTestDto.ENVIRONMENT_RESOURCE_NAME,
                            (key, name) -> testContext.getExistingResourceNames().put(EnvironmentTestDto.class, name));
                    resourceNameMap.computeIfPresent(FreeIpaTestDto.FREEIPA_RESOURCE_NAME,
                            (key, name) -> testContext.getExistingResourceNames().put(FreeIpaTestDto.class, name));
                    resourceNameMap.computeIfPresent(SdxInternalTestDto.SDX_RESOURCE_NAME,
                            (key, name) -> testContext.getExistingResourceNames().put(SdxInternalTestDto.class, name));
                }
            }
        } catch (Exception e) {
            LOGGER.error("Failed to read resource names from file: ", e);
        }
    }

    /**
     * Collects all the available test resources to a JSON file based on the:
     * - Related test DTO: provides a resource name type (eg.: environmentName),
     * - Test Context: provides the resource's unique name (eg.: aws-test-039df4d8aec04a61965).
     *
     * Resource files can be saved based on the test cases (eg.: resource_names_testCreateNewEnvironmentWithNewNetworkAndNoInternet);
     * because of each test has it's own Test Context, that has been built by the Given test steps for that thread of test execution.
     */
    @Override
    public void afterInvocation(IInvokedMethod invokedMethod, ITestResult testResult) {
        TestContext testContext;
        JSONObject jsonObject = new JSONObject();
        Object[] parameters = testResult.getParameters();
        if (parameters == null || parameters.length == 0) {
            LOGGER.warn("Test context could not be found because parameters array is empty in test result.");
            return;
        }
        try {
            testContext = (TestContext) parameters[0];
        } catch (ClassCastException e) {
            LOGGER.warn("Test context could not be casted from test result parameters.");
            return;
        }

        removeOldResourceFile(testContext);

        List<CloudbreakTestDto> testDtos = new ArrayList<>(testContext.getResourceNames().values());
        List<CloudbreakTestDto> orderedTestDtos = testDtos.stream().sorted(new CompareByOrder()).collect(Collectors.toList());
        for (CloudbreakTestDto testDto : orderedTestDtos) {
            try {
                String resourceNameType = Optional.ofNullable(testDto.getResourceNameType()).orElse("");
                if (!resourceNameType.trim().isEmpty()) {
                    if (jsonObject.has(resourceNameType)) {
                        List<String> resourceNames = new ArrayList<>();
                        try {
                            JSONArray resources = jsonObject.getJSONArray(resourceNameType);
                            for (int i = 0; i < resources.length(); i++) {
                                String resource = resources.getString(i);
                                resourceNames.add(resource);
                            }
                        } catch (JSONException e) {
                            String resource = jsonObject.getString(resourceNameType);
                            resourceNames.add(resource);
                        }
                        resourceNames.add(testDto.getName());
                        LOGGER.info("Created resource name array: '{}'.", resourceNames);
                        JSONArray resourceNameArray = new JSONArray(resourceNames);
                        jsonObject.put(resourceNameType, resourceNameArray);
                    } else {
                        jsonObject.put(resourceNameType, testDto.getName());
                    }
                    LOGGER.info("Put Resource Name: '{}' to JSON Object.", testDto.getName());
                }
            } catch (Exception e) {
                LOGGER.info("Appending JSON object is failing, because of: {}", e.getMessage(), e);
            }
        }
        if (jsonObject.length() != 0) {
            String fileName = "resource_names_" + testContext.getTestMethodName().orElseGet(this::getDefaultFileNameTag) + ".json";
            try {
                Path path = Paths.get(fileName);
                Files.writeString(path, jsonObject.toString());
                LOGGER.info("Resource file have been created with name: {}, path: {} and content: {}.", fileName, path.toAbsolutePath(), jsonObject);
            } catch (IOException e) {
                LOGGER.info("Creating/Appending resource file throws exception: {}", e.getMessage(), e);
            } catch (Exception e) {
                LOGGER.info("Creating/Appending resource file is failing, because of: {}", e.getMessage(), e);
            }
        } else {
            LOGGER.info("No resources found, no output file needs to be created.");
        }
    }

    private void removeOldResourceFile(TestContext testContext) {
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(Paths.get(""))) {
            for (Path path : stream) {
                String fileName = path.getFileName().toString();
                if (fileName.contains("resource_names_" + testContext.getTestMethodName().orElseGet(this::getDefaultFileNameTag) + ".json")) {
                    try {
                        FileTime creationTime = Files.readAttributes(Path.of(fileName), BasicFileAttributes.class).creationTime();
                        if (path.toFile().delete()) {
                            LOGGER.info("Old resource file: {} (creation time: {}) have been found and deleted at: {}.",
                                    fileName, creationTime, path.toAbsolutePath());
                        } else {
                            LOGGER.info("Old resource file: {} (creation time: {}) have NOT been deleted at: {}.",
                                    fileName, creationTime, path.toAbsolutePath());
                        }
                    } catch (Exception e) {
                        LOGGER.info("{} resource file cleanup has been failed, because of: {}", fileName, e.getMessage(), e);
                    }
                }
            }
        } catch (IOException e) {
            LOGGER.error("Failed to remove old resource file: ", e);
        }
    }

    private String getDefaultFileNameTag() {
        return "unknown";
    }
}
