# charMeD Protocol -- JSON-RPC 2.0 Specification

This document specifies the complete communication protocol between the Go frontend and Java backend. The protocol uses **JSON-RPC 2.0** over **stdin/stdout** with **Content-Length framing** (the same framing used by the Language Server Protocol).

---

## Transport Layer

### Content-Length Framing

Every message (in both directions) is prefixed with a `Content-Length` header followed by `\r\n\r\n` and the JSON body:

```
Content-Length: 82\r\n
\r\n
{"jsonrpc":"2.0","id":1,"method":"document/open","params":{"path":"README.md"}}
```

**Rules:**
- `Content-Length` is the byte length of the JSON body (not character length -- UTF-8)
- The header ends with `\r\n\r\n` (two CRLFs: one after the header value, one blank line)
- The JSON body immediately follows with no trailing newline
- Messages are sent sequentially (no interleaving of partial messages)

### Pipes

```
Go (parent) ──stdin pipe──►  Java (child)     Go sends requests/notifications
Go (parent) ◄──stdout pipe── Java (child)     Java sends responses/notifications
                stderr ──►   log file          Debug logging only (not protocol)
```

---

## JSON-RPC 2.0 Message Types

### Request (Go → Java)

```json
{
    "jsonrpc": "2.0",
    "id": 1,
    "method": "document/open",
    "params": {
        "path": "README.md"
    }
}
```

- `id`: integer, monotonically increasing, assigned by Go
- `method`: string, namespaced as `category/action`
- `params`: object (always an object, never an array)

### Response (Java → Go)

**Success:**

```json
{
    "jsonrpc": "2.0",
    "id": 1,
    "result": {
        "content": "# Hello\n\nWorld",
        "lineCount": 3,
        "wordCount": 2
    }
}
```

**Error:**

```json
{
    "jsonrpc": "2.0",
    "id": 1,
    "error": {
        "code": -32001,
        "message": "File not found: README.md",
        "data": {
            "path": "README.md"
        }
    }
}
```

### Notification (Either Direction, No Response Expected)

```json
{
    "jsonrpc": "2.0",
    "method": "ui/refresh",
    "params": {
        "lines": ["# Hello", "", "World"],
        "cursor": {"line": 0, "column": 0},
        "mode": "NORMAL"
    }
}
```

- No `id` field -- fire-and-forget
- No response is sent for notifications

---

## Lifecycle Messages

### `initialize` (Go → Java)

Sent once at startup. Java responds with its capabilities.

**Request:**

```json
{
    "jsonrpc": "2.0",
    "id": 1,
    "method": "initialize",
    "params": {
        "clientVersion": "0.1.0",
        "terminalWidth": 120,
        "terminalHeight": 40
    }
}
```

**Response:**

```json
{
    "jsonrpc": "2.0",
    "id": 1,
    "result": {
        "serverVersion": "0.1.0",
        "capabilities": {
            "undo": true,
            "redo": true,
            "search": true,
            "export": ["markdown", "html", "plaintext"]
        }
    }
}
```

### `shutdown` (Go → Java)

Sent when the user quits. Java should save any state and prepare to exit.

**Request:**

```json
{
    "jsonrpc": "2.0",
    "id": 99,
    "method": "shutdown",
    "params": {}
}
```

**Response:**

```json
{
    "jsonrpc": "2.0",
    "id": 99,
    "result": null
}
```

After receiving the response, Go closes stdin. Java detects EOF and exits.

---

## Document Methods

### `document/open`

Open a file, parse it, and initialize the document.

**Request:**

```json
{
    "jsonrpc": "2.0",
    "id": 2,
    "method": "document/open",
    "params": {
        "path": "/home/user/README.md"
    }
}
```

**Response:**

```json
{
    "jsonrpc": "2.0",
    "id": 2,
    "result": {
        "content": "# Hello\n\nThis is a **markdown** file.",
        "lineCount": 3,
        "wordCount": 6,
        "filePath": "/home/user/README.md"
    }
}
```

**Errors:**
- `-32001` FileNotFound: the path doesn't exist
- `-32002` ReadError: permission denied or IO error
- `-32003` ParseError: file content is corrupt (rare, markdown is lenient)

**Side Effects:** Java parses the content into an AST, initializes the Document and Editor, sends a `ui/refresh` notification.

---

### `document/new`

Create a new empty document.

**Request:**

```json
{
    "jsonrpc": "2.0",
    "id": 3,
    "method": "document/new",
    "params": {}
}
```

**Response:**

