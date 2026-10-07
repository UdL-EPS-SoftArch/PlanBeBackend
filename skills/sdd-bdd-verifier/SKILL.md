---
name: sdd-bdd-verifier
description: Execute Cucumber BDD verification tests, extract failing step errors, and format token-efficient diagnostic feedback.
---

# SDD BDD Verifier Skill

This skill provides AI agents with test execution and failure diagnostic instructions.

## Verification Command
```bash
mvn test -Dtest=CucumberTest
```

## Failure Diagnosis Routine
When a test fails, do NOT dump full Maven logs into context. Instead:
1. Check `target/surefire-reports/cat.udl.eps.softarch.demo.CucumberTest.txt`.
2. Extract:
   - Failing Scenario Name
   - Exact step that failed
   - Expected vs Actual status or assertion error
   - Exception line number in StepDefs

## Fix Protocol
Apply minimal diff changes to fix the reported failure.
