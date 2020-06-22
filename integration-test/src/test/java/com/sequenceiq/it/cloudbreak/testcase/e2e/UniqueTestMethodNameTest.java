package com.sequenceiq.it.cloudbreak.testcase.e2e;

import static org.assertj.core.api.Fail.fail;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.reflections.Reflections;
import org.reflections.scanners.MethodAnnotationsScanner;
import org.testng.annotations.Test;

public class UniqueTestMethodNameTest {

    private static final String DUPLICATE_METHOD_NAME_ERROR_MESSAGE = "Method name [%s] is present in more than one classes: %s";

    /**
     * As we are using test method names as tags on cloud resources, we must be sure that they are unique.
     */
    @Test
    public void testMethodNamesShouldBeUnique() {
        Reflections reflections = new Reflections(UniqueTestMethodNameTest.class.getPackageName(), new MethodAnnotationsScanner());
        Map<String, List<Method>> methodsByName = reflections.getMethodsAnnotatedWith(Test.class).stream()
                .collect(Collectors.groupingBy(Method::getName));

        String errorMessage = methodsByName.entrySet()
                .stream()
                .filter(methodsEntry -> methodsEntry.getValue().size() > 1)
                .map(methodsEntry -> getErrorMessage(methodsEntry.getKey(), methodsEntry.getValue()))
                .collect(Collectors.joining(System.lineSeparator()));

        if (!errorMessage.isEmpty()) {
            fail(errorMessage);
        }
    }

    private String getErrorMessage(String methodName, List<Method> methodsWithSameName) {
        String classNames = methodsWithSameName.stream()
                .map(method -> method.getDeclaringClass().getCanonicalName())
                .collect(Collectors.joining(", "));
        return String.format(DUPLICATE_METHOD_NAME_ERROR_MESSAGE, methodName, classNames);
    }
}
