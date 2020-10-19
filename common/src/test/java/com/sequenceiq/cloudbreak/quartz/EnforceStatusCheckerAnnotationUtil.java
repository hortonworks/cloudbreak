package com.sequenceiq.cloudbreak.quartz;

import static org.junit.Assert.assertTrue;

import java.util.Set;
import java.util.stream.Collectors;

import org.quartz.DisallowConcurrentExecution;
import org.reflections.Reflections;
import org.reflections.scanners.SubTypesScanner;
import org.reflections.scanners.TypeAnnotationsScanner;

import com.google.common.base.Joiner;
import com.google.common.collect.Sets;
import com.sequenceiq.cloudbreak.quartz.statuschecker.job.StatusCheckerJob;

public class EnforceStatusCheckerAnnotationUtil {

    private static final Reflections REFLECTIONS = new Reflections("com.sequenceiq",
            new TypeAnnotationsScanner(),
            new SubTypesScanner(false));

    private EnforceStatusCheckerAnnotationUtil() {

    }

    public static void enforceDisablingConcurrentExecution() {
        Set<Class> annotatedStatusCheckers = REFLECTIONS.getTypesAnnotatedWith(DisallowConcurrentExecution.class)
                .stream()
                .filter(clazz -> StatusCheckerJob.class.isAssignableFrom(clazz))
                .collect(Collectors.toSet());
        Set<Class<? extends StatusCheckerJob>> statusCheckers = REFLECTIONS.getSubTypesOf(StatusCheckerJob.class);
        Set<Class<? extends StatusCheckerJob>> statusCheckersWithMissingAnnotation =
                Sets.difference(statusCheckers, annotatedStatusCheckers);
        assertTrue("These classes are missing @DisallowConcurrentExecution annotation: " + Joiner.on(",").join(statusCheckersWithMissingAnnotation),
                statusCheckersWithMissingAnnotation.isEmpty());
    }
}
