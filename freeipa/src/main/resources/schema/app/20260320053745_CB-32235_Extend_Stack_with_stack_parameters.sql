-- // CB-32235 Extend Stack with stack parameters
-- Migration SQL that makes the change goes here.

CREATE TABLE IF NOT EXISTS stack_parameter
(
    stack_id BIGINT NOT NULL,
    paramKey VARCHAR(255) NOT NULL,
    paramValue TEXT,
    CONSTRAINT stack_parameter_pkey PRIMARY KEY (stack_id, paramKey),
    CONSTRAINT "FK_stack_parameter" FOREIGN KEY (stack_id) REFERENCES stack (id)
);

-- //@UNDO
-- SQL to undo the change goes here.

DROP TABLE IF EXISTS stack_parameter;
