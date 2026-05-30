# Milestone Three Narrative — Algorithms and Data Structure

**Course:** CS 499, Computer Science Capstone — Southern New Hampshire University
**Milestone:** Three (Enhancement Category Two — Algorithms and Data Structure)
**Artifact:** Weigh to Go! — repository <https://github.com/rgoshen-snhu/WeighToGo>
**Tag:** `v0.2.0` (this milestone) · prior milestone `v0.1.0` · Android baseline `v1.0.0-android`
**Author:** Rick Goshen

> Render to Word for submission:
>
> ```bash
> cd docs/narratives
> pandoc milestone-three-narrative.md -o milestone-three-narrative.docx \
>     --reference-doc=reference.docx   # optional — for SNHU style
> ```
>
> The `.docx` output is git-ignored (see `.gitignore`); the markdown is the
> single source of truth.

---

## 1. Briefly describe the artifact. What is it? When was it created?

The artifact is **Weigh to Go!**, a weight-tracking application I originally
built as a native Android app for **CS 360 (Mobile Architecture and
Programming)** in 2025, and which I am rebuilding as a full-stack web
application for the CS 499 capstone. The web rebuild uses React 19 with
TypeScript on the frontend and FastAPI on Python 3.12 with SQLAlchemy 2.0 and
PostgreSQL on the backend, in a polyglot monorepo that preserves the original
Java code under `android/` and hosts the rebuild under `web/`.

This narrative covers **Milestone Three (Algorithms and Data Structure)**, which
adds the algorithmic core that Milestone Two deliberately deferred. Where M2
delivered the architecture and a CRUD vertical slice (`v0.1.0`), M3 fills that
architecture with the domain logic it was designed to host: goal management,
achievement detection (milestone and streak), weekly rate-of-change and a
weight-trend chart, user preferences, composite indexes for the time-series read
paths, and a TTL cache for the dashboard read model. Milestone Three is tagged
`v0.2.0`.

---

## 2. Justify the inclusion of the artifact in your ePortfolio

### Why this artifact

The Milestone Two narrative made a specific promise: that M3 would supply the
algorithm and data-structure work the architecture was built to receive — a
trend computation for the dashboard chart, a milestone-detection algorithm for
goal-progress achievements, a streak-detection algorithm for consecutive-day
logging, a composite-index strategy for the time-series read paths, and a
TTL-based cache for the dashboard read model. This milestone delivers each of
those, which makes the artifact a direct, traceable demonstration of the
algorithms-and-data-structure category: the work was named in advance, scoped in
the SRS, and can be checked against the commits and tests that implement it.

### Components that showcase algorithms and data-structure skills

- **Streak-detection algorithm (ADR-0022).** Consecutive-day logging streaks are
  computed with a single-pass longest-run scan over a set-backed, sorted date
  sequence — O(n) time after the sort, O(n) space. The ADR documents the choice
  of a *longest-run* scan over a fixed *rolling-window* count, and why a `set`
  membership structure (O(1) "did the user log on day D?") backs the scan rather
  than repeated list traversal.

- **Milestone-detection algorithm (ADR-0019).** Goal-progress milestones are
  detected on every weight entry. The ADR captures the detection rule, the
  data the algorithm reads, and the complexity of running it inline on the write
  path rather than batch-recomputing history.

- **Composite-index strategy (ADR-0021).** The weekly rate-of-change (FR-D-3) is
  computed from two indexed lookups against composite indexes on the
  weight-history table, rather than scanning the user's full series. The ADR
  reasons about index column order and which read paths each index serves — a
  data-structure decision expressed at the database layer.

- **Opaque compound cursor (ADR-0015).** Carried in from M2 as the
  data-structure exemplar the milestone narrative is asked to cite: an opaque,
  compound cursor that paginates the time-series table without leaking schema or
  skipping rows at date-tie boundaries. The cursor design is what the
  goal-history and trend reads in M3 build on.

