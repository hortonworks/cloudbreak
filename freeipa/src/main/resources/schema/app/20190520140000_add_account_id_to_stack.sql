-- // Add accountid Column

-- This change is to add accountid col into stack table.


ALTER TABLE stack
    ADD accountid VARCHAR(255);

CREATE INDEX accountid_idx
    ON stack (accountid);

CREATE INDEX environment_accountid_idx
    ON stack (environment, accountid);

CREATE UNIQUE INDEX name_environment_accountid_uindex
    ON stack (name, environment, accountid);

DROP INDEX stack_name_environment_uindex;


-- //@UNDO
CREATE UNIQUE INDEX stack_name_environment_uindex
    ON stack (name, environment);


DROP INDEX accountid_idx;

DROP INDEX environment_accountid_idx;

DROP INDEX name_environment_accountid_uindex;

ALTER TABLE stack DROP COLUMN accountid;