package com.sequenceiq.periscope.monitor

import java.util.concurrent.ExecutorService

import org.quartz.JobDataMap
import org.quartz.JobExecutionContext
import org.quartz.JobExecutionException
import org.springframework.context.ApplicationContext

import com.sequenceiq.periscope.domain.Cluster
import com.sequenceiq.periscope.api.model.ClusterState
import com.sequenceiq.periscope.monitor.evaluator.EvaluatorExecutor
import com.sequenceiq.periscope.service.ClusterService

abstract class AbstractMonitor : Monitor {

    private var clusterService: ClusterService? = null
    private var applicationContext: ApplicationContext? = null
    private var executorService: ExecutorService? = null

    @Throws(JobExecutionException::class)
    override fun execute(context: JobExecutionContext) {
        evalContext(context)
        for (cluster in clusterService!!.findAll(ClusterState.RUNNING)) {
            val evaluatorExecutor = applicationContext!!.getBean<EvaluatorExecutor>(evaluatorType.simpleName, EvaluatorExecutor::class.java)
            evaluatorExecutor.setContext(getContext(cluster))
            executorService!!.submit(evaluatorExecutor)
        }
    }

    private fun evalContext(context: JobExecutionContext) {
        val monitorContext = context.jobDetail.jobDataMap
        applicationContext = monitorContext[MonitorContext.APPLICATION_CONTEXT.name] as ApplicationContext
        executorService = applicationContext!!.getBean<ExecutorService>(ExecutorService::class.java)
        clusterService = applicationContext!!.getBean<ClusterService>(ClusterService::class.java)
    }

}
