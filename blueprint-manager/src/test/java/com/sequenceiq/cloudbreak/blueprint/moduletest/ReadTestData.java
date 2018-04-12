package com.sequenceiq.cloudbreak.blueprint.moduletest;

import com.sequenceiq.cloudbreak.blueprint.testrepeater.TestFile;
import com.sequenceiq.cloudbreak.templateprocessor.processor.TemplatePreparationObject;
import com.sequenceiq.cloudbreak.util.FileReaderUtils;

import java.io.File;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public final class ReadTestData {

    private static final String BLUEPRINT_UPDATER_TEST_INPUTS = "module-test/inputs";

    private static final String BLUEPRINT_UPDATER_TEST_OUTPUTS = "module-test/outputs";

    private ReadTestData() {
    }

    public static List<BlueprintDataProvider> getInputOutputData(Map<String, TemplatePreparationObject> models) throws IOException {
        final List<BlueprintDataProvider> testFiles = new LinkedList<>();
        for (Map.Entry<String, TemplatePreparationObject> entry : models.entrySet()) {
            try {
                TestFile inputFile = getTestFile(getFileName(BLUEPRINT_UPDATER_TEST_INPUTS, entry.getKey()));
                TestFile outputFile = getTestFile(getFileName(BLUEPRINT_UPDATER_TEST_OUTPUTS, entry.getKey()));
                testFiles.add(new BlueprintDataProvider(inputFile, outputFile, entry.getValue()));
            } catch (Exception ex) {
                throw new IOException(String.format("Unable to locate the desired folder/file in the classpath <%s>, message: %s", entry.getKey(),
                        ex.getMessage()));
            }
        }
        return testFiles;
    }

    private static TestFile getTestFile(String fileName) throws IOException {
        return new TestFile(new File(fileName).toPath(), FileReaderUtils.readFileFromClasspath(fileName));
    }

    private static String getFileName(String folder, String filename) {
        return folder + "/" + filename + ".bp";
    }

}
