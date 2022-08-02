package com.sequenceiq.it.cloudbreak.assertion.freeipa;

import static java.lang.String.format;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.common.api.command.RemoteCommandsExecutionResponse;
import com.sequenceiq.it.cloudbreak.FreeIpaClient;
import com.sequenceiq.it.cloudbreak.assertion.Assertion;
import com.sequenceiq.it.cloudbreak.dto.freeipa.FreeIpaRemoteTestDto;
import com.sequenceiq.it.cloudbreak.exception.TestFailException;
import com.sequenceiq.it.cloudbreak.log.Log;

public class RecipeTestAssertion {

    private static final Logger LOGGER = LoggerFactory.getLogger(RecipeTestAssertion.class);

    private RecipeTestAssertion() {
    }

    public static Assertion<FreeIpaRemoteTestDto, FreeIpaClient> validateFilesOnFreeIpa(String filePath, String fileName, long requiredNumberOfFiles) {
        return (testContext, freeIpaRemoteTestDto, freeIpaClient) -> {
            RemoteCommandsExecutionResponse response = freeIpaRemoteTestDto.getResponse();

            Log.whenJson(LOGGER, String.format("The command '%s' result:%n", freeIpaRemoteTestDto.getRequest().getCommand()), response);
            response.getResults().forEach((instance, result) -> {
                if (StringUtils.isBlank(result)) {
                    Log.error(LOGGER, String.format("Cannot find any file at '%s' path on '%s' instance!", filePath, instance));
                    throw new TestFailException(format("Cannot find any file at '%s' path on '%s' instance!", filePath, instance));
                } else {
                    List<String> cmdOutputValues = Stream.of(result.split("[\\r\\n\\t]")).filter(Objects::nonNull).collect(Collectors.toList());
                    boolean fileFound = cmdOutputValues.stream()
                            .anyMatch(outputValue -> outputValue.strip().startsWith("/"));
                    String foundFilePath = cmdOutputValues.stream()
                            .filter(outputValue -> outputValue.strip().startsWith("/")).findFirst().orElse(null);
                    Log.log(LOGGER, format(" The file is present '%s' at '%s' path. ", fileFound, foundFilePath));

                    long fileCount = cmdOutputValues.stream().filter(outputValue -> outputValue.strip().startsWith("/")).count();

                    if (requiredNumberOfFiles == fileCount) {
                        Log.log(LOGGER, " Required number (%d) of files are available at '%s' on %s instance. ", fileCount, filePath, instance);
                    } else {
                        Log.error(LOGGER, "Required number ({}) of files are NOT available at '{}' on {} instance!",
                                requiredNumberOfFiles, filePath, instance);
                        throw new TestFailException(format("Required number (%d) of files are NOT available at '%s' on %s instance!", requiredNumberOfFiles,
                                filePath, instance));
                    }
                }
            });
            return freeIpaRemoteTestDto;
        };
    }
}