- **TTL cache (ADR-0023).** The dashboard summary is cached in a small TTL
  structure — a dictionary keyed by user id, mapping to `(value, monotonic
  deadline)` pairs, with O(1) get/set, a bounded `maxsize` with expired-first
  eviction, lazy expiry on read, and invalidation on the writes that change the
  summary. The ADR documents the staleness-versus-cost trade-off and the
  invalidation policy explicitly.

- **Preferences storage (ADR-0020) and the trend chart (DDR-0006).** A
  key/value preference structure drives display (including preference-driven
  unit conversion), and the dashboard renders a weight-trend chart over the
  time series.

### How the artifact was improved

M3 turned a CRUD application into one that *reasons* about a user's data:
detecting achievements, computing rate of change, recognizing streaks, and
visualizing trends — each as framework-free domain logic with documented
time and space complexity, written test-first. The work landed as four
independent vertical slices, each independently code-reviewed and
security-reviewed before merge; that review pass caught a real concurrency
defect in the cache (a non-atomic expiry that could raise under the threadpool)
and confirmed the cache key is strictly per-user, which matters because a shared
cache is exactly where a cross-user data leak would hide. The backend carries
592 tests at 97% coverage, the frontend 377 tests, and the end-to-end suite 19
Playwright specs.

---

## 3. Did you meet the course outcomes you planned to meet in Module One?

**Outcome 3 (design and evaluate computing solutions using algorithmic
principles, managing trade-offs) — Met.** This is the milestone's category and
the outcome that M2 left only partially addressed. Each algorithm ships with an
ADR that names the alternatives and justifies the choice against explicit
trade-offs: longest-run versus rolling-window for streaks (ADR-0022),
inline-on-write versus batch recomputation for milestone detection (ADR-0019),
index column order and coverage for the composite-index strategy (ADR-0021),
and staleness-bound versus compute-cost for the TTL cache (ADR-0023). The
managing-trade-offs language in the outcome is satisfied not by asserting the
algorithms are correct but by recording, for each, what was given up and why.

**Outcome 4 (well-founded, innovative techniques and tools) — Met.** The trend
chart uses Recharts; the read paths use composite indexes proven with `EXPLAIN`
tests; the cache uses a monotonic-clock TTL structure rather than wall-clock
arithmetic. Each is a current, defensible tool choice rather than a hand-rolled
substitute.

**Outcome 5 (security mindset) — Met (reinforced).** Every M3 slice passed an
independent security review before merge. The reviews verified per-user cache-key
isolation, cleared the goal-history and achievement listings of object-level
authorization (IDOR) gaps, and confirmed the new query filters are parameterized.
The one concurrency defect found was fixed with a test that reproduces it.

**Outcome 2 (professional-quality communication) — Met.** Five new ADRs
(0019–0023) and three new DDRs (0006–0008) document the milestone's decisions;
the SRS, README set, OpenAPI snapshot, and this narrative communicate them to
instructors, maintainers, and portfolio readers.

**Outcome 1 (collaborative environments) — Partial.** As in M2, the capstone is
solo, but the decision records, the reviewable per-slice pull requests, the
conventional-commit history, and the deferred-work issues filed during closeout
all demonstrate work produced in a form that supports collaboration.

I have no updates to the Module One outcome-coverage plan. The milestone schedule
(M2 = software engineering, M3 = algorithms and data structures, M4 = databases)
continues to map cleanly to the program outcomes, and the M3 work is exactly the
algorithm and data-structure backlog the M2 narrative named.

---

## 4. Reflect on the process of enhancing and modifying the artifact

**What I learned.**

The most useful lesson was that the cost of an algorithm lives in its
*invariants*, not its happy path. The TTL cache read clean in isolation, but
because the sync request handlers run in a threadpool, two requests reading the
same just-expired key could both try to delete it and the loser would raise. The
fix was one line — evict with a pop that tolerates a missing key — but the lesson
was that any structure with lazy eviction and shared state needs to be reasoned
about under concurrency, not just under a single caller. I now treat "what
happens if two callers hit this at the same instant?" as a default question for
any cache or counter.

