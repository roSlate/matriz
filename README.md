# Matriz

Matriz is a web application where teams organise projects, manage tasks and follow the progress of their work in one shared place.

It is built as a course project for *AI-Powered Software Engineering*, using AI assistants throughout, with every AI output reviewed by the developer.

## The problem

Teams often keep track of work in scattered places: spreadsheets, chat messages, email. It is hard to see what needs doing, who is doing it and how far along it is. Matriz gives a team one place for that, with the basics of tools like Trello or Jira and none of their configuration complexity.

> The client's request: *"We need an application where our teams can organise projects, manage tasks and track the progress of the work."*

Everything beyond that sentence is an assumption, registered in [docs/constraints.md](docs/constraints.md).

## Goals

- Organise work into projects.
- Manage tasks inside a project, with status, priority, assignees and comments.
- Show progress at a glance.
- Keep data private to the people with access, and keep it safe across restarts.

## Planned MVP features

| Area | What it includes |
|---|---|
| Access | Log in, log out, see who you are. |
| Projects | Create, list, view, edit, delete; search by name. |
| Members | Add and remove members with a role per project (Member or Viewer; the project's creator is its Owner). |
| Tasks | Create, edit, delete; status (`TODO`, `IN_PROGRESS`, `DONE`) and priority (`LOW`, `MEDIUM`, `HIGH`); filter by both. |
| Assignees | Assign several people to a task. |
| Comments | Comment on tasks, with author and date. |
| Progress | Dashboard with task counts per status and a progress percentage per project, with an optional manual override by the project owner. |

What is *not* in the MVP (sprints, notifications, attachments, integrations, and more) is listed in [docs/requirements.md](docs/requirements.md).

## Documentation

| File | Contents |
|---|---|
| [docs/requirements.md](docs/requirements.md) | Vision, users and roles, functional and non-functional requirements, MVP scope, out of scope. |
| [docs/user-stories.md](docs/user-stories.md) | User stories with priorities, acceptance criteria and edge cases. |
| [docs/constraints.md](docs/constraints.md) | Constraints, assumptions, security and privacy requirements, pending technical decisions. |
| [docs/architecture.md](docs/architecture.md) | Architecture decision (modular monolith), technology choices, components, order of access checks. |
| [docs/database.md](docs/database.md) | Data model: entities, relationships, integrity rules and the ER diagram. |
| [openapi.yaml](openapi.yaml) | REST API contract of the MVP (OpenAPI 3.1). |
| [docs/ui/](docs/ui/) | Low-fidelity wireframes: dashboard, project detail, task detail. |
| [docs/review-log.md](docs/review-log.md) | Critical reviews of the specification and of the architecture pack: findings and the decision taken on each, plus the prompt comparison from Exercise 1. |
| [AGENTS.md](AGENTS.md) | Persistent instructions for AI coding agents working on this repository. |

## Technology

Java with Spring Boot (backend), React with TypeScript (frontend), PostgreSQL (database), structured as a modular monolith with a REST API described in OpenAPI. Decisions and reasons are in [docs/architecture.md](docs/architecture.md); open points are in [docs/constraints.md](docs/constraints.md), section 4.2.

## Current status

**Stage: architecture and design done; implementation not started.** The Specification Pack (requirements, user stories, constraints, agent instructions) and the Architecture Pack (architecture, data model, API contract, wireframes) are written and have each been through an independent review. There is no application code yet, so there is nothing to install or run. Installation, configuration and test instructions will be added when the implementation exists.

Next stages: MVP implementation, testing and review, containerisation and delivery.
