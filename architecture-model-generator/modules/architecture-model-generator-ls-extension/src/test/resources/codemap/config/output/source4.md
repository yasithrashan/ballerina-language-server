# Project CodeMap

## CodeMap Structure

This document provides a structured overview of the project codebase.
It is organized by file path and summarizes the following elements for each file.
Each artifact is listed with its sub-properties on separate indented lines.


---

## File Path : main.bal

### Code Issues
- [Parser Error] missing returns keyword [L:4 - L:4]
  ```
  io:println("Hello, World!");
  ```
- [Parser Error] missing open brace token [L:4 - L:4]
  ```
  io:println("Hello, World!");
  ```
- [Parser Error] invalid expression statement [L:4 - L:4]
  ```
  io:println("Hello, World!");
  ```

### Imports
- ballerina/io [L:1 - L:1]

### Functions
- function greetUser() [L:9 - L:11]
