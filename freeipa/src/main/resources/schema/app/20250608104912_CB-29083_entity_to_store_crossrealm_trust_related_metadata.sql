-- // CB-29083 Entity to store cross-realm trust related metadata
-- Migration SQL that makes the change goes here.

CREATE SEQUENCE IF NOT EXISTS crossrealmtrust_id_seq START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

CREATE TABLE crossrealmtrust (
  id                BIGSERIAL NOT NULL,
  stack_id          BIGSERIAL NOT NULL,
  environmentcrn    VARCHAR(255) NOT NULL,
  fqdn    VARCHAR(255) NOT NULL,
  ip      VARCHAR(255) NOT NULL,
  realm   VARCHAR(255) NOT NULL,

  CONSTRAINT        pk_crossrealmtrust_id          PRIMARY KEY (id),
  CONSTRAINT        fk_stack_id             FOREIGN KEY (stack_id) REFERENCES stack(id)
);

CREATE UNIQUE INDEX idx_crossrealmtrust_stack_id ON crossrealmtrust (stack_id);
CREATE UNIQUE INDEX idx_crossrealmtrust_envcrn ON crossrealmtrust (environmentcrn);

-- //@UNDO
-- SQL to undo the change goes here.
DROP INDEX IF EXISTS idx_crossrealmtrust_envcrn;
DROP INDEX IF EXISTS idx_crossrealmtrust_stackid;
DROP TABLE IF EXISTS crossrealmtrust;