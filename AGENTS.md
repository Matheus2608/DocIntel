# Implementation Sequence (MANDATORY)

**CRITICAL**: This sequence MUST be followed for ALL implementation tasks. If you cannot execute these steps, DO NOT START.

## Step 1: EVALUATE

For your task, determine which tools/commands/agents/skills are needed:

### MCP Tools (query only, no Skill call needed)
- `mcp__tessl__query_library_docs` - For researching libraries/patterns

### Commands (invoke directly with / prefix)
- `/specify` - For capturing feature requirements and acceptance criteria
- `/plan` - For technical design and architecture

### Subagents (invoke via @mention or Task tool)
- `tdd-test-writer` - For RED phase (write failing tests)
- `tdd-implementer` - For GREEN phase (implement to pass tests)
- `tdd-refactorer` - For REFACTOR phase (improve code quality)

### Skills (invoke via skill tool)
- `tdd-integration` - For enforcing TDD Red-Green-Refactor cycle

## Step 2: ACTIVATE

For **MCP Tools**: Call directly via MCP server (e.g., `tessl_query_library_docs(...)`)

For **Commands**: Use directly (e.g., `/specify`, `/plan`)

For **Subagents**: Use @mention in message (e.g., `@tdd-test-writer write tests for...`) or Task tool

For **Skills**: Use skill tool (e.g., `skill({ name: "tdd-integration" })`)

Example:
```
mcp__tessl__query_library_docs("React Query patterns with TypeScript")
mcp__tessl__query_library_docs("Quarkus REST resources with Hibernate Panache")
/specify
/plan
Task(subagent_type="tdd-test-writer", description="Write failing tests for ChatResource")
Task(subagent_type="tdd-implementer", description="Implement ChatResource endpoint")
Task(subagent_type="tdd-refactorer", description="Optimize ChatResource code")
Skill(skill="webapp-testing", args="Test chat interactions")
```

## Step 3: IMPLEMENT

Only after Step 2 is complete, proceed with task completion.

---

# Workflow Examples

## Backend Feature Implementation

1. Query `mcp__tessl__query_library_docs`: Quarkus patterns, REST resources, Hibernate Panache queries
2. Run `/specify` (if needed): Define API endpoints and business logic
3. Run `/plan` (if needed): Design service and resource structure
4. Task(subagent_type="tdd-test-writer"): Write failing integration tests with JUnit 5
5. Task(subagent_type="tdd-implementer"): Implement services/resources to pass tests
6. Task(subagent_type="tdd-refactorer"): Optimize code while keeping tests passing
7. Commit with concise message

## Frontend Feature Implementation

1. Query `mcp__tessl__query_library_docs`: React 19, TypeScript, TanStack Query patterns
2. Skill(skill="frontend-design"): Create UI mockups and component structure
3. Task(subagent_type="tdd-test-writer"): Write failing component tests
4. Task(subagent_type="tdd-implementer"): Implement React components to pass tests
5. Task(subagent_type="tdd-refactorer"): Improve code quality
6. Skill(skill="webapp-testing"): Validate interactions and responsiveness
7. Commit with concise message

## Full Stack Feature Implementation

1. Query `tessl_query_library_docs`: Research all technologies involved
2. Run `/specify`: Define complete feature requirements
3. Run `/plan`: Design backend and frontend architecture
4. @tdd-test-writer: Write failing tests (backend + frontend)
5. @tdd-implementer: Implement backend and frontend
6. @tdd-refactorer: Optimize both sides
7. Commit with concise message

---

# TDD Pattern (Non-Negotiable)

### RED Phase: Write Failing Tests
- Tests define acceptance criteria
- Should fail before implementation
- Use descriptive test names
- Cover happy path + error cases

### GREEN Phase: Implement to Pass Tests
- Write minimal code to pass tests
- No over-engineering
- Follow existing code patterns
- Use subagent, not manual coding

### REFACTOR Phase: Improve Quality
- Keep all tests passing
- Simplify code logic
- Extract helper functions
- Improve readability
- Use subagent for quality checks

---

# Tool/Command/Subagent/Skill Reference

## MCP Tools (via tessl MCP server)
| Tool | Purpose | When to Use |
|------|---------|------------|
| `tessl_query_library_docs` | Research library patterns and abstractions | Before implementing external libraries |

## Commands (invoke directly with / prefix)
| Command | Purpose | When to Use |
|---------|---------|------------|
| `/specify` | Capture and define feature requirements and AC | At start of feature if not already defined |
| `/plan` | Generate technical design and architecture | Before implementation starts |

## Subagents (invoke via @mention or Task tool)
| Subagent | Purpose | When to Use |
|----------|---------|------------|
| `tdd-test-writer` | RED phase - write failing tests | Before GREEN implementation |
| `tdd-implementer` | GREEN phase - implement to pass tests | After tests are written and failing |
| `tdd-refactorer` | REFACTOR phase - improve code quality | After tests pass and are green |

## Skills (invoke via skill tool)
| Skill | Purpose | When to Use |
|-------|---------|------------|
| `tdd-integration` | Enforce Red-Green-Refactor TDD cycle | For new feature implementation with strict TDD |

---

# Key Rules

1. **NO manual test writing**: Use `@tdd-test-writer` subagent
2. **NO manual implementation**: Use `@tdd-implementer` subagent
3. **NO skipping refactoring**: Use `@tdd-refactorer` subagent
4. **ALWAYS query tessl**: Call `tessl_query_library_docs` before using external libraries
5. **CONCISE commits**: Keep messages brief and focused
6. **TDD always**: RED → GREEN → REFACTOR, never skip phases
7. **Correct invocation methods**:
   - MCP Tools: Call directly as functions
   - Commands: Use / prefix (e.g., `/specify`, `/plan`)
   - Subagents: Use @mention (e.g., `@tdd-test-writer`) or Task tool
   - Skills: Use skill tool with skill parameter

---

# Conversation Management

After completing a phase:
1. Export conversation: `/export`
2. Create folder: `mkdir -p /conversation/<topic>/`
3. Move file: `mv <exported-file> /conversation/<topic>/`
4. Document: What was implemented, tests status, issues resolved

Example folders:
- `/conversation/backend-phase-5/`
- `/conversation/frontend-phase-4/`
- `/conversation/feature-review-page/`

# Agent Rules <!-- tessl-managed -->

@.tessl/RULES.md follow the [instructions](.tessl/RULES.md)
