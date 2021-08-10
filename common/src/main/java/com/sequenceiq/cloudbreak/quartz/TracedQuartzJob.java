package com.sequenceiq.cloudbreak.quartz;

import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.scheduling.quartz.QuartzJobBean;

import com.sequenceiq.cloudbreak.logger.MDCBuilder;
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
        fillMdcContext();
        Span span = TracingUtil.initSpan(tracer, "Quartz", jobName);
        TracingUtil.setTagsFromMdc(span);
        try (Scope ignored = tracer.activateSpan(span)) {
            executeTracedJob(context);
        } finally {
            span.finish();
            MDCBuilder.cleanupMdc();
        }
    }

    protected void fillMdcContext() {
        MDCBuilder.buildMdcContext(getMdcContextObject());
        MDCBuilder.getOrGenerateRequestId();
    }

    protected abstract Object getMdcContextObject();

    protected abstract void executeTracedJob(JobExecutionContext context) throws JobExecutionException;
}
