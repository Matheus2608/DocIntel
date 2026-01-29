# Project Instructions

## Unnegotiables

- **First Principles**: Read codebase before proposing solutions
- **Simplicity & Atomicity**: Minimal code changes, avoid "clever" refactors
- **Root Cause Fixes**: No temporary hacks
- **Progressive Disclosure**: Explain changes and status after each step
- **Context Engineering**: Use tessl Registry + Local Tiles (never hallucinate library APIs)

---

## Implementation Workflow

**CRITICAL**: Do NOT implement features without following the MANDATORY IMPLEMENTATION SEQUENCE in AGENTS.md. If you cannot use the required tools/commands/subagents/skills, **DO NOT START** the task.

### For All Features

1. **Research**: Query `mcp__tessl__query_library_docs` (MCP tool) for patterns BEFORE coding
2. **Specification**: Run `/specify` (command) for requirements (if not already defined)
3. **Planning**: Run `/plan` (command) for technical approach (if not already defined)
4. **TDD Cycle** (MANDATORY - use subagents via Skill() tool, not manual coding):
   - **RED**: `/tdd-test-writer` subagent → write failing tests
   - **GREEN**: `/tdd-implementer` subagent → implement to pass tests
   - **REFACTOR**: `/tdd-refactorer` subagent → improve code quality
5. **Frontend Validation**: `/web-app-testing` skill → test interactions
6. **Commit**: Create concise commit messages

### Technology Stack

- **Backend**: Java 21+, Quarkus 3.30+, PostgreSQL with pgvector, Hibernate Panache
- **Frontend**: React 19, TypeScript, Vite, TanStack Query
- **Testing**: JUnit 5, React Testing Library, Testcontainers
- **Styling**: Custom CSS + Variables (no Tailwind/CSS-in-JS)

### Current Branch

- Branch: 002-embedding-search
- Status: Document embedding and retrieval system implementation

---

---

## Conversation Export Rules

### Folder Structure
```
conversation/
└── features/
    └── {feature-name}/          # Descriptive name (e.g., review-system, deck-management)
        └── phases/
            ├── phase-1/
            │   ├── conversation.txt
            │   └── summary.md
            ├── phase-2/
            │   ├── conversation.txt
            │   └── summary.md
```

### File Requirements
- **conversation.txt**: Full conversation transcript (exported from Claude)
- **summary.md**: 2-3 lines max, one-line description of work done

### Export Process
Use the provided export script:
```bash
./scripts/export-conversation.sh <feature-name> <phase-number>
```

---

## Key References

- `AGENTS.md` → Implementation sequence (MCP tools, commands, subagents, skills) and workflow examples
- `specs/` → Project specifications
- `.tessl/local-tiles/` → Project-specific context patterns
- `back/src/`, `front/src/` → Implementation code

@AGENTS.md

## Active Technologies
- Java 21+ (existing Quarkus 3.30.4 application) (001-docling-ingestion)
- PostgreSQL 14+ with pgvector extension (existing) (001-docling-ingestion)
- Java 21+ (Backend), TypeScript (Frontend) + Quarkus 3.30+, LangChain4j, React 19, Vite 7.2 (002-embedding-search)

## Recent Changes
- 001-docling-ingestion: Added Java 21+ (existing Quarkus 3.30.4 application)
