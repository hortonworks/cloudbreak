---
name: cb-db-migration
description: Add a database schema migration in Cloudbreak using MyBatis Migrations — correct file location, timestamp+ticket naming, the mandatory @UNDO section, and idempotent/reversible DDL. Use when changing the schema of cbdb/datalakedb/freeipadb/redbeamsdb/environmentdb or any service DB.
---

# Cloudbreak DB Migrations (MyBatis Migrations)

Each service owns its database and a directory of forward-only, timestamp-ordered SQL migration files. Migrations are applied with `cbd migrate` (cbd = cloudbreak-deployer). This skill is the procedure for adding one; `core/AGENTS.md` states the `@UNDO` mandate.

## Where migrations live

`<service>/src/main/resources/schema/app/` — one `.sql` file per change, applied in filename order. The MyBatis bookkeeping table is created once by `schema/mybatis/20150421140021_create_changelog.sql`; don't touch that.

| Service | DB name | Migration dir |
|---|---|---|
| core | `cbdb` | `core/src/main/resources/schema/app/` |
| datalake | `datalakedb` | `datalake/src/main/resources/schema/app/` |
| freeipa | `freeipadb` | `freeipa/src/main/resources/schema/app/` |
| redbeams | `redbeamsdb` | `redbeams/src/main/resources/schema/app/` |
| environment | `environmentdb` | `environment/src/main/resources/schema/app/` |
| externalized-compute | `externalizedcomputedb` | `externalized-compute/src/main/resources/schema/app/` |

## File naming

`YYYYMMDDhhmmss_CB-NNNNN_short_description.sql` — UTC timestamp prefix (this is what orders migrations), Jira ticket, then an underscore description. Always include the `.sql` extension (a few legacy files omit it — don't copy that). The timestamp must sort **after** every existing file in the dir; generate it from the current UTC time, e.g. `date -u +%Y%m%d%H%M%S`.

Real examples:
```
20250313141002_CB-28882_add_index_to_structuredevent_table.sql
20250507124100_CB-26976_add_privateid_column_to_resource_table.sql
20260318123014_CB-32106_Implement_instanceType_fallback_for_aws_native.sql
```

## File template (the `@UNDO` section is mandatory)

```sql
-- // CB-NNNNN short description
-- Migration SQL that makes the change goes here.

ALTER TABLE template ADD COLUMN IF NOT EXISTS fallbackInstanceTypes TEXT;

-- //@UNDO
-- SQL to undo the change goes here.

ALTER TABLE template DROP COLUMN IF EXISTS fallbackInstanceTypes;
```

The `-- //@UNDO` marker (exact spelling) splits forward SQL from rollback SQL. Every forward change MUST have a real, reversing `@UNDO` — not an empty stub. A migration without a correct `@UNDO` will be flagged in review.

## DDL rules

- **Idempotent** — use the `IF [NOT] EXISTS` idioms the repo standardizes on so a partial re-run is safe:
  `ADD COLUMN IF NOT EXISTS`, `DROP COLUMN IF EXISTS`, `CREATE TABLE IF NOT EXISTS`, `CREATE [UNIQUE] INDEX IF NOT EXISTS`, `DROP CONSTRAINT IF EXISTS`, `DROP SEQUENCE IF EXISTS`, `DROP TABLE IF EXISTS`.
- **Reversible** — the `@UNDO` must restore the prior state (drop what you added, etc.).
- **Justify indexes** — reviewers ask "why isn't an existing index sufficient?"; check current indices before adding one, and name it consistently (`idx_<table>_<cols>` / `<col>_idx`).
- **Watch renames** — if you rename a constraint/index/column, find and update every code reference to the old name.
- **Prod awareness** — schema already in production can't simply be re-pointed; if a prior migration was reverted-but-already-executed on prod, prefer the safe multi-step path and call it out in the PR. Changing a column type or `NOT NULL` on a populated table needs a data-migration plan.
- **Avoid operations that take a long table lock** on large/populated tables — they block reads/writes for the whole migration and can stall a running service. Watch for the Postgres operations that acquire `ACCESS EXCLUSIVE` and/or rewrite/scan the table:
  - `ALTER COLUMN ... TYPE` (full rewrite), adding a column `WITH` a volatile/non-constant `DEFAULT`, adding `NOT NULL` to an existing column (full scan), and `CREATE INDEX` without `CONCURRENTLY` (blocks writes).
  - Prefer the non-blocking multi-step pattern: add the column **nullable** → backfill in batches → set `NOT NULL` (or keep enforcement in code); add constraints/FKs as `NOT VALID` first, then `VALIDATE CONSTRAINT` in a later step; create indexes with `CREATE INDEX CONCURRENTLY` where the runner allows it.
  - When a lock is unavoidable, say so in the PR and size the affected table so reviewers can weigh the downtime.
- **One concern per migration file**; don't bundle unrelated schema changes.

## Apply & test locally

`cbd` lives at `integration-test/integcb/cbd`. Typical local cycle:

```bash
./cbd migrate <dbname> pending     # apply pending migrations (e.g. cbdb, environmentdb)
./cbd migrate <dbname> down 1      # roll back the last one — verifies your @UNDO works
```

Always run the `down` step to prove the `@UNDO` reverses cleanly, then re-apply. The corresponding JPA entity/MyBatis mapping change ships in the same PR as the migration.
