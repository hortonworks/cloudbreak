package com.sequenceiq.flow.helper;

import static org.junit.jupiter.api.Assertions.fail;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Parameter;
import java.lang.reflect.Type;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Deque;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.Queue;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.SetUtils;
import org.apache.commons.lang3.StringUtils;
import org.reflections.Reflections;
import org.reflections.scanners.SubTypesScanner;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import com.sequenceiq.cloudbreak.common.event.Acceptable;
import com.sequenceiq.cloudbreak.common.event.FlowPayload;
import com.sequenceiq.cloudbreak.common.json.JsonIgnoreDeserialization;

public class FlowPayloadSerializabilityChecker {

    private static final String BASE_PACKAGE = "com.sequenceiq.";

    private final Set<Class<?>> checked = new HashSet<>();

    private final List<String> validationErrors = new ArrayList<>();

    private final Deque<String> parentClasses = new ArrayDeque<>();

    public void checkAcceptableClasses() {
        Reflections reflections = new Reflections(BASE_PACKAGE, new SubTypesScanner(true));
        Set<Class<? extends Acceptable>> acceptableClasses = reflections.getSubTypesOf(Acceptable.class);
        Set<Class<?>> filteredAcceptables = getClassesToCheck(acceptableClasses);
        Set<Class<? extends FlowPayload>> flowPayloadClasses = reflections.getSubTypesOf(FlowPayload.class);
        Set<Class<?>> flowPayloads = getClassesToCheck(flowPayloadClasses);
        Set<Class<?>> allClassesToCheck = SetUtils.union(filteredAcceptables, flowPayloads);
        allClassesToCheck.forEach(this::performChecksCache);
        if (!validationErrors.isEmpty()) {
            fail("There are " + validationErrors.size() + " violations:\n" + String.join("\n", validationErrors));
        }
    }

    private <T> Set<Class<?>> getClassesToCheck(Set<Class<? extends T>> eventClasses) {
        return eventClasses.stream()
                .filter(c -> !c.isInterface() && !isAbstract(c) && !c.isAnonymousClass() && !c.isLocalClass() && !c.isMemberClass())
                .filter(c -> !c.getName().endsWith("Test") && !c.getName().endsWith("Builder"))
                .collect(Collectors.toSet());
    }

    private void performChecksCache(Class<?> clazz) {
        if (!checked.contains(clazz)) {
            checked.add(clazz);
            performChecks(clazz);
        }
    }

    private void performChecks(Class<?> clazz) {
        Constructor<?>[] constructors = clazz.getDeclaredConstructors();
        boolean hasDeserializeAttributeWithBuilder = checkJacksonDeserializeAnnotationForBuilder(clazz);
        checkMissingGetters(clazz);
        checkMissingSetters(clazz);
        boolean hasDefaultConstructor = hasDefaultConstructor(constructors);
        if (hasDefaultConstructor || hasDeserializeAttributeWithBuilder) {
            checkPrivateFields(clazz);
        }
        if (!hasDeserializeAttributeWithBuilder) {
            checkSoloBuilder(clazz, constructors);
            if (!hasDefaultConstructor) {
                checkJacksonCreatorAnnotation(clazz, constructors);
            }
        }
        checkParameters(clazz, constructors);
        checkForRecursiveRelations(clazz);
    }

    private void checkForRecursiveRelations(Class<?> clazz) {
        for (Field field : getInheritedDeclaredFields(clazz)) {
            if (!Modifier.isStatic(field.getModifiers())) {
                boolean oneToMany = field.getAnnotation(OneToMany.class) != null;
                boolean oneToOne = field.getAnnotation(OneToOne.class) != null;
                boolean noJsonIgnore = field.getAnnotation(JsonIgnore.class) == null;
                if (noJsonIgnore && (oneToMany || oneToOne)) {
                    checkOneToRelations(clazz, field, oneToMany);
                }
            }
        }
    }

