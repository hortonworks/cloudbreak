package com.sequenceiq.redbeams.validation;

import static org.junit.jupiter.api.Assertions.fail;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.type.filter.AnnotationTypeFilter;

import com.sequenceiq.cloudbreak.validation.ValidIfExactlyOneNonNull;

class ValidIfExactlyOneNonNullCheckerTest {

    @Test
    void testAllAnnotatedClasses() throws NoSuchFieldException, ClassNotFoundException {
        ClassPathScanningCandidateComponentProvider scanner = new ClassPathScanningCandidateComponentProvider(false);
        scanner.addIncludeFilter(new AnnotationTypeFilter(ValidIfExactlyOneNonNull.class));
        for (BeanDefinition def : scanner.findCandidateComponents("com.sequenceiq.redbeams")) {
            String annotatedClassName = def.getBeanClassName();
            Class<?> annotatedClass = Class.forName(annotatedClassName);
            ValidIfExactlyOneNonNull classAnnotation = annotatedClass.getAnnotation(ValidIfExactlyOneNonNull.class);
            String[] fields = classAnnotation.fields();
            if (fields.length == 0) {
                fail("ValidIfExactlyOneNonNull annotation on class " + annotatedClassName + " has no fields");
            }
            for (String fieldName : classAnnotation.fields()) {
                annotatedClass.getDeclaredField(fieldName);
                // if this does not throw an exception, great
            }
        }
    }
}
