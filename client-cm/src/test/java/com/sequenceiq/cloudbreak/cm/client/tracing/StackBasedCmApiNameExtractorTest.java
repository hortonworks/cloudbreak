package com.sequenceiq.cloudbreak.cm.client.tracing;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.Test;

class StackBasedCmApiNameExtractorTest {

    private StackBasedCmApiNameExtractor underTest = new StackBasedCmApiNameExtractor();

    @Test
    void testWithValidStackTrace() {
        String stackTraceString = getValidStackTraceString();
        StackWalker stackWalkerMock = mock(StackWalker.class);
        Stream<? extends StackWalker.StackFrame> stream = stringToStackFrameStream(stackTraceString);
        when(stackWalkerMock.walk(any(Function.class))).thenAnswer(invocation -> {
            Function argument = invocation.getArgument(0);
            return argument.apply(stream);
        });

        Optional<String> cmApiName = underTest.getCmApiName(stackWalkerMock);

        assertEquals("ClouderaManagerResourceApi.getVersionWithHttpInfo", cmApiName.get());
    }

    @Test
    void testWithMissingCmApiClass() {
        String stackTraceString = getInvalidStackTraceString();
        StackWalker stackWalkerMock = mock(StackWalker.class);
        Stream<? extends StackWalker.StackFrame> stream = stringToStackFrameStream(stackTraceString);
        when(stackWalkerMock.walk(any(Function.class))).thenAnswer(invocation -> {
            Function argument = invocation.getArgument(0);
            return argument.apply(stream);
        });

        Optional<String> cmApiName = underTest.getCmApiName(stackWalkerMock);

        assertTrue(cmApiName.isEmpty());
    }

    Stream<? extends StackWalker.StackFrame> stringToStackFrameStream(String stackTraceString) {
        String[] split = stackTraceString.split("\n");

        List<String> stringList = new ArrayList<>();
        Collections.addAll(stringList, split);

        List<TestStackFrame> stackTrace = stringList.stream()
                .map(s -> StringUtils.removeStart(s, "at "))
                .map(s -> {
                    String[] split1 = StringUtils.split(s, ".");
                    String className = StringUtils.substringBeforeLast(StringUtils.substringBefore(s, "("), ".");
                    String methodName = StringUtils.substringBefore(split1[split1.length - 2], "(");
                    return new TestStackFrame(className, methodName);
                })
                .collect(Collectors.toList());
        return stackTrace.stream();
    }

