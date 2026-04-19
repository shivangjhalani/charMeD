# charMeD Backend -- Java OOP Design

The Java backend is the heart of charMeD. It contains **all business logic** -- parsing, document management, editor state, undo/redo, search, export -- while exposing everything through a JSON-RPC 2.0 interface over stdin/stdout. The Go frontend is a pure view layer; the Java backend is the model and controller.

---

## Package Overview

```
com.charmed/
├── App.java                    # Entry point, wires everything together
├── model/                      # AST nodes          → Composite, Sealed Types
├── document/                   # Document state      → Builder, Observer
├── parser/                     # Markdown parsing    → Factory, Strategy
├── visitor/                    # AST traversal       → Visitor Pattern
├── editor/                     # Editor modes        → State Pattern
├── renderer/                   # Export formats      → Strategy, Decorator
├── event/                      # Event system        → Observer, Generics
├── rpc/                        # JSON-RPC server     → Adapter Pattern
├── config/                     # Configuration       → Builder Pattern
└── exception/                  # Error hierarchy     → Custom Exceptions
```

---

## Package Details

### `App.java` -- Entry Point

```java
public final class App {
    public static void main(String[] args) {
        // 1. Load configuration (Builder pattern)
        // 2. Create EventBus (Observer pattern)
        // 3. Create DocumentManager
        // 4. Create Editor (State pattern)
        // 5. Start RpcServer on stdin/stdout (Adapter pattern)
        // 6. Block on RPC message loop (virtual threads for concurrency)
    }
}
```

Wire-up is done manually (no DI framework) to keep the OOP patterns explicit and visible.

---

### `model/` -- Markdown AST (Composite Pattern + Sealed Types)

This package defines the Abstract Syntax Tree for parsed markdown documents. Every markdown element is a node in a tree. Container nodes (document, blockquote, list) hold children; leaf nodes (text, code, horizontal rule) are terminal.

**Sealed Interface Hierarchy:**

```java
public sealed interface MarkdownNode
    permits DocumentNode, HeadingNode, ParagraphNode, CodeBlockNode,
            InlineCodeNode, ListNode, ListItemNode, BlockquoteNode,
            BoldNode, ItalicNode, LinkNode, HorizontalRuleNode, TextNode {

    void accept(NodeVisitor visitor);   // Visitor pattern hook
    List<MarkdownNode> children();      // Composite pattern
    String rawText();                   // Plain text content
    NodeType nodeType();                // Enum discriminator
}
```

**Why sealed?** Java 21's `sealed` keyword restricts which classes can implement the interface. This enables exhaustive `switch` expressions with pattern matching -- the compiler verifies every node type is handled, eliminating `default` catch-alls that hide bugs.

**Node Types:**

| Class | Type | Children? | Purpose |
|-------|------|-----------|---------|
| `DocumentNode` | Container | Yes | Root of the AST, holds all top-level blocks |
| `HeadingNode` | Container | Yes | `# Heading` -- has level (1-6), children are inline nodes |
| `ParagraphNode` | Container | Yes | Block of text, children are inline nodes |
| `CodeBlockNode` | Leaf | No | ` ```language ... ``` ` fenced code blocks |
| `InlineCodeNode` | Leaf | No | `` `inline code` `` |
| `ListNode` | Container | Yes | Ordered or unordered list, children are `ListItemNode` |
| `ListItemNode` | Container | Yes | Single list item, children are inline/block nodes |
| `BlockquoteNode` | Container | Yes | `> quoted text`, children are block nodes |
| `BoldNode` | Container | Yes | `**bold**`, children are inline nodes |
| `ItalicNode` | Container | Yes | `*italic*`, children are inline nodes |
| `LinkNode` | Leaf | No | `[text](url)` -- stores text and URL |
| `HorizontalRuleNode` | Leaf | No | `---` or `***` |
| `TextNode` | Leaf | No | Raw text content, the terminal node |

**Supporting Enum:**

```java
public enum NodeType {
    DOCUMENT, HEADING, PARAGRAPH, CODE_BLOCK, INLINE_CODE,
    LIST, LIST_ITEM, BLOCKQUOTE, BOLD, ITALIC, LINK,
    HORIZONTAL_RULE, TEXT
}
```

