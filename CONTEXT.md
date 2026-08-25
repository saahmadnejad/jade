# Jade

A fork of the JADE multi-agent framework (Java 21, virtual threads) with a Vert.x REST API and React UI. Beyond the classic FIPA platform, Jade hosts configurable **Scenarios**: pre-built multi-agent teams whose lifecycle is managed through the REST API and observed live in the frontend.

## Language

### Scenarios

**Scenario**:
A named template describing a set of agents and their configurable parameters; discoverable via ServiceLoader and startable through the scenarios API.
_Avoid_: demo, example app

**Instance**:
One running copy of a Scenario, isolated in its own agent container and killable atomically.
_Avoid_: run, session

### Development Team scenario

**Role Agent**:
An agent specialised by a role (Manager, Architect, Implementer, Tester, Reviewer) whose reasoning is delegated to an LLM through the LLM module.
_Avoid_: worker, bot, employee

**Workspace**:
The shared virtual file tree holding all Artifacts a team produces; source of truth for an Instance's work product.
_Avoid_: repo, storage, blackboard

**Artifact**:
A single file (spec, source file, test, review report) in the Workspace, produced or revised by a Role Agent.
_Avoid_: deliverable, output, file-result

**Brief**:
The initial goal text given to the Manager describing what the team must build.
_Avoid_: task, requirement doc, README

**Round**:
One pass of the team workflow: assignment → implementation → testing → review, bounded by caps.
_Avoid_: iteration, sprint, cycle

**Verdict**:
The Reviewer's terminal judgment on a Round: `approved` or `changes-requested`.
_Avoid_: feedback, result, opinion
