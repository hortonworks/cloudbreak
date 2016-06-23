package com.sequenceiq.periscope.monitor.evaluator

import org.springframework.context.ApplicationEventPublisher
import org.springframework.context.ApplicationEventPublisherAware

interface EvaluatorExecutor : ApplicationEventPublisher, ApplicationEventPublisherAware, Runnable {

    fun setContext(context: Map<String, Any>)

}
