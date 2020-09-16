package com.sequenceiq.environment.service.integration;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Set;

import javax.inject.Inject;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;
import org.junit.jupiter.api.function.Executable;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Import;
import org.springframework.data.repository.support.Repositories;

@DataJpaTest(properties = {
        "spring.jpa.properties.hibernate.session_factory.statement_inspector=com.sequenceiq.environment.service.integration.SqlStatementInspector"})
@EntityScan(basePackages = {"com.sequenceiq.flow.domain",
        "com.sequenceiq.environment",
        "com.sequenceiq.cloudbreak.ha.domain",
        "com.sequenceiq.cloudbreak.structuredevent.domain"})
@Import(SqlStatementInspector.class)
public class JpaTest {

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
            case "Long":
            case "long":
            case "Serializable":
            case "Object":
                return 1L;
            case "boolean":
            case "Boolean":
                return true;
            default:
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
                Assertions.assertEquals(1, SqlStatementInspector.getSelectCountNumberAndReset(),
                        "find method should compile only one select!");
            } catch (IllegalAccessException | IllegalArgumentException e) {
                Assertions.fail("Could not call find method: " + method.toString(), e);
            } catch (InvocationTargetException e) {
                if (e.getCause() instanceof UnsupportedOperationException) {
                    Assertions.assertTrue(true, "find finished with UnsupportedOperation");
                } else {
                    Assertions.fail("Could not call find method: " + method.toString(), e);
                }
            }
        };
    }
}
