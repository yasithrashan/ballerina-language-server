# Project CodeMap

## CodeMap Structure

This document provides a structured overview of the project codebase.
It is organized by file path and summarizes the following elements for each file.
Each artifact is listed with its sub-properties on separate indented lines.


---

## File Path : main.bal

### Imports


- ballerina/http
  - **Line Range**: (0:0-0:22)

### Variables


- users
  - **Type**: map<User>
  - **Line Range**: (22:0-22:21)

- nextId
  - **Type**: int
  - **Line Range**: (23:0-23:15)

### Types


- type User
  - **Type Descriptor**: record
  - **Fields**: [id: int, name: string, email: string]
  - **Line Range**: (6:0-10:3)

- type CreateUserRequest
  - **Type Descriptor**: record
  - **Fields**: [name: string, email: string]
  - **Line Range**: (12:0-15:3)

- type ErrorResponse
  - **Type Descriptor**: record
  - **Fields**: [message: string]
  - **Line Range**: (17:0-19:3)

### Listeners


- listener httpListener
  - **Type**: http:Listener
  - **Line Range**: (3:0-3:55)

### Services (Entry Points)


- service /api
  - **Base Path**: /api
  - **Listener Type**: http:Listener
  - **Line Range**: (26:0-70:1)

  - get resource function users
    - **Parameters**: none
    - **Returns**: [User[]|error]
    - **Line Range**: (29:4-31:5)

  - get resource function users/[int id]
    - **Parameters**: none
    - **Returns**: [User|http:NotFound|error]
    - **Line Range**: (34:4-44:5)

  - post resource function users
    - **Parameters**: [payload: CreateUserRequest]
    - **Returns**: [User|error]
    - **Line Range**: (47:4-56:5)

  - get resource function search
    - **Parameters**: [name: string]
    - **Returns**: [User[]|error]
    - **Line Range**: (59:4-64:5)

  - get resource function health
    - **Parameters**: none
    - **Returns**: [string]
    - **Line Range**: (67:4-69:5)
