package com.sequenceiq.flow.domain;

import java.util.Objects;

public class ClassValue {

    private final String name;

    private final Class<?> classValue;

    private ClassValue(String name, Class<?> classValue) {
        this.name = name;
        this.classValue = classValue;
    }

    public static ClassValue of(String name) throws ClassNotFoundException {
        return new ClassValue(name, Class.forName(name));
    }

    public static ClassValue of(Class<?> classValue) {
        return new ClassValue(classValue.getName(), classValue);
    }

    public static ClassValue ofUnknown(String name) {
        return new ClassValue(name, null);
    }

    public boolean isOnClassPath() {
        return classValue != null;
    }

    public String getName() {
        return name;
    }

    public String getSimpleName() {
        if (isOnClassPath()) {
            return classValue.getSimpleName();
        } else {
            if (name.contains(".")) {
                return name.substring(name.lastIndexOf('.') + 1);
            } else {
                return name;
            }
        }
    }

    public Class<?> getClassValue() {
        if (!isOnClassPath()) {
            throw new IllegalStateException(name + " is not a known class.");
        }
        return classValue;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        ClassValue that = (ClassValue) o;
        return Objects.equals(name, that.name) && Objects.equals(classValue, that.classValue);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, classValue);
    }

    @Override
    public String toString() {
        return "ClassValue{" +
                "name='" + name + '\'' +
                ", classValue=" + classValue +
                '}';
    }
}
