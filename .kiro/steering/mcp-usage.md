---
inclusion: always
---

# MCP Server Usage

This project configures two MCP servers defined in `.kiro/settings/mcp.json`.

## GitHub MCP (`github`)

Runs via Docker using GitHub's official image with OAuth authentication (no PAT required).
On first use, a browser window opens for GitHub OAuth — authorize once and the token stays
in memory only.

**Requires**: Docker installed and running.

**Use for**: Creating commits, pushing branches, opening PRs, and other GitHub operations
during task execution.

---

## Fetch MCP (`fetch`)

Runs via `uvx` (Python, no Node required). Fetches live web pages as clean markdown.

**Requires**: `uv` installed and `uvx` on PATH.
Install via the official installer (recommended — puts `uvx` in `~/.local/bin`):
```
curl -LsSf https://astral.sh/uv/install.sh | sh
```
Avoid `pip3 install uv` — it places binaries in a Python-version-specific path that breaks when Python is upgraded.

**Use for**: Pulling current Android developer reference docs by exact URL — ensuring API
signatures and behavior are up to date rather than relying on training data.

**Example URLs for this project:**
- `https://developer.android.com/reference/android/animation/ValueAnimator`
- `https://developer.android.com/reference/android/view/WindowInsetsController`
- `https://developer.android.com/reference/androidx/compose/material3/package-summary`
- `https://developer.android.com/reference/android/view/View#KEEP_SCREEN_ON`

**When NOT to use**: For well-known stable APIs or project-specific logic — read source
files directly or rely on training data. Only fetch when there's genuine uncertainty about
current API behavior or signatures.
