package com.sequenceiq.flow.helper;

import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;

public class GenericParameterTypeParser {

    private GenericParameterTypeParser() {
    }

    public static Set<Class<?>> parse(String basePackage, Type paramType) {
        Set<Class<?>> paramClasses = new HashSet<>();
        String typeName = paramType.getTypeName();
        if (!typeName.contains("<")) {
            if (typeName.startsWith(basePackage)) {
                getClassForName(typeName).ifPresent(paramClasses::add);
            }
        } else {
            while (typeName.contains("<")) {
                String baseTypeName = typeName.substring(0, typeName.indexOf('<'));
                if (baseTypeName.startsWith(basePackage)) {
                    getClassForName(baseTypeName).ifPresent(paramClasses::add);
                }
                if (typeName.charAt(typeName.length() - 1) == '>') {
                    String genericArg = typeName.substring(typeName.indexOf('<') + 1, typeName.length() - 1);
                    typeName = genericArg;
                } else {
                    int lastClosing = typeName.lastIndexOf('>');
                    Set<Class<?>> genericClassArguments = getGenericClassArguments(basePackage, typeName.substring(lastClosing + 1).split(",\\s+"));
                    paramClasses.addAll(genericClassArguments);
                    typeName = typeName.substring(typeName.indexOf('<') + 1, lastClosing);
                }
            }
            Set<Class<?>> genericClassArguments = getGenericClassArguments(basePackage, typeName.split(",\\s+"));
            paramClasses.addAll(genericClassArguments);
        }
        return paramClasses;
    }

    private static Set<Class<?>> getGenericClassArguments(String basePackage, String[] typeNames) {
        return Arrays.stream(typeNames)
                .filter(StringUtils::isNotBlank)
                .filter(s -> s.startsWith(basePackage))
                .map(String::trim)
                .map(GenericParameterTypeParser::getClassForName)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .collect(Collectors.toSet());
    }

    private static Optional<Class<?>> getClassForName(String className) {
        try {
            return Optional.of(Class.forName(className));
        } catch (ClassNotFoundException e) {
            return Optional.empty();
        }
    }
}
