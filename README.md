# ClearDocs

A document approval platform built with Java and Spring Boot. The application allows users to create documents, submit them for review, and manage approvals through a role-based workflow.

## Features

* JWT-based authentication and authorization
* Role-based access control (User, Reviewer, Approver, Admin)
* Document submission and approval workflow
* Audit logging for document actions
* REST APIs with Swagger documentation
* PostgreSQL for relational data
* MongoDB for audit and metadata storage
* Redis caching
* Dockerized development environment

## Tech Stack

* Java 11
* Spring Boot
* Spring Security
* JWT
* PostgreSQL
* MongoDB
* Redis
* Docker & Docker Compose
* JUnit 5 & Mockito

## Workflow

```text
Draft → Submitted → Under Review → Pending Approval
                                      ↓
                             Approved / Rejected
```

## Getting Started

### Clone the repository

```bash
git clone https://github.com/gayatri517/ClearDocs.git
cd ClearDocs
```

### Run with Docker

```bash
docker compose up -d
```

### Access the application

```text
http://localhost:8080
```

### Swagger UI

```text
http://localhost:8080/swagger-ui/index.html
```

## Project Structure

```text
src/main/java
├── controller
├── service
├── repository
├── security
├── statemachine
└── config
```

## What I Built

This project was created to explore:

* Workflow-driven application design
* Spring Security and JWT authentication
* State machine based approval flows
* Audit logging using Spring AOP
* Multi-database integration with PostgreSQL and MongoDB
* Caching with Redis
* Containerized development using Docker

```
```
