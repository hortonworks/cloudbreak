package com.sequenceiq.periscope.monitor

class MonitorUpdateRate private constructor() {

    init {
        throw IllegalStateException()
    }

    companion object {

        /**
         * Every 2 minutes.
         */
        val APP_REPORT_UPDATE_RATE_CRON = "0 0/2 * * * ?"

        /**
         * Every 20 seconds.
         */
        val YARN_UPDATE_RATE_CRON = "0/20 * * * * ?"

        /**
         * Every 30 seconds.
         */
        val METRIC_UPDATE_RATE_CRON = "0/30 * * * * ?"

        /**
         * Every 10 seconds.
         */
        val TIME_UPDATE_RATE_CRON = "0/10 * * * * ?"

        /**
         * Time update rate in ms, aligned to the cron expression.
         */
        val CLUSTER_UPDATE_RATE = 10000
    }
}
