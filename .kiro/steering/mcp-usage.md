---
inclusion: always
---

# MCP Server Usage

This project configures a GitHub MCP server defined in `.kiro/settings/mcp.json`.

## GitHub MCP (`github`)

Runs via Docker using GitHub's official image with OAuth authentication (no PAT required).
On first use, a browser window opens for GitHub OAuth — authorize once and the token stays
in memory only.

**Requires**: Docker installed and running.

**Use for**: Creating commits, pushing branches, opening PRs, and other GitHub operations
during task execution.
