---
inclusion: manual
description: "Analyze changes, generate a conventional commit message from the branch name, stage all changes, commit, and push to the current feature branch."
---

You are helping commit and push the current working changes to Git.

## Steps

1. **Check current branch** by running `git branch --show-current`.

2. **If the branch is `main`**: Ask the user to confirm before proceeding — pushing directly to `main` is unusual and potentially risky.

3. **Review changes** by running `git status && git diff HEAD` in the terminal — do NOT rely on the chat context or open editor files to infer what changed. Always run these commands to get the authoritative list of modified, added, and deleted files before proceeding.

4. **Generate a commit message** using the Conventional Commits format:
   - **Primary source**: derive the message from the branch name, since branches are named after the current task (e.g., `feature/task-1-light-state-model` → `feat(model): add light state model`).
   - **Supplement with diff**: use `git diff HEAD` to enrich the summary or body with specifics about what changed, but the branch name drives the type, scope, and high-level summary.
   - Format: `<type>(<scope>): <short summary>`
   - Types: `feat`, `fix`, `refactor`, `style`, `test`, `docs`, `chore`, `build`
   - Scope: the module or area from the branch name (e.g., `model`, `controller`, `ui`, `data`, `build`)
   - Summary: imperative mood, lowercase, no trailing period, max ~72 chars
   - Add a body if the change is non-trivial — explain *what* and *why*, not *how*
   - Example: branch `feature/task-3-preferences-repository` → `feat(data): add preferences repository with clamp validation`

5. **Stage all changes**: `git add .`

6. **Commit** with the generated message: `git commit -m "<message>"`

7. **Push** to the current branch: `git push origin <branch-name>`
   - If the branch has no upstream yet, use `git push -u origin <branch-name>`

8. **Report** the final commit SHA and branch that was pushed.
