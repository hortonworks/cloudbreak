package com.sequenceiq.cloudbreak.concurrent;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import org.springframework.core.task.TaskDecorator;

class CompositeTaskDecoratorTest {

    @Test
    void testNullDecorators() {
        assertThrows(NullPointerException.class, () -> {
            new CompositeTaskDecorator(null);
        });
    }

    @Test
    void testNoDecorators() {
        CompositeTaskDecorator underTest = new CompositeTaskDecorator(List.of());
        Runnable runnable = () -> { };
        Runnable decoratedRunnable = underTest.decorate(runnable);
        assertEquals(runnable, decoratedRunnable);
    }

    @Test
    void testOneDecorator() {
        DoSomething before = mock(DoSomething.class);
        DoSomething after = mock(DoSomething.class);
        Runnable runnable = mock(Runnable.class);
        InOrder inOrder = inOrder(before, after, runnable);

        CompositeTaskDecorator underTest = new CompositeTaskDecorator(List.of(TestDecorator.around(before, after)));
        Runnable decoratedRunnable = underTest.decorate(runnable);

        decoratedRunnable.run();

        inOrder.verify(before).doSomething();
        inOrder.verify(runnable).run();
        inOrder.verify(after).doSomething();
    }

    @Test
    void testMultipleDecorators() {
        DoSomething before1 = mock(DoSomething.class);
        DoSomething after1 = mock(DoSomething.class);
        DoSomething before2 = mock(DoSomething.class);
        DoSomething after2 = mock(DoSomething.class);
        Runnable runnable = mock(Runnable.class);
        InOrder inOrder = inOrder(before1, after1, before2, after2, runnable);

        CompositeTaskDecorator underTest = new CompositeTaskDecorator(List.of(
                TestDecorator.around(before1, after1),
                TestDecorator.around(before2, after2)));
        Runnable decoratedRunnable = underTest.decorate(runnable);

        decoratedRunnable.run();

        inOrder.verify(before2).doSomething();
        inOrder.verify(before1).doSomething();
        inOrder.verify(runnable).run();
        inOrder.verify(after1).doSomething();
        inOrder.verify(after2).doSomething();
    }

    private static class TestDecorator implements TaskDecorator {
        private final DoSomething before;

        private final DoSomething after;

        TestDecorator(DoSomething before, DoSomething after) {
            this.before = before;
            this.after = after;
        }

        @Override
        public Runnable decorate(Runnable runnable) {
            return () -> {
                before.doSomething();
                runnable.run();
                after.doSomething();
            };
        }

        private static TestDecorator around(DoSomething before, DoSomething after) {
            return new TestDecorator(before, after);
        }
    }

    private interface DoSomething {
        void doSomething();
    }
}