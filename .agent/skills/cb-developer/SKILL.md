---
name: cb-developer
description: Automated janitorial tasks for Cloudbreak developers. Use this skill to standardize imports, perform API audits (Boolean vs boolean), check Jakarta namespace usage, and help prepare commit messages.
---

# Cloudbreak Developer (Janitorial)

This skill provides automated workflows for common "janitorial" tasks in the Cloudbreak project.

## Janitorial Workflows

### 1. Standardize Imports
Use this workflow to reorder imports according to the project mandate:
1. Static all other imports
2. `java.*`
3. `javax.*`
4. `jakarta.*`
5. `org.*`
6. `com.*`
7. All other imports

**Command example**: "Standardize imports for `core/src/main/java/com/sequenceiq/cloudbreak/SomeClass.java`."

### 2. API Audit (Boolean vs boolean)
Identify and suggest fixes for `Boolean` usage in API classes (those with `@Schema` or `@ApiModel` annotations).
- Prefer primitive `boolean`.
- If `Boolean` is necessary, ensure a default value is provided.

**Command example**: "Run an API audit on the `core-api` module."

### 3. Jakarta Namespace Check
Verify that `jakarta.*` is used instead of `javax.*` (except for specific legacy exclusions like `javax.annotation.Generated`).
- Focus on `jakarta.persistence`, `jakarta.validation`, `jakarta.servlet`.

**Command example**: "Check for `javax.*` leaks in the `datalake` module."

### 4. Commit Message Helper
Format the current staged changes or a summary of work into the strict `CB-XXXXX Subject` format.
- Ensure the subject is under 72 chars.
- Ensure the body is wrapped at 72 chars.
- Use imperative mood in the subject.

### 6. Senior Audit (Post-Fix)
After a fix is implemented by `cb-junior-dev`, use this workflow to audit the changes for:
-   **Flow State Leaks**: Ensure that any new `FlowLog` or `State` transitions correctly cleanup resources.
-   **MyBatis Consistency**: Verify that any changes to `repository` or `dao` classes follow the project's MyBatis patterns and schema migration standards.
-   **Jakarta Usage**: A final check for `javax.*` leaks.

**Command example**: "Perform a Senior Audit on the `core` module's flow package."

## Best Practices
- Always run `gradlew check` or relevant unit tests after applying janitorial changes.
- Focus changes on a single sub-module to avoid massive PRs.