A second lesson was about where a data-structure decision actually belongs. The
weekly rate-of-change could have been computed in Python by pulling the user's
series and walking it; instead it became a composite-index decision at the
database layer (ADR-0021), so the work the index does never reaches the
application. Writing that as an ADR forced me to articulate the index column
order against the specific read paths, which is a more honest demonstration of
data-structure thinking than the equivalent in-memory loop would have been.

**Challenges I faced.**

The streak-detection design was the most instructive. The naive version
recomputed a rolling window on every read; the ADR-0022 work replaced it with a
single-pass longest-run scan over a set-backed sorted date sequence, which is
both cheaper and a clearer expression of what a streak actually is. Choosing the
data structure first — a set for membership, a sorted sequence for the scan —
made the algorithm fall out almost for free, which reinforced that the structure
choice is the real design decision.

The closeout itself surfaced a different kind of challenge: documentation drift.
The OpenAPI snapshot had fallen four phases behind the live routes, the DDR index
was missing its two newest records, and the SRS Appendix A still listed the M3
ADRs under their planned (and since-reassigned) numbers. None of these were code
defects, but each would mislead a future reader. The closeout pass exists to
catch exactly that class of rot, and running it as a deliberate step — regenerate
the snapshot from the live app, reconcile the indexes against what is on disk —
turned "documentation is probably fine" into "documentation is verified."

**How this work prepares me for the rest of the capstone.**

M3 leaves the same kind of clean seams for M4 that M2 left for M3. The composite
indexes and the `EXPLAIN`-backed proof of their use are the natural starting
point for the M4 indexing and constraint work. The TTL cache's documented
invalidation policy — and the two deferred items filed as issues during closeout
(weight update/delete invalidation and the cross-worker cache question) — give
M4 a concrete, tracked backlog rather than an implicit one. And the per-slice
review discipline that caught the cache concurrency defect is the habit I most
want to carry into the database work, where the failure modes are quieter and the
cost of finding them late is higher.

---

## Appendix A: Verifiable references

- Repository: <https://github.com/rgoshen-snhu/WeighToGo>
- Milestone Three tag: `v0.2.0`
- Prior milestone tag: `v0.1.0` · Android baseline: `v1.0.0-android`
- Software Requirements Specification: [`/docs/specs/WeighToGo_Web_SRS_v2.md`](../specs/WeighToGo_Web_SRS_v2.md)
- ADR index (M3 records 0019–0023): [`/docs/adr/README.md`](../adr/README.md)
- DDR index (M3 records 0006–0008): [`/docs/ddr/README.md`](../ddr/README.md)
- Milestone-detection algorithm: [`/docs/adr/0019-milestone-detection-algorithm.md`](../adr/0019-milestone-detection-algorithm.md)
- Preferences storage data structure: [`/docs/adr/0020-preferences-storage-data-structure.md`](../adr/0020-preferences-storage-data-structure.md)
- Composite-index strategy: [`/docs/adr/0021-composite-index-strategy.md`](../adr/0021-composite-index-strategy.md)
- Streak-detection algorithm: [`/docs/adr/0022-streak-detection-algorithm.md`](../adr/0022-streak-detection-algorithm.md)
- TTL caching strategy: [`/docs/adr/0023-ttl-caching-strategy.md`](../adr/0023-ttl-caching-strategy.md)
- Opaque compound cursor (M2 data-structure exemplar): [`/docs/adr/0015-opaque-compound-cursor-pagination.md`](../adr/0015-opaque-compound-cursor-pagination.md)
- OpenAPI snapshot: [`/docs/api/openapi.json`](../api/openapi.json)
- Engineering log: [`/SUMMARY.md`](../../SUMMARY.md)
- Milestone Three implementation brief: [`/docs/plans/milestone-three-plan.md`](../plans/milestone-three-plan.md)
- CS 499 code review checklist: [`/docs/standards/cs499_code_review_checklist.md`](../standards/cs499_code_review_checklist.md)
