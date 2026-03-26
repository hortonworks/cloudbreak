# SaltStack Orchestrator Mandates

This file contains foundational mandates for Gemini agents in the `orchestrator-salt` module. These instructions take precedence over general defaults.

## 🧂 SaltStack Core Concepts
SaltStack is used in Cloudbreak for configuration management and remote execution on cluster nodes.

- **Grains**: Static information about the underlying system (minion) collected by the minion (e.g., OS, CPU, roles).
- **Pillars**: Global configuration values defined on the Salt Master and distributed to specific minions (e.g., secrets, configuration variables).
- **States (Salt States)**: YAML files (`.sls`) describing the "desired state" of a system (e.g., package installation, service running).
- **Modules**: Functions for remote execution tasks on minions.
- **Runners**: Applications executed on the Salt Master for orchestration and system management.

## 📂 Logical Directory Structure
- **`src/main/java/com/sequenceiq/cloudbreak/orchestrator/salt/`**: Java implementation of the Salt orchestrator.
  - `SaltOrchestrator.java`: The primary entry point for triggering Salt operations from Cloudbreak.
  - `client/`: Salt API client implementation.
  - `states/`: Logic for managing Salt states from Java.
- **`src/main/resources/salt/salt/`** and **`src/main/resources/salt-common/salt/`**: Salt state files (`.sls`).
  - `top.sls`: The main mapping of states to minion targets.
  - Subdirectories (e.g., `postgresql/`, `cloudera/`, `nginx/`) contain component-specific states.
- **`src/main/resources/salt/pillar/`** and **`src/main/resources/salt-common/pillar/`**: Salt pillar configuration files (`.sls`).
  - `top.sls`: The main mapping of pillars to minion targets.

## ✅ Operational Mandates & Conventions
1. **Jinja Templating**: Almost all `.sls` files use Jinja2 for dynamic content. Use `{% ... %}` for logic and `{{ ... }}` for variable expansion.
2. **Minion Targeting**: Use Salt Grains (e.g., `G@roles:manager_server`) or Pillars (e.g., `salt['pillar.get'](...)`) in `top.sls` and states to target specific nodes.
3. **Java Integration**: When modifying orchestration logic, ensure it aligns with the `SaltOrchestrator` interface.
4. **Backward Compatibility**: Be cautious when changing Pillar structures, as they are used across different cluster versions.
5. **Secrets**: Always use Pillars for sensitive information (passwords, keys, etc.). Never hardcode secrets in Salt states.

## 🔗 Official Documentation
- [SaltStack Documentation](https://docs.saltproject.io/en/latest/contents.html)
