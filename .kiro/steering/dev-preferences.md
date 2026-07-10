---
inclusion: always
---

# Developer Preferences

## Portability

- **Never hardcode user-specific paths** in any config, script, or file (e.g., no `/Users/<username>/...`).
- Use environment variables (`$HOME`, `${HOME}`, `~`) or generic PATH entries that work across machines and users.
- When adding paths to tool configs (e.g., MCP `env.PATH`), include common locations like `/usr/local/bin`, `/opt/homebrew/bin`, `${HOME}/.local/bin` rather than user-specific Python or system paths.
- Config files committed to the repo must be usable by any developer without modification.
