-- // Adds version, environmentlist, and userlist columns to syncoperation table

ALTER TABLE syncoperation
  ADD COLUMN IF NOT EXISTS version bigint,
  ADD COLUMN IF NOT EXISTS environmentlist text,
  ADD COLUMN IF NOT EXISTS userlist text;

UPDATE syncoperation SET version = 1 WHERE version IS NULL;
UPDATE syncoperation SET environmentlist = '[]' WHERE environmentlist IS NULL;
UPDATE syncoperation SET userlist = '[]' WHERE userlist IS NULL;

CREATE INDEX IF NOT EXISTS syncoperation_accountid_syncoperationtype_endtime_idx
  on syncoperation (accountid, syncoperationtype, endtime)

-- //@UNDO

ALTER TABLE syncoperation DROP COLUMN IF EXISTS version;
