package com.sequenceiq.cloudbreak.concurrent;

import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Function;

public class DecoratorExecutorService implements ExecutorService {

    private final ExecutorService delegate;

    private final List<Function<Callable, Callable>> decorators;

    public DecoratorExecutorService(ExecutorService delegate, List<Function<Callable, Callable>> decorators) {
        this.delegate = Objects.requireNonNull(delegate, "delegate must not be null.");
        this.decorators = List.copyOf(Objects.requireNonNull(decorators, "decorators must not be null."));
    }

    @Override
    public void shutdown() {
        delegate.shutdown();
    }

    @Override
    public List<Runnable> shutdownNow() {
        return delegate.shutdownNow();
    }

    @Override
    public boolean isShutdown() {
        return delegate.isShutdown();
    }

    @Override
    public boolean isTerminated() {
        return delegate.isTerminated();
    }

    @Override
    public boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
        return delegate.awaitTermination(timeout, unit);
    }

    @Override
    public <T> Future<T> submit(Callable<T> task) {
        return delegate.submit(decorate(task));
    }

    @Override
    public <T> Future<T> submit(Runnable task, T result) {
        return delegate.submit(decorate(Executors.callable(task, result)));
    }

    @Override
    public Future<?> submit(Runnable task) {
        return delegate.submit(decorate(Executors.callable(task)));
    }

    @Override
    public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks) throws InterruptedException {
        return delegate.invokeAll(decorate(tasks));
    }

    @Override
    public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit) throws InterruptedException {
        return delegate.invokeAll(decorate(tasks), timeout, unit);
    }

    @Override
    public <T> T invokeAny(Collection<? extends Callable<T>> tasks) throws InterruptedException, ExecutionException {
        return delegate.invokeAny(decorate(tasks));
    }

    @Override
    public <T> T invokeAny(Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit)
            throws InterruptedException, ExecutionException, TimeoutException {
        return delegate.invokeAny(decorate(tasks), timeout, unit);
    }

    @Override
    public void execute(Runnable command) {
        Callable<Object> decoratedCallable = decorate(Executors.callable(command));
        delegate.execute(() -> {
            try {
                decoratedCallable.call();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
    }

    public List<Function<Callable, Callable>> getDecorators() {
        return decorators;
    }

    public ExecutorService getDelegate() {
        return delegate;
    }

    protected <T> Collection<? extends Callable<T>> decorate(Collection<? extends Callable<T>> tasks) {
        return tasks.stream().map(this::decorate).toList();
    }

    protected <T> Callable<T> decorate(Callable<T> callable) {
        for (Function<Callable, Callable> decorator : decorators) {
            callable = decorator.apply(callable);
        }
        return callable;
    }
}