```json
{
    "jsonrpc": "2.0",
    "id": 3,
    "result": {
        "content": "",
        "lineCount": 1,
        "wordCount": 0,
        "filePath": null
    }
}
```

---

### `document/save`

Save the current document to disk.

**Request:**

```json
{
    "jsonrpc": "2.0",
    "id": 4,
    "method": "document/save",
    "params": {
        "path": "/home/user/README.md"
    }
}
```

If `path` is omitted, saves to the original file path (the one from `document/open`). If the document was created with `document/new` and no path is provided, returns an error.

**Response:**

```json
{
    "jsonrpc": "2.0",
    "id": 4,
    "result": {
        "saved": true,
        "path": "/home/user/README.md",
        "bytesWritten": 1024
    }
}
```

**Errors:**
- `-32004` WriteError: permission denied or IO error
- `-32005` NoPathError: new document with no path specified

---

### `document/edit`

Apply a text edit to the document. This is the core editing operation.

**Request:**

```json
{
    "jsonrpc": "2.0",
    "id": 5,
    "method": "document/edit",
    "params": {
        "action": "insert",
        "position": {
            "line": 2,
            "column": 5
        },
        "text": "hello world"
    }
}
```

**Edit Actions:**

| Action | Params | Description |
|--------|--------|-------------|
| `insert` | `position`, `text` | Insert text at position |
| `delete` | `range` (`start`, `end`) | Delete text in range |
| `replace` | `range`, `text` | Replace range with new text |
| `formatBold` | `range` | Wrap range in `**...**` |
| `formatItalic` | `range` | Wrap range in `*...*` |
| `newline` | `position` | Insert newline at position |

**Response:**

```json
{
    "jsonrpc": "2.0",
    "id": 5,
    "result": {
        "applied": true,
        "newCursor": {
            "line": 2,
            "column": 16
        }
    }
}
```

**Side Effects:** Creates an `EditorCommand`, executes it (pushes to undo stack), re-parses affected region, publishes `DocumentChangedEvent`, sends `ui/refresh` notification.

---

### `document/undo`

Undo the last edit operation.

**Request:**

```json
{
    "jsonrpc": "2.0",
    "id": 6,
    "method": "document/undo",
    "params": {}
}
```

**Response:**

```json
{
    "jsonrpc": "2.0",
    "id": 6,
    "result": {
        "undone": true,
        "description": "Insert 'hello world'",
        "newCursor": {
            "line": 2,
            "column": 5
        }
    }
}
```

If nothing to undo:

```json
{
    "jsonrpc": "2.0",
    "id": 6,
    "result": {
        "undone": false,
        "description": null,
        "newCursor": null
    }
}
```

---

### `document/redo`

Redo the last undone operation.

**Request:**

```json
{
    "jsonrpc": "2.0",
    "id": 7,
    "method": "document/redo",
    "params": {}
}
```

**Response:** Same shape as `document/undo` but with `"redone"` field.

---

### `document/getContent`

Get the current document content (for re-syncing preview, export, etc.).

**Request:**

```json
{
    "jsonrpc": "2.0",
    "id": 8,
    "method": "document/getContent",
    "params": {
        "format": "raw"
    }
}
```

**Format options:** `"raw"` (original markdown), `"html"`, `"plaintext"`

**Response:**

```json
{
    "jsonrpc": "2.0",
    "id": 8,
    "result": {
        "content": "# Hello\n\nThis is a **markdown** file.",
        "format": "raw"
    }
}
```

---

### `document/search`

Search the document for a pattern.

**Request:**

```json
{
    "jsonrpc": "2.0",
    "id": 9,
    "method": "document/search",
    "params": {
        "query": "markdown",
        "caseSensitive": false,
        "regex": false
    }
}
```

**Response:**

```json
{
    "jsonrpc": "2.0",
    "id": 9,
    "result": {
        "matches": [
            {
                "line": 2,
                "column": 14,
                "length": 8,
                "context": "This is a **markdown** file."
            }
        ],
        "totalMatches": 1
    }
}
```

---

### `document/export`

Export the document to a specific format.

**Request:**

```json
{
    "jsonrpc": "2.0",
    "id": 10,
    "method": "document/export",
    "params": {
        "format": "html",
        "outputPath": "/home/user/README.html"
    }
}
```

If `outputPath` is omitted, the exported content is returned in the response. If provided, the content is written to the file.

**Response (inline):**

```json
{
    "jsonrpc": "2.0",
    "id": 10,
    "result": {
        "content": "<h1>Hello</h1>\n<p>This is a <strong>markdown</strong> file.</p>",
        "format": "html"
    }
}
```