**OOP Patterns:**
- **Composite** -- Uniform treatment of container and leaf nodes via the `MarkdownNode` interface. `children()` returns an empty list for leaf nodes.
- **Sealed Types** -- Exhaustive pattern matching in visitors, renderers, and switch expressions.

---

### `document/` -- Document State (Builder + Observer)

Manages the document's content, cursor, and selection. This is the "Model" in MVC.

| Class | Type | Purpose |
|-------|------|---------|
| `Document` | Class | Holds the raw markdown text (as a list of lines), the parsed AST, cursor position, selection range, file path, dirty flag. Built via a Builder. |
| `DocumentManager` | Class | Manages the active document. Delegates parsing, coordinates with the EventBus to broadcast changes. Facade for document operations. |
| `CursorPosition` | Record | `record CursorPosition(int line, int column)` -- immutable cursor. |
| `SelectionRange` | Record | `record SelectionRange(CursorPosition start, CursorPosition end)` -- immutable selection. |

**Document Builder:**

```java
public class Document {
    // Private constructor -- must use Builder
    private Document(Builder builder) { ... }

    public static class Builder {
        public Builder filePath(Path path) { ... }
        public Builder content(String rawMarkdown) { ... }
        public Builder cursor(CursorPosition cursor) { ... }
        public Document build() { ... }  // parses content into AST
    }
}
```

**Why records for CursorPosition/SelectionRange?** These are pure data carriers with no behavior beyond accessors. Java records give us `equals()`, `hashCode()`, `toString()` for free, plus immutability guarantees -- a cursor at line 5, column 3 is a value, not an identity.

**OOP Patterns:**
- **Builder** -- `Document.Builder` for step-by-step construction of complex Document objects.
- **Observer** -- `DocumentManager` publishes `DocumentChangedEvent` through the `EventBus` whenever content changes.

---

### `parser/` -- Markdown Parsing (Factory + Strategy)

Converts raw markdown text into the AST defined in `model/`.

| Class | Type | Purpose |
|-------|------|---------|
| `Parser` | Interface | `MarkdownNode parse(String input)` -- strategy interface for parsing implementations. |
| `MarkdownParser` | Class | Concrete parser. Tokenizes markdown, then builds the AST using recursive descent. |
| `NodeFactory` | Class | Factory for creating `MarkdownNode` instances from tokens. Centralizes node construction, enforces valid combinations. |
| `Token` | Record | `record Token(TokenType type, String value, int line, int column)` -- lexer output. |
| `TokenType` | Enum | `HEADING`, `PARAGRAPH`, `CODE_FENCE`, `BOLD_MARKER`, `ITALIC_MARKER`, `LINK_OPEN`, `LIST_BULLET`, `BLOCKQUOTE_MARKER`, `HORIZONTAL_RULE`, `TEXT`, `NEWLINE`, `EOF` |

**Parsing Strategy:**

```java
public interface Parser {
    MarkdownNode parse(String input);
}

public class MarkdownParser implements Parser {
    private final NodeFactory factory;

    @Override
    public MarkdownNode parse(String input) {
        List<Token> tokens = tokenize(input);
        return buildAst(tokens);
    }
}
```

The `Parser` interface allows swapping implementations (e.g., a `StrictParser` vs `LenientParser`) without changing any consuming code. Currently only `MarkdownParser` exists, but the Strategy slot is there for extensibility.

**NodeFactory:**

```java
public class NodeFactory {
    public HeadingNode createHeading(int level, List<MarkdownNode> children) { ... }
    public ParagraphNode createParagraph(List<MarkdownNode> children) { ... }
    public CodeBlockNode createCodeBlock(String language, String code) { ... }
    public TextNode createText(String content) { ... }
    // ... one factory method per node type
}
```

**OOP Patterns:**
- **Factory** -- `NodeFactory` centralizes node creation, decoupling parser logic from concrete node constructors.
- **Strategy** -- `Parser` interface allows pluggable parsing implementations.

---

### `visitor/` -- AST Traversal (Visitor Pattern)

