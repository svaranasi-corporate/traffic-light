# Task Execution Workflow

## Rules for Working on Tasks

- Work on **one task at a time**. Do not start a new task until the current one is fully complete and checked off.
- Before writing any code for a task, **summarize the task** for the user in plain language: what feature(s) it adds to the app and what the user will be able to do once it is done.
- **Create a feature branch** before writing any code for a task. Name the branch `feature/task-<N>-<short-description>` (e.g. `feature/task-1-light-state-model`). All changes for the task go on this branch.
- Work strictly from the acceptance criteria in `tasks.md`. Each criterion is a concrete coding deliverable — implement exactly what is listed, nothing more.
- After completing each acceptance criterion, **check it off** in `tasks.md` by changing `- [ ]` to `- [x]`.
- Only mark a task fully done when every acceptance criterion is checked off.
- Do not do design work, requirements work, or spec editing during task execution — stay focused on code.