**Response (file):**

```json
{
    "jsonrpc": "2.0",
    "id": 10,
    "result": {
        "outputPath": "/home/user/README.html",
        "format": "html",
        "bytesWritten": 256
    }
}
```

---

## Editor Methods

### `editor/changeMode`

Change the editor mode (Normal, Insert, Command).

**Request:**

```json
{
    "jsonrpc": "2.0",
    "id": 11,
    "method": "editor/changeMode",
    "params": {
        "mode": "insert"
    }
}
```

**Valid modes:** `"normal"`, `"insert"`, `"command"`

**Response:**

```json
{
    "jsonrpc": "2.0",
    "id": 11,
    "result": {
        "previousMode": "normal",
        "currentMode": "insert"
    }
}
```

**Side Effects:** Transitions the `Editor` state machine, publishes `ModeChangedEvent`, sends `ui/statusBar` notification.

---

### `editor/moveCursor`

Move the cursor in the document.

**Request:**

```json
{
    "jsonrpc": "2.0",
    "id": 12,
    "method": "editor/moveCursor",
    "params": {
        "direction": "down",
        "count": 1
    }
}
```

**Directions:** `"up"`, `"down"`, `"left"`, `"right"`, `"lineStart"`, `"lineEnd"`, `"documentStart"`, `"documentEnd"`, `"wordForward"`, `"wordBackward"`, `"pageUp"`, `"pageDown"`

**Response:**

```json
{
    "jsonrpc": "2.0",
    "id": 12,
    "result": {
        "cursor": {
            "line": 3,
            "column": 0
        }
    }
}
```

---

### `editor/executeCommand`

Execute a command-mode command (`:w`, `:q`, `:e`, etc.).

**Request:**

```json
{
    "jsonrpc": "2.0",
    "id": 13,
    "method": "editor/executeCommand",
    "params": {
        "command": "w"
    }
}
```

**Response:**

```json
{
    "jsonrpc": "2.0",
    "id": 13,
    "result": {
        "executed": true,
        "output": "Written: /home/user/README.md (1024 bytes)"
    }
}
```

---

## Notifications (Java → Go)

These are **fire-and-forget** messages from Java to Go. They have no `id` field and expect no response. Go receives them in a listener goroutine and feeds them into Bubble Tea's `Update` loop.

### `ui/refresh`

Sent after any document or cursor change. This is the primary mechanism for keeping the Go frontend in sync.

```json
{
    "jsonrpc": "2.0",
    "method": "ui/refresh",
    "params": {
        "content": "# Hello\n\nThis is a **markdown** file.",
        "lines": ["# Hello", "", "This is a **markdown** file."],
        "cursor": {
            "line": 0,
            "column": 0
        },
        "mode": "NORMAL",
        "dirty": false
    }
}
```

**Fields:**
- `content`: full markdown content as a single string (for Glamour preview rendering)
- `lines`: array of line strings (for the textarea editor pane)
- `cursor`: current cursor position
- `mode`: current editor mode name
- `dirty`: whether document has unsaved changes

### `ui/statusBar`

Sent when metadata changes (mode, word count, etc.) without a full content refresh.

```json
{
    "jsonrpc": "2.0",
    "method": "ui/statusBar",
    "params": {
        "mode": "INSERT",
        "filePath": "README.md",
        "dirty": true,
        "wordCount": 342,
        "lineCount": 47,
        "cursor": {
            "line": 15,
            "column": 8
        }
    }
}
```

### `ui/searchResults`

Sent after a search completes.

```json
{
    "jsonrpc": "2.0",
    "method": "ui/searchResults",
    "params": {
        "query": "markdown",
        "matches": [
            {
                "line": 2,
                "column": 14,
                "length": 8,
                "context": "This is a **markdown** file."
            }
        ],
        "totalMatches": 1,
        "currentMatch": 0
    }
}
```

### `ui/message`

Sent for status messages, warnings, and errors that should be briefly displayed.

```json
{
    "jsonrpc": "2.0",
    "method": "ui/message",
    "params": {
        "level": "info",
        "text": "File saved successfully"
    }
}
```

**Levels:** `"info"`, `"warning"`, `"error"`

---

## Error Codes

Standard JSON-RPC 2.0 error codes plus application-specific codes:

