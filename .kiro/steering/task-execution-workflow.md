# Task Execution Workflow

## Implementation Policy

- **All app code must be written by the `task-executor` agent.** Do not write implementation code directly in a Vibe or Spec session.
- To start work, invoke the `task-executor` agent and tell it which task to work on, or let it pick the next incomplete task automatically.

## Rules (enforced by `task-executor`)

- One task at a time — never start a new task until the current one is fully complete and checked off.
- Feature branch required — all changes go on a `feature/task-<N>-<short-description>` branch, never directly on `main`.
- **Branch creation per session**: Create a new branch at the start of a task only if no branch for that task already exists in the current session. If the task's branch already exists and is checked out (i.e., feedback or follow-up changes are requested in the same chat session), continue committing to that existing branch — do not create a new one.
- Work strictly from the acceptance criteria in `tasks.md` — nothing more, nothing less.
- Each acceptance criterion is checked off in `tasks.md` as it is completed.
- No design work, requirements work, or spec editing during task execution.
