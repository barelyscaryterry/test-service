---
name: pre-pr-review
description: Staff/principal-level architecture and scale review of the current branch's changes, run before opening a pull request. Use before any `gh pr create` in this repo, or when the user asks to review changes before a PR, check for architectural flaws, or sanity-check scalability.
disable-model-invocation: false
---

# Pre-PR Review

You are reviewing this branch's changes the way a senior/staff engineer at a
top-tier tech company would review a teammate's PR before approving it — not
a linter pass, a design review. The bar is: would this hold up under 100x the
current traffic, a second instance, a bad deploy, or a compromised credential?
This project (`test-service`) is small today (one EC2 instance, one DynamoDB
table, one Beer API) but should be reviewed as if it is the seed of something
that has to scale — call out anything that only works because the system is
currently small.

## Step 1 — Scope the diff

Determine the target branch (default `main` unless the user says otherwise)
and the current branch. Get the full diff, not just the latest commit:

```bash
git fetch origin main --quiet 2>/dev/null || true
git diff origin/main...HEAD
git log origin/main..HEAD --oneline
```

Also `git status` to catch untracked new files the diff above won't show
(new `.tf` files, new resources, new config) — read every new file in full,
not just the tracked diff.

Read enough surrounding context (the full file, not just the changed hunks)
for anything non-trivial — a diff hunk out of context hides exactly the kind
of architectural problem this review exists to catch.

## Step 2 — Review through these lenses

Go through the changed files and, for each substantive change, actively ask:

**Correctness under concurrency and scale**
- Does this assume a single instance, single request, or single writer where
  there could eventually be many? (e.g. in-memory state, local files,
  non-idempotent writes, missing optimistic locking on read-modify-write
  patterns)
- Any unbounded operation that's fine at low volume but becomes an outage at
  scale — full table scans, unpaginated list calls, N+1 calls, loading
  everything into memory, synchronous fan-out with no concurrency limit?
- Any hardcoded capacity assumption (rate limits, thread pools, connection
  pools, timeouts) that was picked for today's load and never explained?

**Failure modes and blast radius**
- What happens when a downstream dependency (DynamoDB, an external API, the
  network) is slow or down? Does a retry/backoff/circuit-breaker/timeout
  exist, or does a failure cascade or hang indefinitely?
- Is there a single point of failure introduced or left unaddressed by this
  change?
- Does a partial failure (e.g. Terraform apply dying halfway through
  resource creation, a deploy racing an infra change) leave the system in a
  broken or ambiguous state? Check for race conditions between independent
  CI workflows/pipelines touching the same resources.

**Security and permission scope**
- Is any new IAM policy, role, or credential broader than the specific
  actions/resources this change needs? Flag wildcard resources/actions that
  could be scoped down (e.g. with a condition key), and say so even if the
  wildcard was unavoidable (some AWS actions, like `cloudwatch:PutMetricData`,
  don't support resource-level scoping — note that explicitly rather than
  flagging it as an oversight).
- Does a new capability (a new endpoint, a new IAM permission, a new
  credential path) expand what a compromised component could do?
- Any secrets, credentials, or account IDs introduced in code/config that
  shouldn't be there?

**Operational soundness**
- Is this change observable — will you know it broke without SSHing in? Are
  there metrics, logs, alarms, or dashboard widgets for new failure modes
  this change can introduce?
- Is there a safe rollback path? Does the change couple two systems (e.g. a
  CI role's permissions and the resources a different config now tries to
  manage) such that reverting one half breaks the other?
- Are new config values (rate limits, thresholds, retry counts, timeouts)
  documented with *why*, or just dropped in with magic numbers?

**Architecture and coupling**
- Does this change tightly couple components that should stay independent
  (e.g. hardcoding another module's resource name/ARN instead of passing it
  as a variable/output)?
- Is there duplicated logic or config that will drift (e.g. a value that
  must be kept in sync across two files with no enforcement)?
- Does this change make a future scaling step harder (e.g. an assumption
  baked in that breaks the moment there's a second instance, a second
  environment, or a second consumer of this API)?

Do not manufacture findings to fill out every lens — a small, well-scoped
change may legitimately have zero issues in most categories. Note what's
already handled well only if it's non-obvious (e.g. "correctly scoped IAM
condition here" is worth a one-line callout if it's easy to miss that it
matters); don't pad the report with generic praise.

## Step 3 — Verify each finding before reporting it

For every candidate finding, before including it: re-read the actual code/config
it's about and confirm the failure scenario is real, not theoretical noise.
Reject a finding if:
- it only applies at a scale or scenario the system will plausibly never see
  (calibrate to what "highly scalable" means for *this* service, not
  hypothetical planet-scale traffic a two-endpoint CRUD service will never get)
- it's already mitigated elsewhere in the diff or codebase (check before flagging)
- it's a pure style preference with no correctness, security, or scale angle

## Step 4 — Report

Use the `ReportFindings` tool if available in this session. Order findings
most-severe first (correctness/security/single-point-of-failure issues before
nice-to-haves). If `ReportFindings` isn't available, output the same
structure as plain text: file, one-sentence summary, concrete failure
scenario, severity.

After reporting, give a one-line verdict: clear to open the PR as-is, or
fix these first. Do not silently fix findings yourself — surface them and
let the user decide, unless they've explicitly asked you to also apply fixes.