| Code | Name | Description |
|------|------|-------------|
| `-32700` | ParseError | Invalid JSON |
| `-32600` | InvalidRequest | Not a valid JSON-RPC request |
| `-32601` | MethodNotFound | Unknown method |
| `-32602` | InvalidParams | Invalid method parameters |
| `-32603` | InternalError | Unexpected server error |
| `-32001` | FileNotFound | File does not exist |
| `-32002` | ReadError | Cannot read file (permissions, IO) |
| `-32003` | MarkdownParseError | Markdown parsing failed |
| `-32004` | WriteError | Cannot write file |
| `-32005` | NoPathError | Save without path on new document |
| `-32006` | InvalidModeTransition | Invalid editor mode change |
| `-32007` | ExportError | Export format error |
| `-32008` | CommandNotFound | Unknown command-mode command |

---

## Message Flow Diagrams

### Opening a File

```
Go                                  Java
│                                    │
│──── document/open ────────────────►│
│     {path: "README.md"}           │
│                                    │ reads file
│                                    │ parses to AST
│                                    │ initializes Document
│                                    │ initializes Editor (Normal mode)
│◄─── response ─────────────────────│
│     {content, lineCount, ...}     │
│                                    │
│◄─── ui/refresh (notification) ────│
│     {lines, cursor, mode}         │
│                                    │
│  Go renders: editor pane (lines)   │
│  Go renders: preview (Glamour)     │
│  Go renders: status bar            │
```

### Typing in Insert Mode

```
Go                                  Java
│                                    │
│──── editor/changeMode ────────────►│
│     {mode: "insert"}              │
│◄─── response ─────────────────────│
│                                    │
│◄─── ui/statusBar (notification) ──│
│     {mode: "INSERT"}              │
│                                    │
│  User types "Hello"               │
│                                    │
│──── document/edit ────────────────►│
│     {action:"insert",text:"Hello"} │ creates InsertTextCommand
│                                    │ executes command
│                                    │ pushes to undo stack
│                                    │ re-parses affected lines
│◄─── response ─────────────────────│
│     {applied, newCursor}          │
│                                    │
│◄─── ui/refresh (notification) ────│
│     {lines, cursor, mode, dirty}  │
│                                    │
│  Go updates: textarea content      │
│  Go re-renders: Glamour preview    │
```

### Undo/Redo

```
Go                                  Java
│                                    │
│──── document/undo ────────────────►│
│                                    │ pops from undo stack
│                                    │ calls command.undo()
│                                    │ pushes to redo stack
│                                    │ re-parses affected region
│◄─── response ─────────────────────│
│     {undone, description, cursor} │
│                                    │
│◄─── ui/refresh (notification) ────│
│     {lines, cursor, mode}         │
```

### Search

```
Go                                  Java
│                                    │
│──── document/search ──────────────►│
│     {query: "hello"}              │ creates SearchVisitor
│                                    │ traverses AST
│                                    │ collects matches
│◄─── response ─────────────────────│
│     {matches: [...]}              │
│                                    │
│◄─── ui/searchResults ────────────│
│     {matches, currentMatch}       │
│                                    │
│  Go highlights matches in editor   │
│  Go scrolls to first match         │
```

### Startup / Shutdown

```
Go                                  Java
│                                    │
│  spawns Java process               │
│                                    │ Java starts, initializes
│──── initialize ───────────────────►│
│     {clientVersion, terminal size} │
│◄─── response ─────────────────────│
│     {serverVersion, capabilities} │
│                                    │
│  ... normal operation ...          │
│                                    │
│──── shutdown ─────────────────────►│
│                                    │ saves state if needed
│◄─── response ─────────────────────│
│     null                          │
│                                    │
│  Go closes stdin pipe              │
│                                    │ Java detects EOF, exits
│  Go waits for process exit         │
```

---

## Implementation Notes

### Batching

For performance, Go MAY batch rapid keystrokes into a single `document/edit` request with concatenated text, rather than sending one request per character. Java handles both single-character and multi-character inserts.

### Ordering

Messages are strictly ordered within each direction. Go sends requests sequentially (waiting for responses before sending the next request that depends on it). Notifications from Java are processed in order by Go's listener goroutine.

### Error Recovery

If Java crashes (process exits unexpectedly):
1. Go detects the pipe close
2. Go displays an error message
3. Go attempts to save any cached content locally as a recovery file
4. Go exits gracefully

If a single request fails (error response):
1. Go displays the error message in the status bar via `ui/message`
2. Go continues operating -- a single error doesn't crash the session

---

## See Also

- [ARCHITECTURE.md](ARCHITECTURE.md) -- System overview
- [BACKEND.md](BACKEND.md) -- Java backend design
- [FRONTEND.md](FRONTEND.md) -- Go frontend design
- [OOP_PATTERNS.md](OOP_PATTERNS.md) -- Design patterns and Java features
