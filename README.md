# charMeD

**charMeD** is a terminal-based markdown editor and viewer built as a **Java OOP showcase project**. It features a two-process architecture with a **Go frontend** (using [Charm](https://charm.sh)'s TUI libraries) and a **Java backend** (handling all business logic).

## Features

- **Split-Pane Interface**: Raw markdown editing on the left, live [Glamour](https://github.com/charmbracelet/glamour)-rendered preview on the right.
- **Vim-like Editing**: Modal editing support (Normal, Insert, Command modes).
- **Architecture**: Clean separation of concerns with JSON-RPC over stdin/stdout.
- **Java OOP Showcase**: Demonstrates SOLID principles, 12+ design patterns, and Java 21+ features (sealed classes, records, pattern matching).

## Prerequisites

- Java 21+
- Go 1.22+
- Make
- Gradle 8.x (wrapper provided)

## Getting Started

1. **Clone the repository**:
   ```bash
   git clone https://github.com/shivangjhalani/charMeD.git
   cd charMeD
   ```

2. Ensure you have Java 21+ and Go 1.22+ installed.

3. **Run the application**:
   ```bash
   make run
   ```

## Build & Test

- **Build Everything**:
  ```bash
  make build
  ```
  This builds the Java backend (shadowJar) and the Go frontend binary.

- **Run Backend Tests**:
  ```bash
  make test
  ```

- **Clean Build Artifacts**:
  ```bash
  make clean
  ```

## Documentation

Detailed documentation can be found in the `docs/` directory:

- [**Architecture**](docs/ARCHITECTURE.md): High-level overview of the two-process system.
- [**Backend Design**](docs/BACKEND.md): Detailed Java backend design and OOP patterns.
- [**Frontend Design**](docs/FRONTEND.md): Go frontend implementation details.
- [**Protocol**](docs/PROTOCOL.md): JSON-RPC specification.
- [**OOP Patterns**](docs/OOP_PATTERNS.md): List of design patterns and Java features used.

## Project Structure

```
charMeD/
├── backend/        # Java backend (Business Logic, OOP Showcase)
├── frontend/       # Go frontend (TUI, Interaction)
├── docs/           # Documentation
└── Makefile        # Build scripts
```
