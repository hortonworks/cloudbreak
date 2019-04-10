-- // CB-783 Add CRN to users.

ALTER TABLE users
    ADD COLUMN usercrn VARCHAR(512);

-- //@UNDO

ALTER TABLE users
    DROP COLUMN usercrn;