    private void checkOneToRelations(Class<?> clazz, Field field, boolean oneToMany) {
        Class<?> otherParty = field.getType();
        for (Field otherPartyField : getInheritedDeclaredFields(otherParty)) {
            if (otherPartyField.getType().equals(clazz)) {
                boolean noOtherPartyJsonIgnore = otherPartyField.getAnnotation(JsonIgnore.class) == null;
                if (noOtherPartyJsonIgnore && (field.getAnnotation(JsonManagedReference.class) == null ||
                        otherPartyField.getAnnotation(JsonBackReference.class) == null)) {

                    validationErrors.add(decorateWitParentClasses(String.format(
                            "Class %s has @OneTo%s relation on field [%s] to %s but no @JsonManagedReference "
                                    + "or no @JsonBackReference on the referenced class' field [%s]. "
                                    + "This causes infinite recursion upon serialization.",
                            clazz.getName(), oneToMany ? "Many" : "One", field.getName(), otherParty.getName(), otherPartyField.getName())));
                }
            }
        }
    }

    private void checkPrivateFields(Class<?> clazz) {
        for (Field f : getInheritedDeclaredFields(clazz)) {
            if (!Modifier.isStatic(f.getModifiers())) {
                checkClassRecursive(clazz, f.getGenericType());
            }
        }
    }

    private void checkParameters(Class<?> clazz, Constructor<?>[] constructors) {
        Set<Type> paramTypes = getAllConstructorParams(constructors);
        for (Type paramType : paramTypes) {
            checkClassRecursive(clazz, paramType);
        }
    }

    private void checkClassRecursive(Class<?> ownerClass, Type paramType) {
        Set<Class<?>> paramClasses = GenericParameterTypeParser.parse(BASE_PACKAGE, paramType);
        paramClasses.forEach(paramClass -> {
            if (paramClass.getPackageName().startsWith(BASE_PACKAGE) &&
                    !paramClass.isEnum() && !paramClass.isInterface() && !paramClass.getName().endsWith("Builder")) {
                parentClasses.add(ownerClass.getName());
                performChecksCache(paramClass);
                parentClasses.removeLast();
            }
        });
    }

    private boolean checkJacksonDeserializeAnnotationForBuilder(Class<?> clazz) {
        JsonDeserialize jsonDeserAnnotation = clazz.getAnnotation(JsonDeserialize.class);
        if (jsonDeserAnnotation != null && !Objects.equals(jsonDeserAnnotation.builder(), Void.class)) {
            Class<?> deserializerClass = jsonDeserAnnotation.builder();
            Optional<Class<?>> builderClassOpt = getBuilderClass(clazz);
            if (builderClassOpt.isEmpty()) {
                validationErrors.add(decorateWitParentClasses(String.format(
                        "Class %s has @JsonDeserialize attribute with Builder %s but it does not have a nested builder class.",
                        clazz.getName(), deserializerClass.getName())));
                return false;
            }
            if (!builderClassOpt.get().equals(deserializerClass)) {
                validationErrors.add(decorateWitParentClasses(String.format(
                        "Class %s has @JsonDeserialize attribute with Builder %s but it does not match the nested builder class %s.",
                        clazz.getName(), deserializerClass.getName(), builderClassOpt.get().getName())));
                return false;
            }
            checkClassForJsonPojoBuilder(clazz, builderClassOpt.get());
            return true;
        }
        return false;
    }

    private void checkJacksonCreatorAnnotation(Class<?> clazz, Constructor<?>[] constructors) {
        Set<Constructor<?>> creators = Arrays.stream(constructors).filter(c -> c.getAnnotation(JsonCreator.class) != null).collect(Collectors.toSet());
        if (creators.isEmpty()) {
            validationErrors.add(decorateWitParentClasses(String.format("Class %s has no constructor with @JsonCreator attribute.", clazz.getName())));
        } else if (creators.size() > 1) {
            validationErrors.add(decorateWitParentClasses(
                    String.format("Class %s has more than 1 constructors with @JsonCreator attribute, ambiguity.", clazz.getName())));
        } else {
            Constructor<?> creator = creators.iterator().next();
            checkCreator(clazz, creator);
        }
    }