The Visitor pattern decouples AST operations from the node classes. Adding a new operation (like "count words" or "export to HTML") requires adding a new Visitor -- no modification of any node class.

| Class | Type | Purpose |
|-------|------|---------|
| `NodeVisitor` | Interface | Defines `visit(XxxNode)` methods for every sealed node type. |
| `PlainTextVisitor` | Class | Traverses the AST, extracts all text content as a single plain string. |
| `HtmlExportVisitor` | Class | Traverses the AST, produces HTML output. |
| `MarkdownExportVisitor` | Class | Traverses the AST, re-serializes back to markdown (useful after AST transformations). |
| `WordCountVisitor` | Class | Counts words across all text nodes. |
| `SearchVisitor` | Class | Finds all occurrences of a search term, returns line/column positions. |

**Visitor Interface:**

```java
public interface NodeVisitor {
    void visit(DocumentNode node);
    void visit(HeadingNode node);
    void visit(ParagraphNode node);
    void visit(CodeBlockNode node);
    void visit(InlineCodeNode node);
    void visit(ListNode node);
    void visit(ListItemNode node);
    void visit(BlockquoteNode node);
    void visit(BoldNode node);
    void visit(ItalicNode node);
    void visit(LinkNode node);
    void visit(HorizontalRuleNode node);
    void visit(TextNode node);
}
```

Each `MarkdownNode` implementation has:

```java
@Override
public void accept(NodeVisitor visitor) {
    visitor.visit(this);
}
```

**Why Visitor?** The AST is a **sealed** hierarchy -- we know all node types at compile time and they change rarely. But operations on the AST change frequently (new export formats, new analysis tools). Visitor lets us add operations without touching node classes. This is the textbook use case.

**OOP Patterns:**
- **Visitor** -- Double dispatch via `accept()`/`visit()` for extensible AST operations.

---

### `editor/` -- Editor State Machine (State Pattern)

The editor mode system uses the State pattern. Currently only `InsertMode` is implemented; the sealed interface is structured to support additional modes in the future.

| Class | Type | Purpose |
|-------|------|---------|
| `Editor` | Class | The context. Holds the current `EditorMode`, delegates key handling to it. Manages the `Document` via `DocumentManager` and `EventBus`. |
| `EditorMode` | Sealed Interface | `sealed interface EditorMode permits InsertMode` |
| `InsertMode` | Class | Text input. Characters are inserted at cursor position. |
| `HandleResult` | Class | Result returned from `handleKey()` -- captures what action was taken. |

**State Interface:**

```java
public sealed interface EditorMode permits InsertMode {
    HandleResult handleKey(Editor editor, String key);
    String modeName();
}
```

**Editor Context:**

```java
public class Editor {
    private EditorMode currentMode;
    private final DocumentManager documentManager;
    private final EventBus eventBus;

    public Editor(DocumentManager documentManager, EventBus eventBus) {
        this.currentMode = new InsertMode();  // starts in InsertMode
    }

    public HandleResult processKey(String key) {
        return currentMode.handleKey(this, key);
    }

    public void transitionTo(EditorMode newMode) {
        // No-op: mode is locked to InsertMode
    }
}
```

**OOP Patterns:**
- **State** -- The `Editor` context delegates to `EditorMode` for key handling. The sealed interface makes mode dispatch exhaustive and type-safe.

---

### `renderer/` -- Export Formats (Strategy + Decorator)

Renderers convert the AST into different output formats. The Strategy pattern allows choosing a renderer at runtime. The Decorator pattern layers styling on top.

| Class | Type | Purpose |
|-------|------|---------|
| `Renderer` | Interface | `String render(MarkdownNode root)` -- strategy interface. |
| `AnsiRenderer` | Class | Renders AST to ANSI-styled terminal text (used for status/debug, NOT for the main preview -- Glamour handles that in Go). |
| `PlainTextRenderer` | Class | Renders AST to unstyled plain text. |
| `HtmlRenderer` | Class | Renders AST to HTML. |
| `StyleDecorator` | Abstract Class | Wraps a `Renderer`, adds styling behavior (e.g., line numbers, word wrapping). |

**Strategy Interface:**

