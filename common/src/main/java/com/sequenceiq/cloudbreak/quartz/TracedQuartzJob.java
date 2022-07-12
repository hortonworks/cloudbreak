package com.sequenceiq.cloudbreak.quartz;

import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.scheduling.quartz.QuartzJobBean;

import com.sequenceiq.cloudbreak.logger.LoggerContextKey;
import com.sequenceiq.cloudbreak.logger.MDCBuilder;
import com.sequenceiq.cloudbreak.logger.MdcContextInfoProvider;
import com.sequenceiq.cloudbreak.tracing.TracingUtil;

import io.opentracing.Scope;
import io.opentracing.Span;
import io.opentracing.Tracer;

public abstract class TracedQuartzJob extends QuartzJobBean {

    private final String jobName;

    private final Tracer tracer;

    public TracedQuartzJob(Tracer tracer, String jobName) {
        this.tracer = tracer;
        this.jobName = jobName;
    }

    @Override
    protected final void executeInternal(JobExecutionContext context) throws JobExecutionException {
        fillMdcContext(context);
        Span span = TracingUtil.initSpan(tracer, "Quartz", jobName);
        TracingUtil.setTagsFromMdc(span);
        try (Scope ignored = tracer.activateSpan(span)) {
            executeTracedJob(context);
        } finally {
            span.finish();
            MDCBuilder.cleanupMdc();
        }
    }

    protected void fillMdcContext(JobExecutionContext context) {
        MdcContextInfoProvider mdcContextConfigProvider = getMdcContextConfigProvider();
        if (mdcContextConfigProvider == null) {
            Object mdcContextObject = getMdcContextObject();
            if (mdcContextObject == null) {
                throw new IllegalArgumentException("Please implement one of them: getMdcContextObject() or getMdcContextConfigProvider()");
            } else {
                MDCBuilder.buildMdcContext(mdcContextObject);
            }
        } else {
            MDCBuilder.buildMdcContextFromInfoProvider(mdcContextConfigProvider);
        }
        String requestId = MDCBuilder.getOrGenerateRequestId();
        context.put(LoggerContextKey.REQUEST_ID.toString(), requestId);
    }

    protected Object getMdcContextObject() {
        return null;
    }

    protected MdcContextInfoProvider getMdcContextConfigProvider() {
        return null;
    }

    protected abstract void executeTracedJob(JobExecutionContext context) throws JobExecutionException;
}