    private void checkCreator(Class<?> clazz, Constructor<?> creator) {
        List<String> missingJsonPropertyParams = new ArrayList<>();
        Set<String> uniqueNamesCheck = new HashSet<>();
        Set<String> duplicateNames = new HashSet<>();
        Set<String> missingGetters = new HashSet<>();
        int jsonPropertyCount = 0;
        for (Parameter param : creator.getParameters()) {
            JsonIgnoreDeserialization ignoreDeserialization = param.getAnnotation(JsonIgnoreDeserialization.class);
            JsonProperty jsonProperty = param.getAnnotation(JsonProperty.class);
            if (jsonProperty == null || StringUtils.isBlank(jsonProperty.value())) {
                missingJsonPropertyParams.add(param.getName());
            } else {
                jsonPropertyCount++;
                if (uniqueNamesCheck.contains(jsonProperty.value())) {
                    duplicateNames.add(jsonProperty.value());
                } else {
                    uniqueNamesCheck.add(jsonProperty.value());
                    if (ignoreDeserialization == null) {
                        if (!isGetterFoundRecursive(clazz, jsonProperty.value(), isBooleanType(param.getType()))) {
                            missingGetters.add(jsonProperty.value());
                        }
                    }
                }
            }
        }
        if (!missingJsonPropertyParams.isEmpty()) {
            validationErrors.add(decorateWitParentClasses(String.format(
                    "Class %s has a @JsonCreator constructor with parameters [%s] missing the @JsonProperty annotation.",
                    clazz.getName(), String.join(",", missingJsonPropertyParams))));
        }
        if (uniqueNamesCheck.size() != jsonPropertyCount) {
            validationErrors.add(decorateWitParentClasses(String.format("Class %s has a @JsonCreator constructor with duplicate @JsonProperty values [%s].",
                    clazz.getName(), String.join(",", duplicateNames))));
        }
        if (!missingGetters.isEmpty()) {
            validationErrors.add(decorateWitParentClasses(String.format(
                    "Class %s has a @JsonCreator constructor with @JsonProperty but no matching getters for values [%s].",
                    clazz.getName(), String.join(",", missingGetters))));
        }
    }

    private void checkClassForJsonPojoBuilder(Class<?> clazz, Class<?> builderClass) {
        if (builderClass.getAnnotation(JsonPOJOBuilder.class) == null) {
            validationErrors.add(decorateWitParentClasses(String.format(
                    "Class %s has @JsonDeserialize attribute with Builder but has no nested Builder class with @JsonPOJOBuilder annotation.",
                    clazz.getName())));
        } else {
            List<String> fieldNames = checkAllPrivateFieldsForBuilderSetter(clazz, builderClass);
            if (!fieldNames.isEmpty()) {
                validationErrors.add(decorateWitParentClasses(String.format("Class %s has field(s) [%s] with no corresponding Builder method(s).",
                        clazz.getName(), String.join(",", fieldNames))));
            }
        }
    }

    private List<String> checkAllPrivateFieldsForBuilderSetter(Class<?> clazz, Class<?> builderClass) {
        List<String> fieldNames = new ArrayList<>();
        for (Field f : clazz.getDeclaredFields()) {
            if (!Modifier.isStatic(f.getModifiers())) {
                boolean setterPresent = isSetterForFieldPresent(builderClass, "with" + StringUtils.capitalize(f.getName()));
                if (!setterPresent) {
                    fieldNames.add(f.getName());
                }
            }
        }
        return fieldNames;
    }

    private void checkMissingGetters(Class<?> clazz) {
        List<String> fieldNames = checkAllPrivateFieldsForGetter(clazz);
        if (!fieldNames.isEmpty()) {
            validationErrors.add(decorateWitParentClasses(String.format("Class %s has non-final field(s) [%s] with no getter.",
                    clazz.getName(), String.join(",", fieldNames))));
        }
    }

    private void checkMissingSetters(Class<?> clazz) {
        List<String> fieldNames = checkAllPrivateFieldsForSetter(clazz);
        if (!fieldNames.isEmpty()) {
            validationErrors.add(decorateWitParentClasses(String.format("Class %s has non-final field(s) [%s] with no setter.",
                    clazz.getName(), String.join(",", fieldNames))));
        }
    }

    private void checkSoloBuilder(Class<?> clazz, Constructor<?>[] constructors) {
        if (constructors.length == 1 &&
                constructors[0].getParameterCount() == 1 &&
                getBuilderClass(clazz).isPresent() &&
                constructors[0].getParameterTypes()[0].equals(getBuilderClass(clazz).get())) {

            validationErrors.add(decorateWitParentClasses(String.format(
                    "Class %s has only a constructor for its builder thus it cannot be deserialized without proper annotation.", clazz.getName())));
        }
    }

    private Set<Type> getAllConstructorParams(Constructor<?>[] constructors) {
        Set<Type> paramTypes = new HashSet<>();
        for (Constructor<?> c : constructors) {
            paramTypes.addAll(Arrays.stream(c.getGenericParameterTypes()).collect(Collectors.toSet()));
        }
        return paramTypes;
    }