```java
public interface Renderer {
    String render(MarkdownNode root);
}
```

**Decorator:**

```java
public abstract class StyleDecorator implements Renderer {
    protected final Renderer wrapped;

    protected StyleDecorator(Renderer wrapped) {
        this.wrapped = wrapped;
    }
}

// Example concrete decorator
public class LineNumberDecorator extends StyleDecorator {
    @Override
    public String render(MarkdownNode root) {
        String base = wrapped.render(root);
        // Prepend line numbers to each line
        ...
    }
}
```

**OOP Patterns:**
- **Strategy** -- `Renderer` interface with pluggable implementations.
- **Decorator** -- `StyleDecorator` wraps renderers to add cross-cutting styling concerns.

---

### `event/` -- Event System (Observer Pattern + Generics)

A lightweight, type-safe event bus for decoupling components. When the document changes, the editor doesn't need to know who cares -- it just publishes an event.

| Class | Type | Purpose |
|-------|------|---------|
| `Event` | Abstract Class | Base event with timestamp and source. |
| `EventBus` | Class | Central publish/subscribe hub. Uses generics for type-safe subscriptions. |
| `EventListener<T extends Event>` | Functional Interface | `void onEvent(T event)` -- generic listener. |
| `DocumentChangedEvent` | Class | Published when document content changes. Contains old/new content, affected range. |
| `CursorMovedEvent` | Class | Published when cursor position changes. Contains old/new position. |

**EventBus with Generics:**

```java
public class EventBus {
    private final Map<Class<? extends Event>, List<EventListener<? extends Event>>> listeners
        = new ConcurrentHashMap<>();

    public <T extends Event> void subscribe(Class<T> eventType, EventListener<T> listener) {
        listeners.computeIfAbsent(eventType, k -> new CopyOnWriteArrayList<>()).add(listener);
    }

    @SuppressWarnings("unchecked")
    public <T extends Event> void publish(T event) {
        List<EventListener<? extends Event>> handlers = listeners.get(event.getClass());
        if (handlers != null) {
            for (EventListener<? extends Event> handler : handlers) {
                ((EventListener<T>) handler).onEvent(event);
            }
        }
    }
}
```

**Functional Interface:**

```java
@FunctionalInterface
public interface EventListener<T extends Event> {
    void onEvent(T event);
}
```

This allows lambda subscriptions: `eventBus.subscribe(DocumentChangedEvent.class, e -> refreshPreview(e))`

**OOP Patterns:**
- **Observer** -- Publish/subscribe decoupling via `EventBus`.
- **Generics** -- Type-safe event subscriptions with bounded wildcards.
- **Functional Interface** -- `EventListener<T>` enables lambda expressions.

---

### `rpc/` -- JSON-RPC Server (Adapter Pattern)

Adapts the internal Java API (Editor, DocumentManager) to the JSON-RPC protocol expected by the Go frontend. This is the boundary layer.

| Class | Type | Purpose |
|-------|------|---------|
| `RpcServer` | Class | Reads JSON-RPC messages from stdin, dispatches to handlers, writes responses to stdout. Uses Content-Length framing. |
| `RpcHandler` | Interface | `RpcResponse handle(String method, JsonObject params)` -- handler contract. |
| `DocumentHandler` | Class | Implements `RpcHandler` for `document/*` methods (open, new, save, edit, search, getContent, export). Adapts RPC params to `DocumentManager` calls. |
| `EditorHandler` | Class | Implements `RpcHandler` for `editor/*` methods (changeMode -- currently a no-op). Adapts RPC params to `Editor` calls. |
| `RpcResponse` | Record | `record RpcResponse(Object result, RpcError error)` -- standard response envelope. |

**Adapter Pattern:**

The Go frontend speaks JSON-RPC. The Java internals speak `Editor.processKey()`, `DocumentManager.open()`, etc. The `DocumentHandler` and `EditorHandler` classes **adapt** between these two interfaces:

