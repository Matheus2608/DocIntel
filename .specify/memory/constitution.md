<!--
SYNC IMPACT REPORT
Version: 1.0.0 (initial creation)
Ratified: 2025-01-27
Principles: 7 core principles established
Sections: 5 (Core Principles, Tech Stack, Development Workflow, Quality Gates, Governance)
Status: All template tokens replaced, no deferred items
-->

# DocIntel Constitution

## Core Principles

### I. Spec-Driven Development (SDD)

Every feature MUST be specified before implementation. Specs capture requirements, acceptance criteria, and technical constraints in human-readable format. Specs are the source of truth for both developers and AI agents.

- Specifications MUST precede code (use `/specify` command)
- Acceptance criteria MUST be testable and unambiguous
- Specs MUST be documented in version control
- Rationale: Clear specifications prevent misalignment, reduce rework, and enable AI agents to implement with fidelity

### II. Test-Driven Development (Mandatory)

All feature implementation MUST follow strict Test-Driven Development with Red-Green-Refactor phases. No code is written before failing tests exist.

- **RED phase**: Write failing integration tests (use `tdd-test-writer` subagent)
- **GREEN phase**: Implement minimal code to pass tests (use `tdd-implementer` subagent)
- **REFACTOR phase**: Improve code quality while keeping tests passing (use `tdd-refactorer` subagent)
- Tests MUST fail before implementation starts
- Tests MUST pass before refactoring begins
- Rationale: TDD ensures correctness, prevents over-engineering, and creates comprehensive test coverage

### III. First Principles & Reading Code

All proposals MUST be grounded in understanding existing code before suggesting solutions. No assumptions about implementation details without reading source.

- Always read relevant source files before proposing changes
- Understand existing patterns and architectural decisions
- Respect the codebase's conventions and styles
- Rationale: Prevents misunderstanding of complex interactions, maintains consistency, catches unintended side effects

### IV. Context Engineering with Tessl

External libraries MUST be integrated using Tessl Registry specs. Never hallucinate or guess library APIs. Specification patterns from Tessl provide accurate, up-to-date documentation.

- Query `mcp__tessl__query_library_docs` before using external libraries
- Install official specs for all major dependencies (Quarkus, React, PostgreSQL, etc.)
- Reference installed specs when implementing features
- Rationale: Prevents API misuse, version conflicts, and outdated documentation errors

### V. Simplicity & Atomicity

Code changes MUST be minimal, focused, and avoid over-engineering. Solve the specific problem at hand without building for hypothetical future requirements.

- Write only what is needed to pass tests
- No "clever" refactors or architectural improvements beyond scope
- No premature abstractions or helper utilities for one-time operations
- No backwards-compatibility hacks or temporary workarounds
- Rationale: Keeps codebase maintainable, reduces cognitive load, enables faster iteration

### VI. Root Cause Fixes Only

All bug fixes MUST address the underlying cause, not symptoms. Temporary hacks are prohibited.

- Identify why the bug exists, not just how to make it disappear
- Fix the root cause, even if it requires more changes
- Document the analysis in commit messages
- Rationale: Prevents recurring bugs, maintains code quality, reduces technical debt

### VII. Progressive Disclosure

Every significant step MUST include explanation of what was done and why. Status updates should clarify decisions and trade-offs.

- Explain code changes and architectural decisions after each step
- Document assumptions and constraints in specifications
- Communicate blockers and decisions to the user
- Rationale: Maintains alignment, enables informed decision-making, creates audit trail

## Technology Stack

### Backend
- **Runtime**: Java 21+
- **Framework**: Quarkus 3.30+
- **Database**: PostgreSQL 14+ with pgvector extension
- **ORM**: Hibernate with Panache (Quarkus default)
- **AI Integration**: OpenAI API (GPT-4o, GPT-4o-mini)
- **Build Tool**: Maven 3.9+

### Frontend
- **Framework**: React 18+
- **Language**: TypeScript
- **Build Tool**: Vite
- **HTTP Client**: TanStack Query (React Query)
- **Styling**: Custom CSS + CSS Variables (no Tailwind, no CSS-in-JS)
- **Package Manager**: npm

### Testing
- **Backend Unit Tests**: JUnit 5
- **Integration Tests**: Testcontainers (PostgreSQL, pgvector)
- **Frontend Unit Tests**: Jest
- **Frontend Component Tests**: React Testing Library
- **API Mocking**: MSW (Mock Service Worker)

### Developer Tools
- **Spec Management**: Tessl 0.57.3+ (SDD registry)
- **Spec Execution**: Claude Code with speckit commands
- **Version Control**: Git

## Development Workflow

### Implementation Sequence (Mandatory)

Every feature MUST follow this exact sequence in order:

1. **Research**: Query `mcp__tessl__query_library_docs` for relevant patterns and abstractions
2. **Specify**: Run `/specify` to create feature requirements and acceptance criteria
3. **Plan**: Run `/plan` to design technical architecture and contracts
4. **TDD Cycle**: Execute full Red-Green-Refactor cycle using subagents (never manual coding for features)
5. **Validation**: Use skills for testing interactions (e.g., `webapp-testing` for frontend)
6. **Commit**: Create atomic, focused commits with clear messages

### Commit Message Requirements

- Use concise, action-oriented messages (5-10 words typical)
- Follow format: `<type>: <description>`
- Types: `feat:` (new feature), `fix:` (bug fix), `test:` (test additions), `docs:` (documentation), `refactor:` (code quality)
- Include reasoning in message body if non-obvious
- Example: `feat: add semantic reranking for RAG retrieval`

### Code Review Gates

- All code MUST pass tests before merge
- All code MUST follow TDD cycle (tests exist and pass)
- All code MUST maintain existing test coverage
- Specifications MUST be updated if requirements change

## Quality Gates

### Testing Requirements

- **Backend**: Minimum 70% code coverage for new modules
- **Frontend**: Component-level tests for all UI changes
- **Integration**: Full end-to-end testing for cross-layer features
- **Performance**: RAG retrieval latency < 2 seconds, UI interactions respond < 500ms

### Code Quality Standards

- No console.logs in production code
- No commented-out code
- No TODO comments without associated issue
- No unused imports or variables
- No magic numbers (use named constants)

### Documentation Standards

- Specs MUST be complete before implementation
- API contracts MUST be documented in plan artifacts
- Complex algorithms MUST include rationale comments
- Database schema changes MUST be versioned

## Governance

### Constitutional Authority

This constitution is the source of truth for DocIntel development practices. It supersedes all other informal guidelines or conventions.

### Amendment Procedure

1. Propose change with clear rationale (what, why, impact)
2. Document in branch/PR with "constitution:" prefix
3. Explain impact on existing code and practices
4. Merge to main only after review and approval
5. Update this document with new version and amendment date

### Version Bumping Rules

- **MAJOR** (X.0.0): Principle removal, redefinition, or incompatible governance change
- **MINOR** (0.X.0): New principle, new section, or materially expanded guidance
- **PATCH** (0.0.X): Clarifications, wording improvements, non-semantic refinements

### Compliance Review

- Constitution MUST be reviewed quarterly or when major changes occur
- Spec artifacts MUST reference applicable principles
- CI/CD gates SHOULD enforce testing and coverage requirements

**Version**: 1.0.0 | **Ratified**: 2025-01-27 | **Last Amended**: 2025-01-27
