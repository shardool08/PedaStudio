# Assessment Tool Design — PedaStudio

This document defines how Baseline, Unit Test, and Endline assessments are built, administered, and scored for municipal school teachers (Grades 1–5 English).

## 1. What exists today vs what we built

| Layer | Status |
|-------|--------|
| Assessment **catalog** (baseline / unit / endline list) | In app (`lib/assessment-service.ts`) |
| **Aggregate score** entry (single % slider) | In app — placeholder until item tools ship |
| **Structured assessment tools** (items + marking + tallies) | New: `lib/assessment-tools/` |
| Grade 1 L2 full pack (baseline + 16 unit tests + endline) | Complete in `lib/assessment-tools/content/grade1-l2/` |

Grades 2–5 and L1 tracks follow the same schema; content is rolled out grade-by-grade using Grade 1 as the template.

---

## 2. Validity & reliability design

### Content validity
- Every item maps to **Balbharati lesson IDs** and **competency strings** from curriculum data.
- Items are grouped by **FLN strand** (Oral Language, Phonics, Decoding, Reading, Writing, Vocabulary).
- Unit tests align to existing **assessment groups** (A1–D3 for Grade 1 L2).
- Baseline tests **entry readiness**, not taught Unit 1 content (except familiar oral vocabulary).
- Endline tests **year-end competencies** across all four units.

### Construct validity
- Each item is tagged with **Bloom's level** (Remember / Understand / Apply) matching lesson progression.
- MCQs test one construct per item; distractors are plausible but clearly wrong for teachers who taught the unit.

### Reliability
- **Assessor copy** includes unambiguous marking scheme and correct answer for every item.
- **Subjective / performance items** use 0–1–2 rubrics (not open-ended grading).
- **Distractor notes** on tricky items reduce scorer disagreement.
- **Class aggregate entry** (not per-student) reduces clerical error for 40+ student classes.

### Future psychometric pass (pilot schools)
- Item **p-value** (% correct) is computed automatically — flag items with p < 0.2 or p > 0.95.
- Weak items (`p < 0.4`) appear in **reteach report**.
- After one term of data, revise or replace misfitting items.

---

## 3. Recommended teacher workflow (most efficient)

For **Grade 1–2**, this oral-group workflow is fastest and most accurate:

```
┌─────────────────────────────────────────────────────────────────┐
│  BEFORE CLASS                                                    │
│  1. Download Student Copy + Assessor Copy from app (PDF)       │
│  2. Prepare A/B/C/D cards (4 folded paper squares per child)     │
│  3. Note total students present (N)                              │
└─────────────────────────────────────────────────────────────────┘
                              ↓
┌─────────────────────────────────────────────────────────────────┐
│  DURING ASSESSMENT (~20–25 min for unit test)                    │
│  • Teacher reads question twice                                  │
│  • MCQ: children hold up answer card; teacher scans and tallies  │
│    on Assessor Copy data sheet (tick column A/B/C/D)             │
│  • Performance/oral: count hands or mark ✓ for each child        │
│    (quick row scan — no individual papers)                       │
└─────────────────────────────────────────────────────────────────┘
                              ↓
┌─────────────────────────────────────────────────────────────────┐
│  DATA ENTRY IN APP (~5 min)                                      │
│  • Open Assessment → Unit Test A1 → Enter Results                │
│  • Enter N once; for each item enter counts (A,B,C,D or ✓)       │
│  • App computes score %, FLN strand report, weak LOs              │
└─────────────────────────────────────────────────────────────────┘
```

### Why class-aggregate entry (not per-student)?

| Approach | Pros | Cons |
|----------|------|------|
| **Per-student bubble sheet** | Individual pupil tracking | Slow for G1 oral; high paper cost; scanning infra needed |
| **Class aggregate tallies** ✓ | Fast; matches oral delivery; 5-min app entry | No child-level longitudinal record |
| **Single overall % (current)** | Very fast | No item diagnosis; weak validity |