```java
public class DocumentHandler implements RpcHandler {
    private final DocumentManager documentManager;
    private final Editor editor;

    @Override
    public RpcResponse handle(String method, JsonObject params) {
        return switch (method) {
            case "document/open"       -> handleOpen(params);
            case "document/new"        -> handleNew(params);
            case "document/save"       -> handleSave(params);
            case "document/edit"       -> handleEdit(params);
            case "document/search"     -> handleSearch(params);
            case "document/getContent" -> handleGetContent(params);
            case "document/export"     -> handleExport(params);
            default -> new RpcResponse(null, RpcError.methodNotFound(method));
        };
    }
}
```

**OOP Patterns:**
- **Adapter** -- Converts JSON-RPC protocol to internal Java API calls.
- **Records** -- `RpcResponse` as an immutable response envelope.

---

### `config/` -- Configuration (Builder + Strategy)

| Class | Type | Purpose |
|-------|------|---------|
| `EditorConfig` | Class | Holds all editor settings: tab size, auto-indent, word wrap width, theme, key binding scheme. Built via a Builder with sensible defaults. |
| `KeyBindingScheme` | Interface | Strategy interface for key binding configurations. Allows swapping between vim/emacs/custom key maps. |

**Config Builder:**

```java
public class EditorConfig {
    private EditorConfig(Builder builder) { ... }

    public static class Builder {
        private int tabSize = 4;           // default
        private boolean autoIndent = true; // default
        private int wrapWidth = 80;        // default

        public Builder tabSize(int size) { ... }
        public Builder autoIndent(boolean enabled) { ... }
        public Builder wrapWidth(int width) { ... }
        public Builder keyBindings(KeyBindingScheme scheme) { ... }
        public EditorConfig build() { ... }
    }
}
```

**OOP Patterns:**
- **Builder** -- Step-by-step configuration with defaults and validation.
- **Strategy** -- `KeyBindingScheme` for pluggable key binding configurations.

---

### `exception/` -- Custom Exception Hierarchy

A clean exception hierarchy for domain-specific errors. All extend from a common base for uniform handling.

```
CharMedException (abstract, extends RuntimeException)
├── ParseException          -- Malformed markdown, unexpected tokens
├── RenderException         -- Rendering failures
└── DocumentIOException     -- File read/write failures
```

```java
public abstract class CharMedException extends RuntimeException {
    private final String errorCode;

    protected CharMedException(String message, String errorCode) {
        super(message);
        this.errorCode = errorCode;
    }

    protected CharMedException(String message, String errorCode, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }

    public String errorCode() { return errorCode; }
}
```

Each subclass adds context-specific fields:

```java
public class ParseException extends CharMedException {
    private final int line;
    private final int column;
    // ...
}
```

The `errorCode` maps directly to JSON-RPC error codes, allowing the RPC layer to translate Java exceptions into protocol-compliant error responses.

---

## Dependencies

| Dependency | Group ID | Purpose |
|------------|----------|---------|
| Gson | `com.google.code.gson:gson:2.11.0` | JSON serialization/deserialization for RPC messages |

Content-Length framing is implemented manually in `RpcServer` (~50 lines). No heavy frameworks (Spring, lsp4j, etc.) -- intentional to keep the OOP patterns explicit.

---

## Java 21+ Features Used

| Feature | Where | Why |
|---------|-------|-----|
| Sealed interfaces | `MarkdownNode`, `EditorMode` | Exhaustive pattern matching, closed hierarchies |
| Records | `CursorPosition`, `SelectionRange`, `Token`, `RpcResponse` | Immutable data carriers |
| Pattern matching `switch` | Visitors, RPC handlers | Concise, exhaustive, type-safe branching |
| Generics with bounds | `EventListener<T extends Event>`, `EventBus` | Type-safe event subscriptions and callbacks |
| Functional interfaces | `EventListener<T>` | Lambda-friendly observer callbacks |
| Enhanced enums | `NodeType`, `TokenType` | Behavior-carrying enumerations |

---

## See Also

- [ARCHITECTURE.md](ARCHITECTURE.md) -- System overview
- [FRONTEND.md](FRONTEND.md) -- Go frontend design
- [PROTOCOL.md](PROTOCOL.md) -- JSON-RPC protocol specification
- [OOP_PATTERNS.md](OOP_PATTERNS.md) -- Design patterns and Java features in detail
