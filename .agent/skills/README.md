# Agent playbooks (tool-neutral content)

These directories contain **`SKILL.md`** files plus optional **`references/`** material. The content is **Markdown** and is meant to be usable by **any** coding assistant or similar automation.

- **YAML front matter** at the top of each `SKILL.md` (`name`, `description`) is optional metadata for tools that index skills by name and description. Other tools can ignore it; the headings and bullet rules below it are the real playbook.
- **Path**: Canonical playbooks live under **`.agent/skills/`**. If your tool discovers skills only under a different directory, configure it to use this path, or see your team’s setup notes in the root **`AGENTS.md`**.
- **Edits**: Change the Markdown like any other doc. If you add a playbook, mirror the existing layout (`<skill-id>/SKILL.md`) and list it in the root **`AGENTS.md`** so the team can find it.
