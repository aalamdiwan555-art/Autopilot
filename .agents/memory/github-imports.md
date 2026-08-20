---
name: GitHub imports
description: Durable guidance for importing an existing repository into the workspace and publishing changes safely.
---

When importing a repository into a workspace with its own Git history, fetch the destination branch before publishing and merge remote commits rather than force-pushing.

**Why:** The workspace history and the repository history can diverge even when the source files look similar; a direct push can be rejected or overwrite work that already exists upstream.

**How to apply:** Preserve the remote tip, resolve only intentional file conflicts, verify the requested changes remain present, then push the merged result.