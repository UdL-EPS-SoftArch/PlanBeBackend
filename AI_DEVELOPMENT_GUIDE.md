# AI-Assisted Spec-Driven Development (SDD) Guide

This guide explains how to expand this Spring Data REST + Cucumber backend template using AI agents and Spec-Driven Development (SDD).

---

## 1. Architecture & SDD Overview

Development is strictly guided by **Behavior-Driven Development (BDD)** scenarios:
1. **Spec Phase**: Write executable Cucumber `.feature` files.
2. **Red Phase**: Write failing step definitions (`*StepDefs.java`).
3. **Green Phase**: Implement domain entities, `@RepositoryEventHandler` classes, and `@RepositoryRestResource` interfaces.
4. **Verify Phase**: Execute `mvn test -Dtest=CucumberTest`.

The project provides **5 modular skill playbooks** in `skills/`:
- `skills/sdd-feature-designer/SKILL.md`
- `skills/sdd-cucumber-steps/SKILL.md`
- `skills/spring-datarest-entity/SKILL.md`
- `skills/spring-datarest-repository/SKILL.md`
- `skills/sdd-bdd-verifier/SKILL.md`

---

## 2. Using AI Agents with Large Context Windows
*(Tools: OpenCode, Claude Code, Cursor, GitHub Copilot,...)*

When using AI harnesses with high token context limits, agents can read project conventions and execute multi-step workflows autonomously.

### Workflow
1. **Prompt the Agent**:
   ```
   "Add a new domain entity called 'Comment' that is attached to a Record and owned by a User.
   Follow the SDD workflow in AGENTS.md and apply the skills in skills/."
   ```
2. **Agent Execution**:
   - Creates `src/test/resources/features/ManageComment.feature` using `sdd-feature-designer`.
   - Creates `ManageCommentStepDefs.java` using `sdd-cucumber-steps`.
   - Creates `Comment.java` and `CommentRepository.java` using `spring-datarest-entity`.
   - Creates `CommentEventHandler.java` using `spring-datarest-handler`.
   - Runs `mvn test -Dtest=CucumberTest` using `sdd-bdd-verifier`.

---

## 3. Using Aider in VS Code with Restricted APIs & Small Models
*(Tools: Aider + Groq Free Tier 8K Token limit)*

