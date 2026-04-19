# charMeD Architecture

## Project Overview

**charMeD** is a terminal-based markdown editor and viewer built as a **Java OOP showcase project**. It uses a two-process architecture: a **Go frontend** powered by [Charm](https://charm.sh)'s beautiful TUI libraries for the terminal interface, and a **Java backend** containing all business logic, designed to demonstrate advanced object-oriented programming principles.

### Goals

1. **Java OOP Showcase** -- Demonstrate all 4 OOP pillars, SOLID principles, 12+ design patterns, and Java 21+ language features (sealed classes, records, pattern matching) in a naturally motivated, non-trivial application.
2. **Beautiful Terminal UI** -- Leverage Charm's ecosystem (Bubble Tea, Lip Gloss, Glamour, Bubbles) for a polished, modern TUI that rivals GUI editors in aesthetics.
3. **Full Editor + Viewer** -- Split-pane interface with raw markdown editing on the left, live Glamour-rendered preview on the right.

---

## Two-Process Architecture

```
┌─────────────────────────────────┐    stdin/stdout     ┌──────────────────────────────────┐
│        Go Frontend (TUI)        │ ◄─ JSON-RPC 2.0 ─► │       Java Backend (OOP)          │
│                                 │ Content-Length       │                                  │
│  Bubble Tea   (event loop)      │ framing (LSP-style) │  Markdown parsing (AST)          │
│  Glamour      (md preview)      │                     │  Document model (Composite)      │
│  Lip Gloss    (styling/layout)  │ Go spawns Java as   │  Editor state (State pattern)    │
│  Bubbles      (TUI components)  │ child process via   │  Visitors, Observers, Strategy   │
│                                 │ exec.Command         │                                  │
│  Owns: terminal, rendering,     │                     │  Owns: ALL business logic,       │
│  key capture, mouse events,     │ stderr → log file   │  document state, parsing,        │
│  layout composition             │ (debug only)        │  export, search                  │
└─────────────────────────────────┘                     └──────────────────────────────────┘
```

### Why Two Processes?

- **Charm libraries are Go-native.** There is no way to call Bubble Tea, Lip Gloss, or Glamour from Java. TUI4J exists as a partial Java port but lacks Glamour (markdown rendering) and Lip Gloss (styling), which are essential for the "beautiful" requirement.
- **Clean separation of concerns.** The Go process is a pure view layer -- it has zero business logic. The Java process is a pure model/controller layer -- it has zero terminal knowledge. This separation is itself an OOP principle (MVC).
- **JSON-RPC over stdin/stdout** is the simplest IPC that requires no network ports, no external dependencies, and works on all platforms. The Content-Length framing (borrowed from the Language Server Protocol) gives clean message boundaries.

### Process Lifecycle

```
1. User runs `charmed` (Go binary)
2. Go spawns Java: exec.Command("java", "-jar", "backend.jar")
   - Java's stdin  ← connected to Go's writer (Go sends requests)
   - Java's stdout → connected to Go's reader (Java sends responses/notifications)
   - Java's stderr → log file (not part of protocol)
3. Go sends "initialize" request, Java responds with capabilities
4. Normal operation: Go sends user actions, Java sends state updates
5. User quits → Go sends "shutdown" request → Java exits → Go exits
```

---

## Tech Stack

| Layer     | Technology         | Version | Purpose                              |
|-----------|--------------------|---------|--------------------------------------|
| Frontend  | Go                 | 1.22+   | TUI host process                     |
| Frontend  | Bubble Tea         | v2      | Terminal application framework       |
| Frontend  | Lip Gloss          | v2      | CSS-like terminal styling            |
| Frontend  | Glamour            | latest  | Markdown → ANSI rendering            |
| Frontend  | Bubbles            | v2      | Pre-built TUI components             |
| Backend   | Java               | 21+     | Business logic, OOP showcase         |
| Backend   | Gradle             | 8.x     | Build system                         |
| Backend   | Gson               | 2.x     | JSON serialization (Content-Length framing hand-rolled) |
| Protocol  | JSON-RPC 2.0       | --      | Request/response/notification format |
| Protocol  | Content-Length      | --      | Message framing (LSP-style)          |

---

## Responsibility Split

### Go Frontend Owns

- Terminal raw mode, key capture, mouse events
- Bubble Tea event loop (`Init` / `Update` / `View` cycle)
- Lip Gloss layout composition (split panes via `JoinHorizontal`, borders, padding, colors)
- **Glamour** for rendering markdown preview (NOT Glow -- Glow is a consumer app, not a library)
- Bubbles components: `viewport` (preview), `textarea` (editor), `textinput` (command palette), `list` (file browser), `filepicker`, `help`, `spinner`
- Translating key presses into JSON-RPC requests/notifications sent to Java
- Receiving state updates from Java and re-rendering the view

### Java Backend Owns

- Markdown parsing: raw text → AST (Abstract Syntax Tree) using the Composite pattern
- Document model: content storage, cursor position, selection ranges
- Editor state machine: Insert mode (State pattern)
- AST traversal for search, word count, export (Visitor pattern)
- Event system for state change propagation (Observer pattern)
- Multiple export formats: markdown, HTML, plain text (Strategy pattern)
- Configuration management (Builder pattern)
- All 12+ design patterns, Java 21+ features, SOLID compliance

---

## Directory Structure

```
charMeD/
├── backend/                          # Java backend (Gradle project)
│   ├── build.gradle.kts
│   ├── settings.gradle.kts
│   └── src/main/java/com/charmed/
│       ├── App.java                  # Entry point
│       ├── model/                    # AST nodes (Composite, Sealed types)
│       ├── document/                 # Document model (Builder, Observer)
│       ├── parser/                   # Markdown parser (Factory, Strategy)
│       ├── visitor/                  # AST visitors (Visitor pattern)
│       ├── editor/                   # Editor state machine (State pattern)
│       ├── renderer/                 # Export renderers (Strategy, Decorator)
│       ├── event/                    # Event bus (Observer, Generics)
│       ├── rpc/                      # JSON-RPC server (Adapter pattern)
│       ├── config/                   # Configuration (Builder)
│       └── exception/               # Custom exception hierarchy
│
├── frontend/                         # Go frontend (Go module)
│   ├── go.mod
│   ├── main.go                       # Spawns Java, runs Bubble Tea
│   ├── rpc/
│   │   ├── client.go                 # JSON-RPC client
│   │   └── messages.go               # Go structs for params/results
│   ├── ui/
│   │   ├── model.go                  # Bubble Tea Model (Init/Update/View)
│   │   ├── editor_pane.go            # Left pane: textarea for raw markdown
│   │   ├── preview_pane.go           # Right pane: Glamour-rendered viewport
│   │   ├── status_bar.go             # Bottom bar: mode, file, word count
│   │   ├── command_palette.go        # ':' command overlay (textinput)
│   │   ├── file_tree.go              # File browser (list/filepicker)
│   │   └── styles.go                 # All Lip Gloss style definitions
│   └── keys/
│       └── keys.go                   # Key bindings → RPC method mapping
│
├── docs/                             # Architecture documentation
│   ├── ARCHITECTURE.md               # This file
│   ├── BACKEND.md                    # Java backend design
│   ├── FRONTEND.md                   # Go frontend design
│   ├── PROTOCOL.md                   # JSON-RPC protocol specification
│   └── OOP_PATTERNS.md              # OOP patterns & Java features
│
├── Makefile                          # Build & run targets
├── .envrc                            # direnv config
└── devenv.nix                        # Nix development environment
```

---

## Build & Run

### Prerequisites

- Java 21+ (provided by devenv.nix)
- Go 1.22+ (provided by devenv.nix)
- Gradle 8.x (Gradle wrapper included in backend/)

### Makefile Targets

```makefile
# Build everything
make build          # Builds both backend JAR and frontend binary

# Build individually
make build-backend  # cd backend && gradle shadowJar
make build-frontend # cd frontend && go build -o charmed .

# Run
make run            # Builds and runs the frontend (which spawns the backend)

# Development
make dev            # Run with hot-reload (rebuild on file changes)
make test           # Run Java tests (cd backend && gradle test)
make clean          # Remove build artifacts
```

### Manual Build

```bash
# Backend
cd backend
gradle shadowJar
# Produces: backend/build/libs/backend-all.jar

# Frontend
cd frontend
go build -o charmed .

# Run
./frontend/charmed              # Opens empty editor
./frontend/charmed README.md    # Opens a file
```

---

## Data Flow

### Opening a File

```
User presses 'o' → Go captures key
  → Go sends JSON-RPC: {"method": "document/open", "params": {"path": "README.md"}}
  → Java reads file, parses to AST, initializes Document
  → Java responds: {"result": {"content": "...", "lineCount": 42}}
  → Java sends notification: {"method": "ui/refresh", "params": {"lines": [...], "cursor": {...}}}
  → Go receives notification, updates textarea + renders preview via Glamour
  → Bubble Tea re-renders the view
```

### Editing Text

```
User types characters → Go sends: {"method": "document/edit", "params": {"text": "hello", "position": {...}}}
  → Java inserts text into document, re-parses affected region, updates AST
  → Java publishes DocumentChangedEvent → RpcServer sends ui/refresh notification
  → Java notifies: {"method": "ui/refresh", "params": {"lines": [...], "cursor": {...}}}
  → Go re-renders editor pane + preview pane
```

---

## Key Design Decisions

| Decision | Choice | Rationale |
|----------|--------|-----------|
| IPC mechanism | stdin/stdout JSON-RPC | Simplest, no ports, no deps, LSP-proven |
| Markdown rendering | Glamour (not Glow) | Glow is a CLI app, not a library; Glamour is the rendering engine |
| Editor paradigm | Insert mode (State pattern) | `EditorMode` sealed interface delegates key handling; modes are extensible by design |
| Java version | 21+ | Sealed classes, records, pattern matching, virtual threads |
| Build system | Gradle + Go modules | Standard for each ecosystem |
| Message framing | Content-Length headers | Battle-tested in LSP; clean message boundaries |
| Frontend framework | Bubble Tea v2 | Most mature Go TUI framework; Elm architecture fits well |

---

## See Also

- [BACKEND.md](BACKEND.md) -- Java backend design in detail
- [FRONTEND.md](FRONTEND.md) -- Go frontend design in detail
- [PROTOCOL.md](PROTOCOL.md) -- Full JSON-RPC protocol specification
- [OOP_PATTERNS.md](OOP_PATTERNS.md) -- Design patterns and Java features
