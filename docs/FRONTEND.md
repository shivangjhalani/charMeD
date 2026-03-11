# charMeD Frontend -- Go TUI Design

The Go frontend is a **pure view layer**. It owns the terminal, captures user input, renders the UI, and communicates with the Java backend via JSON-RPC over stdin/stdout. It contains **zero business logic** -- all editing, parsing, and document management happens in Java.

---

## Charm Libraries Used

| Library | Version | Purpose in charMeD |
|---------|---------|---------------------|
| [Bubble Tea](https://github.com/charmbracelet/bubbletea) | v2 | Application framework -- Elm architecture (Init/Update/View) |
| [Lip Gloss](https://github.com/charmbracelet/lipgloss) | v2 | CSS-like terminal styling: colors, borders, padding, layout composition |
| [Glamour](https://github.com/charmbracelet/glamour) | latest | Markdown string → ANSI-styled string rendering for the preview pane |
| [Bubbles](https://github.com/charmbracelet/bubbles) | v2 | Pre-built TUI components: viewport, textarea, textinput, list, etc. |

### Why Glamour, Not Glow?

**Glow** (23.5k stars) is a *consumer application* -- a CLI tool for reading markdown files. It has no public rendering API and cannot be imported as a library. Internally, Glow uses **Glamour** for its rendering. Glamour (3.3k stars) is the actual rendering library with a clean public API:

```go
renderer, _ := glamour.NewTermRenderer(
    glamour.WithAutoStyle(),    // auto-detect light/dark terminal
    glamour.WithWordWrap(80),   // wrap at 80 columns
)
rendered, _ := renderer.Render(markdownString)
// rendered is an ANSI-styled string ready for terminal output
```

We use Glamour directly in the preview pane.

---

## File Structure

```
frontend/
├── main.go                    # Entry point: spawns Java, sets up RPC, runs Bubble Tea
├── go.mod                     # Module: github.com/charmed/frontend
│
├── rpc/
│   ├── client.go              # JSON-RPC client: send requests, receive notifications
│   └── messages.go            # Go structs for all RPC params and results
│
├── ui/
│   ├── model.go               # Root Bubble Tea Model (Init/Update/View)
│   ├── editor_pane.go         # Left pane: bubbles/textarea for raw markdown editing
│   ├── preview_pane.go        # Right pane: Glamour-rendered markdown in bubbles/viewport
│   ├── status_bar.go          # Bottom bar: mode indicator, filename, word count, line info
│   ├── command_palette.go     # ':' command overlay using bubbles/textinput
│   ├── file_tree.go           # File browser using bubbles/list + filepicker
│   └── styles.go              # ALL Lip Gloss style definitions centralized here
│
└── keys/
    └── keys.go                # Key bindings: maps keys to RPC method calls
```

---

## `main.go` -- Entry Point

```go
func main() {
    // 1. Spawn Java backend as child process
    cmd := exec.Command("java", "-jar", "backend/build/libs/backend-all.jar")
    stdin, _ := cmd.StdinPipe()    // Go writes to Java's stdin
    stdout, _ := cmd.StdoutPipe()  // Go reads from Java's stdout
    cmd.Stderr = logFile           // Java's stderr → log file (not protocol)
    cmd.Start()

    // 2. Create JSON-RPC client over the pipes
    rpcClient := rpc.NewClient(stdin, stdout)

    // 3. Send "initialize" request to Java
    rpcClient.Initialize()

    // 4. Create Bubble Tea model
    model := ui.NewModel(rpcClient)

    // 5. Run Bubble Tea program
    p := tea.NewProgram(model, tea.WithAltScreen(), tea.WithMouseAllMotion())
    p.Run()

    // 6. On exit: send "shutdown" to Java, wait for process to exit
    rpcClient.Shutdown()
    cmd.Wait()
}
```

---

## Bubble Tea Model -- The Elm Architecture

Bubble Tea uses the Elm architecture: `Init` → `Update` → `View` in a loop. The model is the single source of truth for the UI state.

### `ui/model.go`

```go
type Model struct {
    rpc           *rpc.Client
    editorPane    EditorPane      // left: raw markdown textarea
    previewPane   PreviewPane     // right: Glamour-rendered viewport
    statusBar     StatusBar       // bottom bar
    commandPalette CommandPalette // ':' command overlay
    fileTree      FileTree        // file browser overlay

    width, height int             // terminal dimensions
    activePane    Pane            // which pane has focus (editor or preview)
    showFileTree  bool            // is file browser visible?
    showCommand   bool            // is command palette visible?
}

type Pane int
const (
    EditorFocus Pane = iota
    PreviewFocus
)
```

**Init** -- Sets up the initial state, starts listening for Java notifications:

```go
func (m Model) Init() tea.Cmd {
    return tea.Batch(
        m.editorPane.Init(),
        m.listenForNotifications(),  // goroutine reading RPC notifications
    )
}
```

**Update** -- Processes all messages (key presses, mouse, resize, RPC notifications):

```go
func (m Model) Update(msg tea.Msg) (tea.Model, tea.Cmd) {
    switch msg := msg.(type) {
    case tea.KeyMsg:
        return m.handleKey(msg)
    case tea.WindowSizeMsg:
        return m.handleResize(msg)
    case rpc.NotificationMsg:
        return m.handleNotification(msg)
    // ...
    }
}
```

**View** -- Composes the final terminal output using Lip Gloss:

```go
func (m Model) View() string {
    editor := m.editorPane.View()
    preview := m.previewPane.View()

    // Split panes side by side
    mainContent := lipgloss.JoinHorizontal(lipgloss.Top, editor, preview)

    // Stack vertically: content + status bar
    return lipgloss.JoinVertical(lipgloss.Left, mainContent, m.statusBar.View())
}
```

---

## Panes and Components

### Editor Pane (Left) -- `ui/editor_pane.go`

Uses **`bubbles/textarea`** for multi-line text input.

```go
type EditorPane struct {
    textarea textarea.Model
    style    lipgloss.Style
}
```

- The textarea holds the raw markdown content
- When the user types, the Go frontend sends the edit operation to Java via JSON-RPC
- Java processes the edit (creates a command, updates the document, re-parses)
- Java sends back a `ui/refresh` notification with updated state
- The textarea content is synchronized with Java's document state

**Key behaviors:**
- In **Insert mode**: keystrokes go to the textarea and are forwarded to Java
- In **Normal mode**: keystrokes are navigation commands sent to Java
- The textarea's cursor position is kept in sync with Java's `CursorPosition`

### Preview Pane (Right) -- `ui/preview_pane.go`

Uses **`bubbles/viewport`** to display Glamour-rendered markdown.

```go
type PreviewPane struct {
    viewport viewport.Model
    renderer *glamour.TermRenderer
    style    lipgloss.Style
}

func (p *PreviewPane) SetContent(markdown string) {
    rendered, _ := p.renderer.Render(markdown)
    p.viewport.SetContent(rendered)
}
```

- When Java sends a `ui/refresh` notification, the full markdown content is re-rendered through Glamour
- Glamour handles: headings, bold/italic, code blocks with syntax highlighting, links, lists, blockquotes, horizontal rules, tables
- The viewport provides scrolling when content exceeds the pane height
- Glamour's `WithAutoStyle()` automatically picks light or dark theme based on terminal background

**Glamour renderer setup:**

```go
func NewPreviewPane(width int) PreviewPane {
    renderer, _ := glamour.NewTermRenderer(
        glamour.WithAutoStyle(),
        glamour.WithWordWrap(width - 4),  // account for borders/padding
    )
    // ...
}
```

### Status Bar (Bottom) -- `ui/status_bar.go`

A single-line bar at the bottom showing current state.

```go
type StatusBar struct {
    mode      string   // "NORMAL", "INSERT", "COMMAND"
    filename  string   // current file path or "[No File]"
    wordCount int
    lineCount int
    cursorLine int
    cursorCol  int
    dirty     bool     // unsaved changes indicator
    style     lipgloss.Style
}
```

**Rendered output example:**

```
 NORMAL │ README.md [+] │ Ln 15, Col 8 │ 342 words │ 47 lines
```

The mode indicator uses different background colors:
- Normal: subtle gray
- Insert: green
- Command: yellow

### Command Palette -- `ui/command_palette.go`

Uses **`bubbles/textinput`** for single-line command input. Appears when the user presses `:` in Normal mode.

```go
type CommandPalette struct {
    textinput textinput.Model
    visible   bool
    style     lipgloss.Style
}
```

Supports commands like:
- `:w` -- save
- `:q` -- quit
- `:wq` -- save and quit
- `:e <path>` -- open file
- `:/pattern` -- search
- `:export html` -- export to HTML

When the user presses Enter, the command is sent to Java via `editor/executeCommand` RPC.

### File Tree -- `ui/file_tree.go`

Uses **`bubbles/list`** with fuzzy filtering for file selection, and **`bubbles/filepicker`** for filesystem navigation.

```go
type FileTree struct {
    list    list.Model
    picker  filepicker.Model
    visible bool
    style   lipgloss.Style
}
```

- Activated by a keybinding (e.g., `Ctrl+o` or `:e`)
- Shows files in the current directory with fuzzy search
- Selecting a file sends `document/open` to Java

---

## Lip Gloss Styling Strategy -- `ui/styles.go`

All Lip Gloss style definitions are centralized in a single file for consistency and easy theming.

```go
package ui

import "github.com/charmbracelet/lipgloss/v2"

// Colors
var (
    primaryColor   = lipgloss.Color("#7C3AED")  // purple
    secondaryColor = lipgloss.Color("#06B6D4")  // cyan
    accentColor    = lipgloss.Color("#F59E0B")  // amber
    bgColor        = lipgloss.Color("#1E1E2E")  // dark background
    fgColor        = lipgloss.Color("#CDD6F4")  // light foreground
    mutedColor     = lipgloss.Color("#6C7086")  // muted text
    successColor   = lipgloss.Color("#A6E3A1")  // green
    errorColor     = lipgloss.Color("#F38BA8")  // red
)

// Pane styles
var (
    editorPaneStyle = lipgloss.NewStyle().
        BorderStyle(lipgloss.RoundedBorder()).
        BorderForeground(primaryColor).
        Padding(0, 1)

    previewPaneStyle = lipgloss.NewStyle().
        BorderStyle(lipgloss.RoundedBorder()).
        BorderForeground(secondaryColor).
        Padding(0, 1)

    activeBorderColor   = primaryColor
    inactiveBorderColor = mutedColor
)

// Status bar styles
var (
    statusBarStyle = lipgloss.NewStyle().
        Background(lipgloss.Color("#313244")).
        Foreground(fgColor).
        Padding(0, 1)

    normalModeStyle = lipgloss.NewStyle().
        Background(mutedColor).
        Foreground(lipgloss.Color("#000000")).
        Padding(0, 1).
        Bold(true)

    insertModeStyle = lipgloss.NewStyle().
        Background(successColor).
        Foreground(lipgloss.Color("#000000")).
        Padding(0, 1).
        Bold(true)

    commandModeStyle = lipgloss.NewStyle().
        Background(accentColor).
        Foreground(lipgloss.Color("#000000")).
        Padding(0, 1).
        Bold(true)
)

// File tree styles
var (
    fileTreeStyle = lipgloss.NewStyle().
        BorderStyle(lipgloss.RoundedBorder()).
        BorderForeground(accentColor).
        Padding(1, 2).
        Width(40)
)
```

### Layout Composition

The main layout uses Lip Gloss's join functions:

```
┌──────────────────────┬──────────────────────┐
│                      │                      │
│    Editor Pane       │    Preview Pane       │
│    (textarea)        │    (Glamour viewport) │
│                      │                      │
│    Raw markdown      │    Rendered markdown  │
│    with cursor       │    with styling       │
│                      │                      │
├──────────────────────┴──────────────────────┤
│ NORMAL │ README.md [+] │ Ln 15, Col 8 │ ...│
└─────────────────────────────────────────────┘
```

```go
func (m Model) View() string {
    // Calculate pane widths (50/50 split minus borders)
    halfWidth := m.width / 2

    // Style panes with correct width/height
    editor := editorPaneStyle.Width(halfWidth).Height(m.height - 2).
        Render(m.editorPane.View())
    preview := previewPaneStyle.Width(halfWidth).Height(m.height - 2).
        Render(m.previewPane.View())

    // Highlight active pane border
    if m.activePane == EditorFocus {
        editor = editorPaneStyle.BorderForeground(activeBorderColor).
            Width(halfWidth).Height(m.height - 2).Render(m.editorPane.View())
    } else {
        preview = previewPaneStyle.BorderForeground(activeBorderColor).
            Width(halfWidth).Height(m.height - 2).Render(m.previewPane.View())
    }

    // Compose layout
    main := lipgloss.JoinHorizontal(lipgloss.Top, editor, preview)
    statusBar := statusBarStyle.Width(m.width).Render(m.statusBar.View())

    return lipgloss.JoinVertical(lipgloss.Left, main, statusBar)
}
```

### Responsive Resizing

On `tea.WindowSizeMsg`, all components are resized proportionally:

```go
func (m Model) handleResize(msg tea.WindowSizeMsg) (tea.Model, tea.Cmd) {
    m.width = msg.Width
    m.height = msg.Height

    halfWidth := m.width / 2
    contentHeight := m.height - 2  // minus status bar

    m.editorPane.SetSize(halfWidth - 2, contentHeight - 2)   // minus border
    m.previewPane.SetSize(halfWidth - 2, contentHeight - 2)

    // Recreate Glamour renderer with new width for proper word wrapping
    m.previewPane.UpdateWidth(halfWidth - 4)

    return m, nil
}
```

---

## Bubbles Component Mapping

| Bubbles Component | charMeD Usage | Location |
|-------------------|---------------|----------|
| `textarea` | Raw markdown editor (left pane) | `editor_pane.go` |
| `viewport` | Glamour-rendered preview (right pane) | `preview_pane.go` |
| `textinput` | Command palette (`:` commands) | `command_palette.go` |
| `list` | File browser with fuzzy filtering | `file_tree.go` |
| `filepicker` | Filesystem navigation | `file_tree.go` |
| `help` | Keybinding help bar (toggle with `?`) | `model.go` |
| `spinner` | Loading indicator during file open/export | `status_bar.go` |
| `key` | Keybinding definitions | `keys/keys.go` |

---

## Key Bindings -- `keys/keys.go`

Key bindings map terminal key presses to RPC method calls. The mapping depends on the current editor mode (which Java controls).

```go
package keys

import "github.com/charmbracelet/bubbles/v2/key"

type KeyMap struct {
    // Normal mode
    Quit         key.Binding
    Save         key.Binding
    Open         key.Binding
    EnterInsert  key.Binding
    EnterCommand key.Binding
    Undo         key.Binding
    Redo         key.Binding
    MoveUp       key.Binding
    MoveDown     key.Binding
    MoveLeft     key.Binding
    MoveRight    key.Binding
    SwitchPane   key.Binding
    Search       key.Binding
    Help         key.Binding

    // Global
    ForceQuit    key.Binding
}

var DefaultKeyMap = KeyMap{
    Quit:         key.NewBinding(key.WithKeys("q"), key.WithHelp("q", "quit")),
    Save:         key.NewBinding(key.WithKeys("ctrl+s"), key.WithHelp("ctrl+s", "save")),
    Open:         key.NewBinding(key.WithKeys("ctrl+o"), key.WithHelp("ctrl+o", "open file")),
    EnterInsert:  key.NewBinding(key.WithKeys("i"), key.WithHelp("i", "insert mode")),
    EnterCommand: key.NewBinding(key.WithKeys(":"), key.WithHelp(":", "command mode")),
    Undo:         key.NewBinding(key.WithKeys("u"), key.WithHelp("u", "undo")),
    Redo:         key.NewBinding(key.WithKeys("ctrl+r"), key.WithHelp("ctrl+r", "redo")),
    MoveUp:       key.NewBinding(key.WithKeys("k", "up"), key.WithHelp("k/up", "up")),
    MoveDown:     key.NewBinding(key.WithKeys("j", "down"), key.WithHelp("j/down", "down")),
    MoveLeft:     key.NewBinding(key.WithKeys("h", "left"), key.WithHelp("h/left", "left")),
    MoveRight:    key.NewBinding(key.WithKeys("l", "right"), key.WithHelp("l/right", "right")),
    SwitchPane:   key.NewBinding(key.WithKeys("tab"), key.WithHelp("tab", "switch pane")),
    Search:       key.NewBinding(key.WithKeys("/"), key.WithHelp("/", "search")),
    Help:         key.NewBinding(key.WithKeys("?"), key.WithHelp("?", "help")),
    ForceQuit:    key.NewBinding(key.WithKeys("ctrl+c"), key.WithHelp("ctrl+c", "force quit")),
}
```

### Key → RPC Mapping

When a key is pressed in the Go frontend, it's translated to an RPC call:

| Key (Normal mode) | RPC Method | RPC Params |
|--------------------|------------|------------|
| `i` | `editor/changeMode` | `{"mode": "insert"}` |
| `:` | `editor/changeMode` | `{"mode": "command"}` |
| `h/j/k/l` | `editor/moveCursor` | `{"direction": "left/down/up/right"}` |
| `u` | `document/undo` | `{}` |
| `Ctrl+r` | `document/redo` | `{}` |
| `/` | (local) open search input | -- |
| `q` | `shutdown` | `{}` |

In **Insert mode**, all printable characters are sent as `document/edit` with the typed text. `Esc` sends `editor/changeMode` with `{"mode": "normal"}`.

---

## RPC Client -- `rpc/client.go`

The RPC client wraps stdin/stdout communication with Content-Length framing.

```go
type Client struct {
    writer io.Writer  // connected to Java's stdin
    reader io.Reader  // connected to Java's stdout
    id     int64      // incrementing request ID
    mu     sync.Mutex // protects writer
}

func (c *Client) Request(method string, params any) (json.RawMessage, error) {
    // 1. Marshal params to JSON
    // 2. Build JSON-RPC request: {"jsonrpc":"2.0","id":N,"method":"...","params":{...}}
    // 3. Frame with Content-Length header
    // 4. Write to Java's stdin
    // 5. Read response from Java's stdout (Content-Length framed)
    // 6. Return result or error
}

func (c *Client) Notify(method string, params any) error {
    // Same as Request but no "id" field (fire-and-forget)
}
```

### Notification Listener

A separate goroutine reads notifications from Java and sends them as Bubble Tea messages:

```go
func (c *Client) ListenForNotifications(p *tea.Program) {
    for {
        msg, err := c.readMessage()
        if err != nil {
            return
        }
        if msg.ID == nil {  // notification (no ID)
            p.Send(NotificationMsg{Method: msg.Method, Params: msg.Params})
        }
    }
}
```

This integrates Java's push notifications into Bubble Tea's `Update` loop.

---

## Go Dependencies

```
github.com/charmbracelet/bubbletea/v2     # TUI framework
github.com/charmbracelet/lipgloss/v2      # Styling
github.com/charmbracelet/glamour          # Markdown rendering
github.com/charmbracelet/bubbles/v2       # TUI components
```

No JSON-RPC library is needed on the Go side -- the Content-Length framing and JSON-RPC message format are simple enough to implement directly (~100 lines).

---

## See Also

- [ARCHITECTURE.md](ARCHITECTURE.md) -- System overview
- [BACKEND.md](BACKEND.md) -- Java backend design
- [PROTOCOL.md](PROTOCOL.md) -- Full JSON-RPC protocol specification
- [OOP_PATTERNS.md](OOP_PATTERNS.md) -- Design patterns and Java features
