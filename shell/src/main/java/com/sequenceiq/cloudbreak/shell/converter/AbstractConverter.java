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
        } catch (Exception e) {
            return null;
        }
    }

    public boolean getAllPossibleValues(List<Completion> completions, Collection<String> values) {
        for (String value : values) {
            completions.add(new Completion(value));
        }
        return true;
    }

}