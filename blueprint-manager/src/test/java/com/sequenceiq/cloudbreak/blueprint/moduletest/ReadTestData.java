package com.sequenceiq.cloudbreak.blueprint.moduletest;

import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.sequenceiq.cloudbreak.blueprint.BlueprintPreparationObject;
import com.sequenceiq.cloudbreak.blueprint.testrepeater.TestFile;
import com.sequenceiq.cloudbreak.util.FileReaderUtils;

public final class ReadTestData {

    private static final String BLUEPRINT_UPDATER_TEST_INPUTS = "module-test/inputs";

    private static final String BLUEPRINT_UPDATER_TEST_OUTPUTS = "module-test/outputs";

    private ReadTestData() {
    }

    public static List<BlueprintDataProvider> getInputOutputData(Map<String, BlueprintPreparationObject> models) throws IOException {
        final List<Path> inputFiles = getPathsFromClasspathFolder(BLUEPRINT_UPDATER_TEST_INPUTS);
        final List<Path> outputFiles = getPathsFromClasspathFolder(BLUEPRINT_UPDATER_TEST_OUTPUTS);

        final List<BlueprintDataProvider> testFiles = new LinkedList<>();

        // starts from 1 because the first entry the folder name itself
        for (int i = 1; i < inputFiles.size(); i++) {
            String inputFileName = getFileNameWithoutExtension(inputFiles.get(i));
            for (int j = 1; j < outputFiles.size(); j++) {
                if (getFileNameWithoutExtension(outputFiles.get(j)).equals(inputFileName)) {
                    testFiles.add(new BlueprintDataProvider(
                            new TestFile(inputFiles.get(i), FileReaderUtils.readFileFromClasspath(combinePathFromSourceAndFile(BLUEPRINT_UPDATER_TEST_INPUTS,
                                    inputFiles.get(i)))),
                            new TestFile(outputFiles.get(j), FileReaderUtils.readFileFromClasspath(combinePathFromSourceAndFile(BLUEPRINT_UPDATER_TEST_OUTPUTS,
                                    outputFiles.get(j)))),
                            createBpPrepObjectBasedOnActualPath(inputFiles.get(i), models)));
                    break;
                }
            }
        }
        return testFiles;
    }

    private static BlueprintPreparationObject createBpPrepObjectBasedOnActualPath(Path path, Map<String, BlueprintPreparationObject> models) {
        String fileNameWithExtension = getFileNameWithoutExtension(path);
        if (models.containsKey(fileNameWithExtension)) {
            return models.get(fileNameWithExtension);
        } else {
            throw new CentralBlueprintUpdateTestPreparationException(String.format("There is no configuration model for the <%s> file", fileNameWithExtension));
        }
    }

    private static String combinePathFromSourceAndFile(String strPathToResourceFolder, Path actualFileAsPath) {
        return strPathToResourceFolder + System.getProperty("file.separator") + actualFileAsPath.toFile().getName();
    }

    private String getFileNameFromPath(Path pathToFile) {
        String fileName = pathToFile.toFile().getName();
        return fileName.substring(0, fileName.lastIndexOf('.'));
    }

    private static String getPath(String path) throws IOException {
        URL pathToResource = ReadTestData.class.getClassLoader().getResource(path);
        if (pathToResource != null) {
            return pathToResource.getPath();
        } else {
            throw new IOException(String.format("Unalble to locate the desired folder/file in the classpath <%s>", path));
        }
    }

    private static List<Path> getPathsFromClasspathFolder(String path) throws IOException {
        final List<Path> paths = new LinkedList<>();
        try (Stream<Path> pathStream = Files.walk(Paths.get(getPath(path)))) {
            paths.addAll(pathStream.distinct().collect(Collectors.toList()));
        }
        return paths;
    }

    private static String getFileNameWithoutExtension(Path path) {
        String fileName = path.toFile().getName();
        return fileName.substring(0, fileName.indexOf('.'));
    }

}
