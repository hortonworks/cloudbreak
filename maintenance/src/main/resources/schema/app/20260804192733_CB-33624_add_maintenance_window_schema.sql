-- // CB-33624 maintenance window related schemas
-- Migration SQL that makes the change goes here.

CREATE TABLE IF NOT EXISTS maintenance_window_schedule (
   id                  BIGSERIAL PRIMARY KEY,
   account_id          VARCHAR(255) NOT NULL,
   name                VARCHAR(255) NOT NULL,
   scope_type          VARCHAR(50) NOT NULL, -- ENVIRONMENT, TENANT, DATAHUB, DATALAKE, FREEIPA
   scope_id            VARCHAR(255) NOT NULL, -- Polymorphic scope key (tenant accountId, environment CRN, or resource CRN per scope_type)
   recurrence_kind     VARCHAR(32) NOT NULL, -- WEEKLY, MONTHLY_NTH_WEEKDAY, MONTHLY_DAY_OF_MONTH, CRON
   timezone            VARCHAR(64) NOT NULL DEFAULT 'UTC',
   description         TEXT,
   duration_minutes    INTEGER NOT NULL,
   start_local_time    VARCHAR(5),           -- HH:mm, required for WEEKLY, MONTHLY_NTH_WEEKDAY, and MONTHLY_DAY_OF_MONTH
   day_of_week         VARCHAR(16),          -- MONDAY..SUNDAY, required for WEEKLY and MONTHLY_NTH_WEEKDAY
   week_ordinal        INTEGER,              -- 1-5, required for MONTHLY_NTH_WEEKDAY (e.g. 3 = third)
   day_of_month        INTEGER,              -- 1-31, required for MONTHLY_DAY_OF_MONTH (e.g. 15 = 15th)
   cron_dialect        VARCHAR(16) DEFAULT 'QUARTZ',
   cron_expression     VARCHAR(255),         -- required for CRON
   created_at          BIGINT NOT NULL,
   updated_at          BIGINT NOT NULL,
   created_by          VARCHAR(255) NOT NULL,
   updated_by          VARCHAR(255),
   archived            BOOLEAN NOT NULL DEFAULT false,
   version             INTEGER NOT NULL DEFAULT 1,
   CONSTRAINT maintenance_window_schedule_recurrence_kind_chk
       CHECK (recurrence_kind IN ('WEEKLY', 'MONTHLY_NTH_WEEKDAY', 'MONTHLY_DAY_OF_MONTH', 'CRON')),
   CONSTRAINT maintenance_window_schedule_duration_chk
       CHECK (duration_minutes >= 60),
   CONSTRAINT maintenance_window_schedule_weekly_chk
       CHECK (recurrence_kind != 'WEEKLY'
           OR (start_local_time IS NOT NULL AND day_of_week IS NOT NULL)),
   CONSTRAINT maintenance_window_schedule_monthly_nth_weekday_chk
       CHECK (recurrence_kind != 'MONTHLY_NTH_WEEKDAY'
           OR (start_local_time IS NOT NULL AND day_of_week IS NOT NULL
               AND week_ordinal IS NOT NULL AND week_ordinal BETWEEN 1 AND 5)),
   CONSTRAINT maintenance_window_schedule_monthly_day_of_month_chk
       CHECK (recurrence_kind != 'MONTHLY_DAY_OF_MONTH'
           OR (start_local_time IS NOT NULL AND day_of_month IS NOT NULL AND day_of_month BETWEEN 1 AND 31)),
   CONSTRAINT maintenance_window_schedule_cron_chk
       CHECK (recurrence_kind != 'CRON' OR cron_expression IS NOT NULL)
);

CREATE INDEX IF NOT EXISTS maintenance_window_schedule_account_id
   ON maintenance_window_schedule (account_id);

CREATE INDEX IF NOT EXISTS maintenance_window_schedule_account_active
   ON maintenance_window_schedule (account_id)
   WHERE archived = false;

CREATE INDEX IF NOT EXISTS maintenance_window_schedule_account_scope
   ON maintenance_window_schedule (account_id, scope_type, scope_id);

CREATE UNIQUE INDEX IF NOT EXISTS maintenance_window_schedule_active_scope
   ON maintenance_window_schedule (account_id, scope_type, scope_id)
   WHERE archived = false;

CREATE TABLE IF NOT EXISTS maintenance_window_skip (
   id                      BIGSERIAL PRIMARY KEY,
   maintenance_schedule_id BIGINT NOT NULL REFERENCES maintenance_window_schedule (id) ON DELETE CASCADE,
   window_start            BIGINT NOT NULL,
   window_end              BIGINT NOT NULL,
   timezone                VARCHAR(64) NOT NULL DEFAULT 'UTC',
   created_at              BIGINT NOT NULL,
   created_by              VARCHAR(255) NOT NULL,
   reason                  TEXT,
   CONSTRAINT maintenance_window_skip_window_order
       CHECK (window_end > window_start)
);

CREATE INDEX IF NOT EXISTS maintenance_window_skip_schedule_id
   ON maintenance_window_skip (maintenance_schedule_id);

CREATE UNIQUE INDEX IF NOT EXISTS maintenance_window_skip_schedule_window
   ON maintenance_window_skip (maintenance_schedule_id, window_start);

