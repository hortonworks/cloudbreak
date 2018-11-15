package com.sequenceiq.cloudbreak.converter;

import org.springframework.context.support.ConversionServiceFactoryBean;
import org.springframework.core.convert.ConversionFailedException;
import org.springframework.core.convert.support.DefaultConversionService;
import org.springframework.core.convert.support.GenericConversionService;

public class CloudbreakConversionServiceFactoryBean extends ConversionServiceFactoryBean {

    @Override
    protected GenericConversionService createConversionService() {
        return new CloudbreakConversionService();
    }

    private static class CloudbreakConversionService extends DefaultConversionService {

        @Override
        public <T> T convert(Object source, Class<T> targetType) {
            try {
                return super.convert(source, targetType);
            } catch (ConversionFailedException ex) {
                throw new ConversionException(String.format("Failed to convert from type [%s] to type [%s] %s", source.getClass().getName(),
                        targetType.getName(), ex.getCause().getMessage()), ex);
            }
        }
    }
}