**Recommendation:** Use **class aggregate tallies** as default. Add optional per-student mode only for Prime tier / remedial groups (≤10 children).

### Grade 3+ written tests
- Same tally method if oral; for written MCQ, collect sheets and tally once from stack (still aggregate).
- Prime tier **scan + AI mark** can replace manual tally for written papers later.

---

## 4. Document structure (download pack)

Each assessment generates two PDFs:

### Student Copy
- Title, class, date line
- Questions only (no answers)
- MCQ options A–D
- Subjective: blank line or “show your answer to teacher”

### Assessor Copy
- Everything in Student Copy
- **Stimulus notes** (which picture to show)
- **Marking scheme** + correct answer
- **Data entry table** at end:

| Item | A | B | C | D | ✓ | NA |
|------|---|---|---|---|---|---|
| A1-01 |   |   |   |   |   |   |

- MCQ: fill A–D counts  
- Subjective/performance: fill ✓ (correct count) only  
- NA = not assessed (subtract from denominator)

---

## 5. Scoring & reports

Implementation: `lib/assessment-tools/scoring.ts`

- **Item score** = (correct count / effective N) × item marks  
- **Class score %** = total marks obtained / total marks possible × 100  
- **FLN strand report** = sum of item marks grouped by strand  
- **Weak items** = p-value < 40% → linked competencies for reteach  
- **Baseline vs Endline** = compare strand percents for growth chart  

Firestore schema extension (when integrating):

```typescript
// users/{uid}/assessmentResults/{toolId}
{
  toolId: "g1-l2-unit-A1",
  type: "unit",
  grade: 1,
  studentsAssessed: 42,
  tallies: [{ itemId, countA, countB, ... }],
  scorePercent: 68.5,
  strandScores: { OL: 72, VOC: 65, ... },
  weakItems: ["A1-06"],
  createdAt, updatedAt
}
```

---

## 6. File layout

```
lib/assessment-tools/
  types.ts              — schema
  fln-goals.ts          — grade-band FLN expectations
  scoring.ts            — report from tallies
  render.ts             — markdown/PDF source
  index.ts              — getAssessmentTool(), listAssessmentTools()
  content/
    grade1-l2/
      baseline.ts       — 12 items, 16 marks
      endline.ts        — 20 items, 29 marks
      units.ts          — 16 unit tests × 8 items each
    grade2-l2/          — (next)
    grade1-l1/          — (next)
```

---

## 7. Rollout plan

1. **Phase A (done):** Grade 1 L2 baseline + all unit tests + endline  
2. **Phase B:** Grade 1 L1, Grade 2 L2/L1 — clone item patterns from curriculum  
3. **Phase C:** App integration — download PDF, item-level entry UI, replace slider  
4. **Phase D:** Pilot validation in 5 PCMC/NMC schools; revise items with p-value data  

---

## 8. Item writing rules (for authors)

1. **Stem ≤ 15 words** for Grade 1 oral delivery  
2. **One correct answer** per MCQ; three plausible distractors from same topic  
3. **At least 2 items per unit test** must be Apply level (not all Remember)  
4. **Mix formats:** ~60% MCQ, ~40% subjective/performance in Grade 1  
5. **No negative framing** (“Which is NOT…”) in Grade 1–2  
6. **Cultural neutrality:** festivals include Diwali, Eid, Christmas as in textbook  
7. **Align to pages taught** — reference lesson vocabulary only  

---

## 9. Quick reference — FLN strands

| Code | Strand | Grade 1 examples |
|------|--------|------------------|
| OL | Oral Language | Follow instructions, greetings, dialogue |
| PA | Phonological Awareness | Rhyme, word families |
| DEC | Decoding | Letter sounds, CVC |
| RF | Reading Fluency | Sight words, name reading |
| RC | Reading Comprehension | Story who/why/sequence |
| WR | Writing | Copy letters, CVC dictation |
| VOC | Vocabulary | Body parts, objects, colours |
