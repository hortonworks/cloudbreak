package com.sequenceiq.flow.event;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;

import org.reflections.Reflections;
import org.reflections.scanners.SubTypesScanner;

import com.sequenceiq.flow.core.FlowEvent;

public class FlowSelectors {

    public void assertUniquenessInFlowEventNames(String packagePrefix) {
        Reflections reflections = new Reflections(packagePrefix, new SubTypesScanner());
        Set<Class<? extends FlowEvent>> eventClasses = reflections.getSubTypesOf(FlowEvent.class);
        assertThat(eventClasses)
                .withFailMessage("No FlowEvent subtypes found in package %s. Please check your 'packagePrefix' parameter.", packagePrefix)
                .isNotEmpty();
        Map<String, List<String>> names = new HashMap<>();
        eventClasses.forEach(enumClass -> Arrays.stream(enumClass.getEnumConstants())
                .forEach(item -> names.compute(item.event(), (eventName, enumList) -> enumList == null
                        ? new ArrayList<>(Arrays.asList(enumClass.getSimpleName()))
                        : addToListAndReturn(enumList, enumClass.getSimpleName()))));
        Map<String, List<String>> duplicates = names.entrySet().stream()
                .filter(e -> e.getValue().size() > 1)
                .collect(Collectors.toMap(Entry::getKey, Entry::getValue, this::mergeLists, TreeMap::new));

        assertThat(duplicates)
                .withFailMessage(() -> printDuplicates(duplicates))
                .isEmpty();
    }

    private String printDuplicates(Map<String, List<String>> duplicates) {
        StringBuilder failMessage = new StringBuilder("The following enum event() values are present in more than one FlowEvent enum:\n");
        duplicates.forEach((enumValue, list) -> failMessage.append(String.format("%s in %s%n", enumValue, list)));
        return failMessage.toString();
    }

    private List<String> mergeLists(List<String> list1, List<String> list2) {
        list1.addAll(list2);
        return list1;
    }

    private List<String> addToListAndReturn(List<String> list, String enumClassName) {
        list.add(enumClassName);
        return list;
    }

}
