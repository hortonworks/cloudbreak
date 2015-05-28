-- // CLOUD-785 distributed storage accounts on azure
-- Migration SQL that makes the change goes here.

CREATE TABLE stack_parameters
(
   stack_id   BIGINT NOT NULL,
   value      TEXT,
   key        VARCHAR (255) NOT NULL,
   PRIMARY KEY (stack_id, key)
);

ALTER TABLE stack_parameters ADD CONSTRAINT "FK_stack_parameters_1" FOREIGN KEY (stack_id) REFERENCES stack (id);

-- //@UNDO
-- SQL to undo the change goes here.


