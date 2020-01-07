package com.sequenceiq.cloudbreak.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.convert.ApplicationConversionService;
import org.springframework.context.support.ConversionServiceFactoryBean;
import org.springframework.core.convert.ConversionFailedException;
import org.springframework.core.convert.support.GenericConversionService;

import com.sequenceiq.cloudbreak.exception.BadRequestException;

public class CloudbreakConversionServiceFactoryBean extends ConversionServiceFactoryBean {

    private static final Logger LOGGER = LoggerFactory.getLogger(CloudbreakConversionServiceFactoryBean.class);

    @Override
    protected GenericConversionService createConversionService() {
        return new CloudbreakConversionService();
    }

    private static class CloudbreakConversionService extends ApplicationConversionService {

        @Override
        public <T> T convert(Object source, Class<T> targetType) {
            try {
                return super.convert(source, targetType);
            } catch (ConversionFailedException ex) {
                String errorMessage = String.format("Failed to convert from type [%s] to type [%s] %s", source.getClass().getName(),
                        targetType.getName(), ex.getCause().getMessage());
                LOGGER.error(errorMessage, ex);
                if (ex.getCause() instanceof BadRequestException) {
                    throw new BadRequestException(ex.getCause().getMessage(), ex.getCause());
                }
                throw new BadRequestException(errorMessage, ex);
            }
        }
    }
}