CREATE TABLE IF NOT EXISTS maintenance_window_task (
   id                      BIGSERIAL PRIMARY KEY,
   account_id              VARCHAR(255) NOT NULL,
   resource_crn            VARCHAR(255) NOT NULL,
   environment_crn         VARCHAR(255) NOT NULL,
   task_type               VARCHAR(64) NOT NULL,
   work_item_id            VARCHAR(255) NOT NULL, -- (e.g. secret id, runtime:7.2.18);
   task_kind               VARCHAR(16) NOT NULL,  -- EVERY_WINDOW | ONE_SHOT
   status                  VARCHAR(16) NOT NULL,  -- ACTIVE | DISABLED | COMPLETED | DELETED
   submitter_service       VARCHAR(64) NOT NULL,
   task_payload            TEXT,
   execution_ref           TEXT NOT NULL,
   priority                INTEGER NOT NULL DEFAULT 100,
   depends_on_task_id      BIGINT REFERENCES maintenance_window_task (id) ON DELETE SET NULL,
   retry_within_occurrence BOOLEAN NOT NULL DEFAULT false,
   max_attempts_per_occurrence INTEGER NOT NULL DEFAULT 1,
   retry_cooldown_minutes  INTEGER NOT NULL DEFAULT 0,
   created_at              BIGINT NOT NULL,
   updated_at              BIGINT NOT NULL,
   created_by              VARCHAR(255) NOT NULL,
   updated_by              VARCHAR(255),
   disabled_at             BIGINT,
   completed_at            BIGINT,
   version                 INTEGER NOT NULL DEFAULT 1,
   CONSTRAINT maintenance_window_task_task_kind_chk
       CHECK (task_kind IN ('EVERY_WINDOW', 'ONE_SHOT')),
   CONSTRAINT maintenance_window_task_status_chk
       CHECK (status IN ('ACTIVE', 'DISABLED', 'COMPLETED', 'DELETED')),
   CONSTRAINT maintenance_window_task_priority_chk
       CHECK (priority >= 0),
   CONSTRAINT maintenance_window_task_no_self_dependency_chk
       CHECK (depends_on_task_id IS NULL OR depends_on_task_id != id),
   CONSTRAINT maintenance_window_task_max_attempts_chk
       CHECK (max_attempts_per_occurrence >= 1),
   CONSTRAINT maintenance_window_task_retry_cooldown_chk
       CHECK (retry_cooldown_minutes >= 0)
);

CREATE UNIQUE INDEX IF NOT EXISTS maintenance_window_task_one_active_per_work_item
   ON maintenance_window_task (account_id, resource_crn, task_type, work_item_id)
   WHERE status = 'ACTIVE';

CREATE INDEX IF NOT EXISTS maintenance_window_task_work_item_lookup
   ON maintenance_window_task (account_id, resource_crn, task_type, work_item_id, status);

CREATE INDEX IF NOT EXISTS maintenance_window_task_status_dispatcher
   ON maintenance_window_task (status, priority DESC, created_at);

CREATE INDEX IF NOT EXISTS maintenance_window_task_depends_on_task_id
   ON maintenance_window_task (depends_on_task_id)
   WHERE depends_on_task_id IS NOT NULL;

CREATE INDEX IF NOT EXISTS maintenance_window_task_account_id
   ON maintenance_window_task (account_id);

CREATE INDEX IF NOT EXISTS maintenance_window_task_resource_crn
   ON maintenance_window_task (resource_crn);

CREATE TABLE IF NOT EXISTS maintenance_window_run (
   id                         BIGSERIAL PRIMARY KEY,
   account_id                 VARCHAR(255) NOT NULL,
   maintenance_window_task_id BIGINT NOT NULL REFERENCES maintenance_window_task (id) ON DELETE CASCADE,
   resource_crn               VARCHAR(255) NOT NULL,
   maintenance_schedule_id    BIGINT REFERENCES maintenance_window_schedule (id) ON DELETE SET NULL,
   window_start               BIGINT NOT NULL,
   window_end                 BIGINT NOT NULL,
   status                     VARCHAR(50) NOT NULL, -- PLANNED, RUNNING, COMPLETED, SKIPPED, FAILED
   policy_revision            VARCHAR(128) NOT NULL,
   created_at                 BIGINT NOT NULL,
   updated_at                 BIGINT NOT NULL,
   window_execution_start     BIGINT,
   window_execution_end       BIGINT,
   error_detail               TEXT,
   version                    INTEGER NOT NULL DEFAULT 1,
   CONSTRAINT maintenance_window_run_window_order
       CHECK (window_end > window_start),
   CONSTRAINT maintenance_window_run_one_per_window
       UNIQUE (maintenance_window_task_id, window_start)
);

CREATE INDEX IF NOT EXISTS maintenance_window_run_account_id
   ON maintenance_window_run (account_id);

CREATE INDEX IF NOT EXISTS maintenance_window_run_resource_crn
   ON maintenance_window_run (resource_crn);

CREATE INDEX IF NOT EXISTS maintenance_window_run_status
   ON maintenance_window_run (status);

-- //@UNDO

DROP TABLE IF EXISTS maintenance_window_run;
DROP TABLE IF EXISTS maintenance_window_task;
DROP TABLE IF EXISTS maintenance_window_skip;
DROP TABLE IF EXISTS maintenance_window_schedule;
