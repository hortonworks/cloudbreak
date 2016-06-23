package com.sequenceiq.periscope.utils

import java.text.ParseException
import java.util.Date

import org.joda.time.DateTime
import org.joda.time.DateTimeZone
import org.quartz.CronExpression
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class DateUtils private constructor() {

    init {
        throw IllegalStateException()
    }

    companion object {

        private val LOGGER = LoggerFactory.getLogger(DateUtils::class.java)

        fun isTrigger(cron: String, timeZone: String, monitorUpdateRate: Long): Boolean {
            try {
                val cronExpression = getCronExpression(cron)
                val currentTime = getCurrentDate(timeZone)
                val nextTime = cronExpression.getNextValidTimeAfter(currentTime)
                val nextDateTime = getDateTime(nextTime, timeZone).minus(monitorUpdateRate)
                val interval = nextDateTime.toDate().time - currentTime.time
                return interval > 0 && interval < monitorUpdateRate
            } catch (e: ParseException) {
                LOGGER.warn("Invalid cron expression, {}", e.message)
                return false
            }

        }

        @Throws(ParseException::class)
        fun getCronExpression(cron: String): CronExpression {
            return CronExpression(cron)
        }

        private fun getDateTime(date: Date, timeZone: String): DateTime {
            return DateTime(date).withZone(getTimeZone(timeZone))
        }

        private fun getCurrentDate(timeZone: String): Date {
            return getCurrentDateTime(timeZone).toLocalDateTime().toDate()
        }

        private fun getCurrentDateTime(timeZone: String): DateTime {
            return DateTime.now(getTimeZone(timeZone))
        }

        private fun getTimeZone(timeZone: String): DateTimeZone {
            return DateTimeZone.forID(timeZone)
        }
    }

}
