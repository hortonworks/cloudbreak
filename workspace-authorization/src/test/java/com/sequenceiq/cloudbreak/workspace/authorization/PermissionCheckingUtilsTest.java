package com.sequenceiq.cloudbreak.workspace.authorization;

import static java.lang.String.format;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

public class PermissionCheckingUtilsTest {

    private static final String INDEX_NAME = "someIndexNameValue";

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @InjectMocks
    private PermissionCheckingUtils underTest;

    @Mock
    private ProceedingJoinPoint proceedingJoinPoint;

    @Mock
    private MethodSignature methodSignature;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void testValidateIndexWhenIndexIsGreaterThanLengthThenIllegalArgumentExceptionComes() {
        int index = 2;
        int length = 1;
        String indexName = INDEX_NAME;

        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage(format("The %s [%s] cannot be bigger than or equal to the methods argument count [%s]", indexName, index, length));

        underTest.validateIndex(index, length, indexName);
    }

    @Test
    public void testValidateIndexWhenIndexIsEqualsWithLengthThenIllegalArgumentExceptionComes() {
        int index = 2;
        int length = 2;
        String indexName = INDEX_NAME;

        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage(format("The %s [%s] cannot be bigger than or equal to the methods argument count [%s]", indexName, index, length));

        underTest.validateIndex(index, length, indexName);
    }

    @Test
    public void testValidateIndexWhenIndexIsLessThanLengthThenIllegalArgumentExceptionComes() {
        int index = 2;
        int length = 3;

        underTest.validateIndex(index, length, INDEX_NAME);
    }

}