    private Optional<Class<?>> getBuilderClass(Class<?> clazz) {
        for (Class<?> innerClass : clazz.getDeclaredClasses()) {
            if (isStatic(innerClass) && isPublic(innerClass) &&
                    innerClass.getSimpleName().toLowerCase(Locale.ROOT).contains("builder")) {
                return Optional.of(innerClass);
            }
        }
        return Optional.empty();
    }

    private List<String> checkAllPrivateFieldsForGetter(Class<?> clazz) {
        List<String> fieldNames = new ArrayList<>();
        for (Field f : clazz.getDeclaredFields()) {
            if (!Modifier.isFinal(f.getModifiers())) {
                boolean getterPresent = isGetterForFieldPresent(clazz, f.getName(), isBooleanType(f.getType()));
                if (!getterPresent) {
                    fieldNames.add(f.getName());
                }
            }
        }
        return fieldNames;
    }

    private List<String> checkAllPrivateFieldsForSetter(Class<?> clazz) {
        List<String> fieldNames = new ArrayList<>();
        for (Field f : clazz.getDeclaredFields()) {
            if (!Modifier.isFinal(f.getModifiers())) {
                boolean setterPresent = isSetterForFieldPresent(clazz, "set" + StringUtils.capitalize(f.getName()));
                if (!setterPresent) {
                    fieldNames.add(f.getName());
                }
            }
        }
        return fieldNames;
    }

    private boolean hasDefaultConstructor(Constructor<?>[] constructors) {
        for (Constructor<?> c : constructors) {
            if (c.getParameterCount() == 0) {
                return true;
            }
        }
        return false;
    }

    private boolean isBooleanType(Class<?> clazz) {
        return clazz.equals(boolean.class);
    }

    private boolean isGetterForFieldPresent(Class<?> clazz, String fieldName, boolean booleanField) {
        String expectedGetterName = (booleanField ? "is" : "get") + StringUtils.capitalize(fieldName);
        for (Method m : clazz.getDeclaredMethods()) {
            if (expectedGetterName.equals(m.getName()) ||
                    m.getAnnotation(JsonProperty.class) != null && fieldName.equals(m.getAnnotation(JsonProperty.class).value())) {
                return true;
            }
        }
        return false;
    }

    private boolean isSetterForFieldPresent(Class<?> clazz, String expectedSetterName) {
        for (Method m : clazz.getDeclaredMethods()) {
            if (expectedSetterName.equals(m.getName())) {
                return true;
            }
        }
        return false;
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

    private boolean isGetterFoundRecursive(Class<?> fromClass, String paramName, boolean booleanParam) {
        String expectedName = (booleanParam ? "is" : "get") + StringUtils.capitalize(paramName);
        Queue<Class<?>> classQueue = new LinkedList<>();
        Set<Class<?>> visitedInterfaces = new HashSet<>();
        classQueue.add(fromClass);
        while (!classQueue.isEmpty()) {
            Class<?> currentClass = classQueue.remove();
            if (Arrays.stream(currentClass.getDeclaredMethods()).anyMatch(m -> expectedName.equals(m.getName()) ||
                    m.getAnnotation(JsonProperty.class) != null && paramName.equals(m.getAnnotation(JsonProperty.class).value()))) {
                return true;
            }
            Class<?> superclass = currentClass.getSuperclass();
            if (superclass != null && !superclass.equals(Object.class)) {
                classQueue.add(superclass);
            }
            Arrays.asList(currentClass.getInterfaces()).forEach(iface -> {
                if (!visitedInterfaces.contains(iface)) {
                    visitedInterfaces.add(iface);
                    classQueue.add(iface);
                }
            });
        }
        return false;
    }

    private String decorateWitParentClasses(String content) {
        return String.format("%s%s", content, CollectionUtils.isEmpty(parentClasses)
                ? StringUtils.EMPTY
                : String.format(" Parent classes: %s", parentClasses));
    }

    List<Field> getInheritedDeclaredFields(Class<?> fromClass) {
        Class<?> currentClass = fromClass;
        List<Field> fields = new ArrayList<>();
        do {
            fields.addAll(Arrays.asList(currentClass.getDeclaredFields()));
            currentClass = currentClass.getSuperclass();
        } while (currentClass != null && currentClass.getPackageName().startsWith(BASE_PACKAGE));
        return fields;
    }
}
