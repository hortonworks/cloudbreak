package com.sequenceiq.flow.helper;

import static org.junit.jupiter.api.Assertions.fail;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.apache.commons.lang3.ClassUtils;
import org.reflections.Reflections;
import org.reflections.scanners.SubTypesScanner;

import com.sequenceiq.cloudbreak.common.event.Acceptable;

public class FlowPayloadConstructorChecker {

    private final Set<Class<?>> checked = new HashSet<>();

    private final List<String> validationErrors = new ArrayList<>();

    public void checkConstructorOnAcceptableClasses() {
        Reflections reflections = new Reflections("com.sequenceiq", new SubTypesScanner(true));
        Set<Class<? extends Acceptable>> eventClasses = reflections.getSubTypesOf(Acceptable.class);
        eventClasses.stream()
                .filter(c -> !c.isInterface() && !isAbstract(c) && !c.isAnonymousClass() && !c.isLocalClass() && !c.isMemberClass())
                .filter(c -> !c.getName().endsWith("Test") && !c.getName().endsWith("Builder"))
                .forEach(this::checkForConstructorAndCache);
        if (!validationErrors.isEmpty()) {
            fail(String.join("\n", validationErrors));
        }
    }

    private void checkForConstructorAndCache(Class<?> clazz) {
        if (!checked.contains(clazz)) {
            checked.add(clazz);
            checkForConstructor(clazz);
        }
    }

    private void checkForConstructor(Class<?> clazz) {
        Constructor<?>[] constructors = clazz.getDeclaredConstructors();
        checkDefaultConstructor(clazz, constructors);
        checkSoloBuilder(clazz, constructors);

        Optional<Constructor<?>> maxParamCtor = getConstructorWithMaximalPositiveParameterCount(constructors);
        if (maxParamCtor.isPresent()) {
            for (Class<?> paramType : maxParamCtor.get().getParameterTypes()) {
                if (paramType.getPackageName().startsWith("com.sequenceiq.") &&
                        !paramType.isEnum() &&
                        !ClassUtils.isPrimitiveOrWrapper(paramType)) {

                    checkForConstructorAndCache(paramType);
                }
            }
        }
    }

    private void checkDefaultConstructor(Class<?> clazz, Constructor<?>[] constructors) {
        if (hasDefaultConstructor(constructors)) {
            List<String> setterNames = checkAllSetters(clazz);
            if (!setterNames.isEmpty()) {
                validationErrors.add(String.format("Class %s with default constructor has setter(s) [%s] with no corresponding backing field(s)",
                        clazz.getName(), String.join(",", setterNames)));
            }

            List<String> fieldNames = checkAllPrivateFields(clazz);
            if (!fieldNames.isEmpty()) {
                validationErrors.add(String.format("Class %s with default constructor has field(s) [%s] with no setter",
                        clazz.getName(), String.join(",", fieldNames)));
            }
        }
    }

    private boolean hasDefaultConstructor(Constructor<?>[] constructors) {
        for (Constructor<?> c : constructors) {
            if (c.getParameterCount() == 0) {
                return true;
            }
        }
        return false;
    }

    private void checkSoloBuilder(Class<?> clazz, Constructor<?>[] constructors) {
        if (constructors.length == 1 &&
                constructors[0].getParameterCount() == 1 &&
                getBuilderClass(clazz).isPresent() &&
                constructors[0].getParameterTypes()[0].equals(getBuilderClass(clazz).get())) {

            validationErrors.add(
                    String.format("Class %s has only a constructor for its builder thus it cannot be deserialized without proper annotation", clazz.getName()));
        }
    }

    private Optional<Constructor<?>> getConstructorWithMaximalPositiveParameterCount(Constructor<?>[] declaredConstructors) {
        int maxParam = 0;
        Constructor<?> maxParamCtor = null;
        for (Constructor<?> c : declaredConstructors) {
            if (c.getParameterCount() > maxParam) {
                maxParam = c.getParameterCount();
                maxParamCtor = c;
            }
        }
        return Optional.ofNullable(maxParamCtor);
    }

    private Optional<Class<?>> getBuilderClass(Class<?> clazz) {
        for (Class<?> innerClass : clazz.getDeclaredClasses()) {
            if (isStatic(innerClass) && isPublic(innerClass) &&
                    innerClass.getSimpleName().toLowerCase().contains("builder")) {
                return Optional.of(innerClass);
            }
        }
        return Optional.empty();
    }

    private List<String> checkAllSetters(Class<?> clazz) {
        List<String> setterNames = new ArrayList<>();
        for (Method m : clazz.getDeclaredMethods()) {
            if (isSetterForClass(m)) {
                boolean fieldPresent = isFieldForSetterPresent(clazz, m.getName());
                if (!fieldPresent) {
                    setterNames.add(m.getName());
                }
            }
        }
        return setterNames;
    }

    private List<String> checkAllPrivateFields(Class<?> clazz) {
        List<String> fieldNames = new ArrayList<>();
        for (Field f : clazz.getDeclaredFields()) {
            if (!Modifier.isFinal(f.getModifiers())) {
                boolean setterPresent = isSetterForFieldPresent(clazz, f.getName());
                if (!setterPresent) {
                    fieldNames.add(f.getName());
                }
            }
        }
        return fieldNames;
    }

    private boolean isSetterForFieldPresent(Class<?> clazz, String fieldName) {
        String expectedSetterName = "set" + fieldName.substring(0, 1).toUpperCase() + fieldName.substring(1);
        for (Method m : clazz.getDeclaredMethods()) {
            if (expectedSetterName.equals(m.getName())) {
                return true;
            }
        }
        return false;
    }

    private boolean isFieldForSetterPresent(Class<?> clazz, String setterName) {
        String expectedBackingFieldName = setterName.substring(3, 4).toLowerCase() + setterName.substring(4);
        for (Field f : clazz.getDeclaredFields()) {
            if (f.getName().equals(expectedBackingFieldName)) {
                return true;
            }
        }
        return false;
    }

    public static boolean isSetterForClass(Method method) {
        if (!method.getName().startsWith("set")) {
            return false;
        }
        return method.getParameterTypes().length == 1;
    }

    private boolean isAbstract(Class<?> clazz) {
        return Modifier.isAbstract(clazz.getModifiers());
    }

    private boolean isPublic(Class<?> clazz) {
        return Modifier.isPublic(clazz.getModifiers());
    }

    private boolean isStatic(Class<?> clazz) {
        return Modifier.isStatic(clazz.getModifiers());
    }

}
