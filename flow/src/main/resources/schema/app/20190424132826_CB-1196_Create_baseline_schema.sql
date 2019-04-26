-- // CB-1196 Create baseline schema
-- Migration SQL that makes the change goes here.

-- // flowlog

CREATE TABLE IF NOT EXISTS flowlog (
  id bigserial NOT NULL,
  created int8 NOT NULL DEFAULT (date_part('epoch'::text, now()) * 1000::double precision),
  flowid varchar(255) NOT NULL,
  nextevent varchar(255) NULL,
  payloadtype varchar(255) NULL,
  flowtype varchar(255) NULL,
  currentstate varchar(255) NOT NULL,
  payload text NULL,
  stackid int8 NOT NULL,
  finalized bool NOT NULL,
  flowchainid varchar(255) NULL,
  variables text NULL,
  cloudbreaknodeid varchar(255) NULL,
  "version" int8 NULL,
  statestatus varchar(255) NULL,
  CONSTRAINT pk_flowlog PRIMARY KEY (id)
);
CREATE INDEX IF NOT EXISTS idx_flowlog_finalized ON flowlog USING btree (finalized);
CREATE INDEX IF NOT EXISTS idx_flowlog_finalized_stackid_flowtype ON flowlog USING btree (finalized, stackid, flowtype);
CREATE INDEX IF NOT EXISTS idx_flowlog_flowid ON flowlog USING btree (flowid);
CREATE INDEX IF NOT EXISTS idx_flowlog_statestatus ON flowlog USING btree (statestatus);

-- // flowchainlog

CREATE TABLE IF NOT EXISTS flowchainlog (
  id bigserial NOT NULL,
  created int8 NOT NULL DEFAULT (date_part('epoch'::text, now()) * 1000::double precision),
  flowchainid varchar(255) NOT NULL,
  parentflowchainid varchar(255) NULL,
  "chain" text NOT NULL,
  CONSTRAINT flowchainlog_pkey PRIMARY KEY (id)
);
CREATE INDEX IF NOT EXISTS idx_flowchainlog_flowchainid_created ON flowchainlog USING btree (flowchainid, created);

-- //@UNDO
-- SQL to undo the change goes here.

DROP INDEX IF EXISTS idx_flowchainlog_flowchainid_created;
DROP TABLE IF EXISTS flowchainlog;

DROP INDEX IF EXISTS idx_flowlog_statestatus;
DROP INDEX IF EXISTS idx_flowlog_flowid;
DROP INDEX IF EXISTS idx_flowlog_finalized_stackid_flowtype;
DROP INDEX IF EXISTS idx_flowlog_finalized;
DROP TABLE IF EXISTS flowlog;