    // CHECKSTYLE:OFF
    private String getValidStackTraceString() {
        return "at java.base/java.lang.Thread.dumpStack(Thread.java:1387)\n" +
                "at com.sequenceiq.cloudbreak.cm.client.tracing.CmOkHttpTracingInterceptor.intercept(CmOkHttpTracingInterceptor.java:35)\n" +
                "at com.squareup.okhttp.Call$ApplicationInterceptorChain.proceed(Call.java:232)\n" +
                "at com.squareup.okhttp.Call.getResponseWithInterceptorChain(Call.java:205)\n" +
                "at com.squareup.okhttp.Call.execute(Call.java:80)\n" +
                "at com.cloudera.api.swagger.client.ApiClient.execute(ApiClient.java:989)\n" +
                "at com.cloudera.api.swagger.ClouderaManagerResourceApi.getVersionWithHttpInfo(ClouderaManagerResourceApi.java:2165)\n" +
                "at com.cloudera.api.swagger.ClouderaManagerResourceApi.getVersion(ClouderaManagerResourceApi.java:2152)\n" +
                "at com.cloudera.api.swagger.ClouderaManagerResourceApi$$FastClassBySpringCGLIB$$97c0d158.invoke(<generated>)\n" +
                "at org.springframework.cglib.proxy.MethodProxy.invoke(MethodProxy.java:218)\n" +
                "at org.springframework.aop.framework.CglibAopProxy$CglibMethodInvocation.invokeJoinpoint(CglibAopProxy.java:749)\n" +
                "at org.springframework.aop.framework.ReflectiveMethodInvocation.proceed(ReflectiveMethodInvocation.java:163)\n" +
                "at org.springframework.aop.aspectj.MethodInvocationProceedingJoinPoint.proceed(MethodInvocationProceedingJoinPoint.java:88)\n" +
                "at com.sequenceiq.cloudbreak.cm.client.retry.CmApiRetryAspect.lambda$retryableApiCall$0(CmApiRetryAspect.java:28)\n" +
                "at org.springframework.retry.support.RetryTemplate.doExecute(RetryTemplate.java:286)\n" +
                "at org.springframework.retry.support.RetryTemplate.execute(RetryTemplate.java:163)\n" +
                "at com.sequenceiq.cloudbreak.cm.client.retry.CmApiRetryAspect.retryableApiCall(CmApiRetryAspect.java:28)\n" +
                "at jdk.internal.reflect.GeneratedMethodAccessor788.invoke(Unknown Source)\n" +
                "at java.base/jdk.internal.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:43)\n" +
                "at java.base/java.lang.reflect.Method.invoke(Method.java:566)\n" +
                "at org.springframework.aop.aspectj.AbstractAspectJAdvice.invokeAdviceMethodWithGivenArgs(AbstractAspectJAdvice.java:644)\n" +
                "at org.springframework.aop.aspectj.AbstractAspectJAdvice.invokeAdviceMethod(AbstractAspectJAdvice.java:633)\n" +
                "at org.springframework.aop.aspectj.AspectJAroundAdvice.invoke(AspectJAroundAdvice.java:70)\n" +
                "at org.springframework.aop.framework.ReflectiveMethodInvocation.proceed(ReflectiveMethodInvocation.java:186)\n" +
                "at org.springframework.aop.interceptor.ExposeInvocationInterceptor.invoke(ExposeInvocationInterceptor.java:93)\n" +
                "at org.springframework.aop.framework.ReflectiveMethodInvocation.proceed(ReflectiveMethodInvocation.java:186)\n" +
                "at org.springframework.aop.framework.CglibAopProxy$DynamicAdvisedInterceptor.intercept(CglibAopProxy.java:688)\n" +
                "at com.cloudera.api.swagger.ClouderaManagerResourceApi$$EnhancerBySpringCGLIB$$c61039f7.getVersion(<generated>)\n" +
                "at com.sequenceiq.cloudbreak.cm.ClouderaManagerClusterStatusService.isClusterManagerRunning(ClouderaManagerClusterStatusService.java:334)\n" +
                "at com.sequenceiq.cloudbreak.cm.ClouderaManagerClusterStatusService.getStatus(ClouderaManagerClusterStatusService.java:216)\n" +
                "at com.sequenceiq.cloudbreak.job.StackStatusCheckerJob.queryClusterStatus(StackStatusCheckerJob.java:249)\n" +
                "at com.sequenceiq.cloudbreak.job.StackStatusCheckerJob.isClusterManagerRunning(StackStatusCheckerJob.java:234)\n" +
                "at com.sequenceiq.cloudbreak.job.StackStatusCheckerJob.doSync(StackStatusCheckerJob.java:170)\n" +
                "at com.sequenceiq.cloudbreak.job.StackStatusCheckerJob.lambda$executeInternal$0(StackStatusCheckerJob.java:103)\n" +
                "at com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider.doAs(ThreadBasedUserCrnProvider.java:112)\n" +
                "at com.sequenceiq.cloudbreak.job.StackStatusCheckerJob.executeInternal(StackStatusCheckerJob.java:103)\n" +
                "at org.springframework.scheduling.quartz.QuartzJobBean.execute(QuartzJobBean.java:75)\n" +
                "at org.quartz.core.JobRunShell.run(JobRunShell.java:202)\n" +
                "at org.quartz.simpl.SimpleThreadPool$WorkerThread.run(SimpleThreadPool.java:573)";
    }