When working under strict token limits (such as Groq's 8K tokens per message limit), **never load all skills or the entire codebase into Aider at once**. Doing so will immediately exceed context limits and result in truncated output.

---

### 3.1 Installation, Configuration & Launching Aider in VS Code

#### Installation
Install or update Aider in your Python environment following instructions at https://aider.chat/docs/install.html

#### Understanding `.aider.conf.yml` Options for Token Budgeting
The root `.aider.conf.yml` controls Aider's runtime options and is essential for keeping the context small:

```yaml
# Models
model: groq/qwen/qwen3.6-27b
editor-model: groq/openai/gpt-oss-120b
weak-model: groq/llama-3.1-8b-instant

# Always load project conventions
read:
  - CONVENTIONS.md

# Settings optimized for low-token budget models via Groq
reasoning-effort: none
thinking-tokens: 0
edit-format: diff                               # forces diff-style edits, not whole-file rewrites
map-tokens: 512                                 # small repo map instead of the ~1024+ default
max-chat-history-tokens: 2000                   # aggressively trims conversation history each turn
cache-prompts: true

# Behavior
test-cmd: "mvn test -Dtest=CucumberTest"
auto-test: false
auto-commits: true
auto-lint: false
stream: true
dark-mode: true
```

**Why this configuration keeps context small**:
- `model`, `editor-model`, `weak-model`: Configures Groq models suitable for low-token budgets.
- `read: [CONVENTIONS.md]`: Loads only lightweight (<50 lines) rules. Specific skills from `skills/` are loaded on-demand during session stages.
- `edit-format: diff`: Forces diff-style edits rather than whole-file rewrites to save tokens.
- `map-tokens: 512`: Limits repository map token budget to avoid flooding context.
- `max-chat-history-tokens: 2000`: Aggressively trims conversation history each turn.
- `auto-test: false`: Prevents Aider from re-running tests after every minor chat turn.

#### Starting Aider in VS Code
1. Open VS Code in this repository root.
2. Open the integrated terminal.
3. Set your API key (e.g. for Groq) following the instructions at https://aider.chat/docs/llms/groq.html
4. Start Aider, no need to state the model, they are pre-configured in `.aider.conf.yml`, just type:
   ```bash
   aider --watch-files
   ```

---

### 3.2 Key In-Session Commands for Context Management

Once Aider is running inside the VS Code terminal, manage your files and context using these commands:

| In-Session Command | Purpose & Context Control |
| :--- | :--- |
| `/add <file>` | Adds a file to the active session for **editing** or **creating**. |
| `/read <file>` | Adds a file as **read-only context** (e.g. skills, reference code). |
| `/drop <file>` | Removes a file from session context when finished with a stage. |
| `/clear` | Clears conversation history and active files (essential between stages!). |
| `/tokens` | Displays current token usage (ensure total remains under **8,000 tokens**). |

---

### 3.3 Stage-by-Stage BDD Workflow: Feature "Manage Tag for Records"

Below is the step-by-step workflow for developing a **Tag** entity associated with **Records** inside an active Aider session in VS Code.

---

#### Stage 1: Design Feature Spec for Tag Entity
Inside your active Aider terminal session in VS Code:

1. **Ensure reasoning is deactivated at the beggining of each session to reduce output tokens budget **
   ```
   /reasoning-effort none
   ```

2. **Load skill and reference feature as read-only, add target feature file**:
   ```
   /read skills/sdd-feature-designer/SKILL.md
   /read src/test/resources/features/ManageRecord.feature
   /add src/test/resources/features/ManageTag.feature
   ```

3. **Check token budget**:
   ```
   /tokens
   ```

4. **Prompt Aider**:
   > Create the Cucumber BDD scenarios for managing Tags that can be associated with Records. Keep the file under 50 lines.

---

#### Stage 2: Implement Failing Step Definitions
Before starting Stage 2, drop files from Stage 1 to free up tokens:

1. **Reset context for Stage 2**:
   ```
   /drop skills/sdd-feature-designer/SKILL.md
   /drop src/test/resources/features/ManageRecord.feature
   ```

2. **Load step definition skill and context**:
   ```
   /read skills/sdd-cucumber-steps/SKILL.md
   /read src/test/resources/features/ManageTag.feature  # Needed just if it was previously dropped
   /read src/test/java/cat/udl/eps/softarch/demo/steps/StepDefs.java
   /add src/test/java/cat/udl/eps/softarch/demo/steps/ManageTagStepDefs.java
   ```

3. **Prompt Aider**:
   > Implement the Cucumber step definitions in ManageTagStepDefs.java for ManageTag.feature.

---

#### Stage 3: Implement Tag Domain Entity, Repository Repository with SpEL Security
Drop previous step definitions from session context:

1. **Reset context for Stage 3**:
   ```
   /drop skills/sdd-cucumber-steps/SKILL.md
   /drop src/test/resources/features/ManageTag.feature
   /drop src/test/java/cat/udl/eps/softarch/demo/steps/StepDefs.java
   /drop src/test/java/cat/udl/eps/softarch/demo/steps/ManageTagStepDefs.java
   ```

2. **Load entity skill and reference code**:
   ```
   /read skills/spring-datarest-entity/SKILL.md
   /add src/main/java/cat/udl/eps/softarch/demo/domain/Record.java
   /add src/main/java/cat/udl/eps/softarch/demo/repository/RecordRepository.java
   /add src/main/java/cat/udl/eps/softarch/demo/domain/Tag.java
   /add src/main/java/cat/udl/eps/softarch/demo/repository/TagRepository.java
   ```

3. **Prompt Aider**:
   > Implement entity Tag with a name field and a ManyToMany relationship to Record. Also implement TagRepository. Just add the relationship between tags and records to the Record side, the reverse should be a findByTags method in RecordRepository

---

#### Stage 4: Implement Tag Lifecycle Event Handler
Drop domain entity files from session context:

1. **Reset context for Stage 4**:
   ```
   /drop skills/spring-datarest-entity/SKILL.md
   /drop src/main/java/cat/udl/eps/softarch/demo/domain/Record.java
   /drop src/main/java/cat/udl/eps/softarch/demo/domain/RecordRepository.java
   ```

2. **Load repository skill and reference repository**:
   ```
   /read skills/spring-datarest-handler/SKILL.md
   /read src/main/java/cat/udl/eps/softarch/demo/domain/Tag.java
   /read src/main/java/cat/udl/eps/softarch/demo/domain/TagRepository.java
   /read src/main/java/cat/udl/eps/softarch/demo/repository/RecordEventHandler.java
   /add src/main/java/cat/udl/eps/softarch/demo/handler/TagEventHandler.java
   ```

3. **Prompt Aider**:
   > Implement the TagEventHandler keeping track of ownership.

---

#### Stage 5: BDD Test Verification & Failure Diagnosis
Execute the Cucumber tests:

1. **Run verification**:
   ```
   mvn -q test -Dtest=CucumberTest -Dcucumber.plugin=summary -Dsurefire.trimStackTrace=true 
   ```

The output is also available as an HTML report at `target/cucumber-report.html`. Though it is possible to run the tests directly inside Aider using the `/test` command, it is to big given the context size restrictions.

2. **If a test fails**, drop all files and load only the failure log and failing step class:
   ```
   /clear
   /read skills/sdd-bdd-verifier/SKILL.md
   /read target/surefire-reports/cat.udl.eps.softarch.demo.CucumberTest.txt
   /add src/test/java/cat/udl/eps/softarch/demo/steps/ManageTagStepDefs.java
   ```

3. **Prompt Aider**:
   > Fix the failing step in ManageTagStepDefs.java based on the surefire report summary.

If the failure log is too long, copy and paste in Aider just the relevant fragment and let it address one issue at a time.

---

## 4. Summary of Rules for Token Budgeting
- **Check tokens frequently**: Run `/tokens` before sending requests to verify you are below **8,000 tokens**.
- **Always `/drop` or `/clear` between stages**: Never leave previous stage code in active context when starting the next stage.
- **Use `/read` for reference files**: Reading files as read-only ensures Aider doesn't attempt to rewrite reference classes.
