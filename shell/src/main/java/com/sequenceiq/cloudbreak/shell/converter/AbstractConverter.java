package com.sequenceiq.cloudbreak.shell.converter;

import java.lang.reflect.Constructor;
import java.util.Collection;
import java.util.List;

import org.springframework.shell.core.Completion;
import org.springframework.shell.core.Converter;

import com.sequenceiq.cloudbreak.shell.completion.AbstractCompletion;

public abstract class AbstractConverter<T extends AbstractCompletion> implements Converter<T> {

    protected AbstractConverter() {

    }

    @Override
    public T convertFromText(String value, Class<?> clazz, String optionContext) {
        try {
            Constructor<?> constructor = clazz.getDeclaredConstructor(String.class);
            return (T) constructor.newInstance(value);
        } catch (Exception ignored) {
            return null;
        }
    }

    public <E> boolean getAllPossibleValues(List<Completion> completions, Collection<E> values) {
        for (E value : values) {
            completions.add(new Completion(value.toString()));
        }
        return true;
    }

}