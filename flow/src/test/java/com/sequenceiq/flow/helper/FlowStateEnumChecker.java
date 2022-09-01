package com.sequenceiq.flow.helper;

import static org.junit.jupiter.api.Assertions.fail;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.reflections.Reflections;
import org.reflections.scanners.SubTypesScanner;

import com.sequenceiq.flow.core.FlowState;

public class FlowStateEnumChecker {

    private static final String BASE_PACKAGE = "com.sequenceiq.";

    private final List<String> validationErrors = new ArrayList<>();

    public void checkFlowStateClasses() {
        Reflections reflections = new Reflections(BASE_PACKAGE, new SubTypesScanner(true));
        Set<Class<? extends FlowState>> flowStateClasses = reflections.getSubTypesOf(FlowState.class);
        flowStateClasses.forEach(this::performCheck);
        if (!validationErrors.isEmpty()) {
            fail("There are " + validationErrors.size() + " violations:\n" + String.join("\n", validationErrors));
        }
    }

    private void performCheck(Class<? extends FlowState> clazz) {
        if (!clazz.getName().contains("Test") && !clazz.isEnum()) {
            validationErrors.add(String.format("Class %s is not an enum. Implementers of FlowState must be enums.", clazz.getName()));
        }
    }

}
