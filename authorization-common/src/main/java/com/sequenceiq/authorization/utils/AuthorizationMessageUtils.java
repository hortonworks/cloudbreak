package com.sequenceiq.authorization.utils;

import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.lang3.tuple.Pair;

public class AuthorizationMessageUtils {

    private AuthorizationMessageUtils() {
    }

    public static String formatIdentifiersForErrorMessage(List<String> crns, Function<String, Optional<String>> nameMapper) {
        return crns.stream().map(crn -> Pair.of(crn, nameMapper.apply(crn)))
                .map(crnAndName -> {
                            String resourceNameFormatted = crnAndName.getRight()
                                    .map(name -> String.format("name: %s", name)).orElse("");
                            String resourceCrnFormatted = String.format("crn: %s", crnAndName.getLeft());
                            return Stream.of(resourceNameFormatted, resourceCrnFormatted)
                                    .filter(part -> !part.isEmpty())
                                    .collect(Collectors.joining(", "));
                        }
                ).map(nameAndCrnFormatted -> String.format("[%s]", nameAndCrnFormatted))
                .collect(Collectors.joining(" "));
    }
}
