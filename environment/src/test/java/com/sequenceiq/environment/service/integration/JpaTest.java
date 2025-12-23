package com.sequenceiq.environment.service.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import jakarta.inject.Inject;

import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;
import org.junit.jupiter.api.function.Executable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Import;
import org.springframework.data.repository.support.Repositories;

@DataJpaTest(properties = {
        "spring.jpa.properties.hibernate.session_factory.statement_inspector=com.sequenceiq.environment.service.integration.SqlStatementInspector"})
@EntityScan(basePackages = {"com.sequenceiq.flow.domain",
        "com.sequenceiq.cloudbreak.rotation",
        "com.sequenceiq.environment",
        "com.sequenceiq.notification",
        "com.sequenceiq.cloudbreak.ha.domain",
        "com.sequenceiq.notification.domain",
        "com.sequenceiq.cloudbreak.structuredevent.domain"})
@Import(SqlStatementInspector.class)
public class JpaTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(JpaTest.class);

    @Inject
    private ApplicationContext applicationContext;

    @TestFactory
    public Collection<DynamicTest> dynamicTestsForRepositorySelectCount() {
        ArrayList<DynamicTest> tests = new ArrayList<>();
        Repositories repositories = new Repositories(applicationContext);
        for (Class domainType : repositories) {
            Class<?> repoInterface = repositories.getRepositoryInformationFor(domainType).get().getRepositoryInterface();
            Object repositoryInstance = repositories.getRepositoryFor(domainType).get();
            Method[] methods = repoInterface.getDeclaredMethods();
            for (Method method : methods) {
                if (method.getName().startsWith("find")) {
                    tests.add(
                            DynamicTest.dynamicTest(
                                    repoInterface.getSimpleName() + "." + method.getName(),
                                    callRepositoryMethod(repositoryInstance, method)));
                }
            }
        }
        return tests;
    }

    private Object[] methodArgs(Method method) {
        Object[] arguments = new Object[method.getParameterCount()];
        int i = 0;
        for (Class type : method.getParameterTypes()) {
            arguments[i] = getSampleInstances(type.getSimpleName());
            i++;
        }
        return arguments;
    }

    private Object getSampleInstances(String typeName) {
        switch (typeName) {
            case "String":
                return "this";
            case "Set":
            case "Collection":
                return Set.of();
            case "List":
                return List.of();
            case "Long":
            case "long":
            case "Serializable":
            case "Object":
                return 1L;
            case "boolean":
            case "Boolean":
                return true;
            default:
                LOGGER.info("Cannot parse '{}' type", typeName);
                return null;
        }
    }

    private Executable callRepositoryMethod(Object repository, Method method) {
        return () -> {
            SqlStatementInspector.getSelectCountNumberAndReset();
            try {
                if (method.getParameterCount() == 0) {
                    method.invoke(repository);
                } else {
                    method.invoke(repository, methodArgs(method));
                }
                assertEquals(1, SqlStatementInspector.getSelectCountNumberAndReset(),
                        "find method should compile only one select!");
            } catch (IllegalAccessException | IllegalArgumentException e) {
                fail("Could not call find method: " + method.toString(), e);
            } catch (InvocationTargetException e) {
                if (e.getCause() instanceof UnsupportedOperationException) {
                    assertTrue(true, "find finished with UnsupportedOperation");
                } else {
                    fail("Could not call find method, because InvocationTargetException: " + method.toString(), e);
                }
            }
        };
    }
}
