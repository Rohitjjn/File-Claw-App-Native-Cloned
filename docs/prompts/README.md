# File Claw prompt index

This is the canonical index for improvement-loop prompts. Legacy `NEXT_LOOP_PROMPT_v*.md` files are retained as compatibility pointers/history.

## Active scope
- **Loop 8 — current implementation:** [LOOP-08-four-format-premium-viewers.md](LOOP-08-four-format-premium-viewers.md)
- **Next planned loop:** [LOOP-09-four-format-reader-polish.md](LOOP-09-four-format-reader-polish.md)

## History
- [Loop 7 — semantic outline and reliability](LOOP-07-semantic-outline.md)
- [Prompt v2 — autonomous improvement](archive/PROMPT-V2-autonomous-improvement.md)
- [Prompt v3 — autonomous improvement](archive/PROMPT-V3-autonomous-improvement.md)
- [Previous Loop 8 proposal — reader reliability](archive/LOOP-08-reader-reliability-proposal.md)

## Operating rules
- Current product scope is PDF, DOCX, TXT and Markdown only.
- Read `docs/TODO.md` and current CI results before coding.
- Use `.github/workflows/android-tests.yml` for test gates; never publish a release from test CI.
- [Hermes Agent](https://github.com/NousResearch/hermes-agent) is a public MIT-licensed optional orchestrator with subagent delegation. It is not an Android runtime dependency; don't claim agent counts/concurrency without actual runtime evidence.
- Update docs only against the actual tested branch and mark unrun checks as pending.