    private String getInvalidStackTraceString() {
        return "at java.base/java.lang.Thread.dumpStack(Thread.java:1387)\n" +
                    "at com.sequenceiq.cloudbreak.cm.client.tracing.CmOkHttpTracingInterceptor.intercept(CmOkHttpTracingInterceptor.java:35)\n" +
                    "at com.squareup.okhttp.Call$ApplicationInterceptorChain.proceed(Call.java:232)\n" +
                    "at com.squareup.okhttp.Call.getResponseWithInterceptorChain(Call.java:205)\n" +
                    "at com.squareup.okhttp.Call.execute(Call.java:80)\n" +
                    "at com.cloudera.api.swagger.client.ApiClient.execute(ApiClient.java:989)\n" +
                    "at org.springframework.cglib.proxy.MethodProxy.invoke(MethodProxy.java:218)\n" +
                    "at org.springframework.aop.framework.CglibAopProxy$CglibMethodInvocation.invokeJoinpoint(CglibAopProxy.java:749)\n" +
                    "at org.springframework.aop.framework.ReflectiveMethodInvocation.proceed(ReflectiveMethodInvocation.java:163)\n" +
                    "at org.springframework.aop.aspectj.MethodInvocationProceedingJoinPoint.proceed(MethodInvocationProceedingJoinPoint.java:88)\n" +
                    "at com.sequenceiq.cloudbreak.cm.client.retry.CmApiRetryAspect.lambda$retryableApiCall$0(CmApiRetryAspect.java:28)\n" +
                    "at org.springframework.retry.support.RetryTemplate.doExecute(RetryTemplate.java:286)\n" +
                    "at org.springframework.retry.support.RetryTemplate.execute(RetryTemplate.java:163)\n" +
                    "at com.sequenceiq.cloudbreak.cm.client.retry.CmApiRetryAspect.retryableApiCall(CmApiRetryAspect.java:28)\n" +
                    "at jdk.internal.reflect.GeneratedMethodAccessor788.invoke(Unknown Source)\n" +
                    "at java.base/jdk.internal.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:43)\n" +
                    "at java.base/java.lang.reflect.Method.invoke(Method.java:566)\n" +
                    "at org.springframework.aop.aspectj.AbstractAspectJAdvice.invokeAdviceMethodWithGivenArgs(AbstractAspectJAdvice.java:644)\n" +
                    "at org.springframework.aop.aspectj.AbstractAspectJAdvice.invokeAdviceMethod(AbstractAspectJAdvice.java:633)\n" +
                    "at org.springframework.aop.aspectj.AspectJAroundAdvice.invoke(AspectJAroundAdvice.java:70)\n" +
                    "at org.springframework.aop.framework.ReflectiveMethodInvocation.proceed(ReflectiveMethodInvocation.java:186)\n" +
                    "at org.springframework.aop.interceptor.ExposeInvocationInterceptor.invoke(ExposeInvocationInterceptor.java:93)\n" +
                    "at org.springframework.aop.framework.ReflectiveMethodInvocation.proceed(ReflectiveMethodInvocation.java:186)\n" +
                    "at org.springframework.aop.framework.CglibAopProxy$DynamicAdvisedInterceptor.intercept(CglibAopProxy.java:688)\n" +
                    "at com.sequenceiq.cloudbreak.cm.ClouderaManagerClusterStatusService.isClusterManagerRunning(ClouderaManagerClusterStatusService.java:334)\n" +
                    "at com.sequenceiq.cloudbreak.cm.ClouderaManagerClusterStatusService.getStatus(ClouderaManagerClusterStatusService.java:216)\n" +
                    "at com.sequenceiq.cloudbreak.job.StackStatusCheckerJob.queryClusterStatus(StackStatusCheckerJob.java:249)\n" +
                    "at com.sequenceiq.cloudbreak.job.StackStatusCheckerJob.isClusterManagerRunning(StackStatusCheckerJob.java:234)\n" +
                    "at com.sequenceiq.cloudbreak.job.StackStatusCheckerJob.doSync(StackStatusCheckerJob.java:170)\n" +
                    "at com.sequenceiq.cloudbreak.job.StackStatusCheckerJob.lambda$executeInternal$0(StackStatusCheckerJob.java:103)\n" +
                    "at com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider.doAs(ThreadBasedUserCrnProvider.java:112)\n" +
                    "at com.sequenceiq.cloudbreak.job.StackStatusCheckerJob.executeInternal(StackStatusCheckerJob.java:103)\n" +
                    "at org.springframework.scheduling.quartz.QuartzJobBean.execute(QuartzJobBean.java:75)\n" +
                    "at org.quartz.core.JobRunShell.run(JobRunShell.java:202)\n" +
                    "at org.quartz.simpl.SimpleThreadPool$WorkerThread.run(SimpleThreadPool.java:573)";
    }
    // CHECKSTYLE:ON
}