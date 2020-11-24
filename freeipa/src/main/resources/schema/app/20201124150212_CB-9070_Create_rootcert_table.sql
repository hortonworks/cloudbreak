-- // CB-9070 Create rootcert table
-- Migration SQL that makes the change goes here.

CREATE SEQUENCE IF NOT EXISTS rootcert_id_seq START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

CREATE TABLE rootcert (
  id                BIGSERIAL NOT NULL,
  stack_id           BIGSERIAL NOT NULL,
  environmentcrn    VARCHAR(255) NOT NULL,
  cert              TEXT,

  CONSTRAINT        pk_rootcert_id          PRIMARY KEY (id),
  CONSTRAINT        fk_stack_id             FOREIGN KEY (stack_id) REFERENCES stack(id)
);

CREATE UNIQUE INDEX idx_rootcert_stack_id ON rootcert (stack_id);
CREATE UNIQUE INDEX idx_rootcert_envcrn ON rootcert (environmentcrn);

-- //@UNDO
-- SQL to undo the change goes here.
DROP INDEX IF EXISTS idx_rootcert_envcrn;
DROP INDEX IF EXISTS idx_rootcert_stackid;
DROP TABLE IF EXISTS rootcert;

