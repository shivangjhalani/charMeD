# charMeD OOP Patterns & Java Features

This document maps every design pattern and Java 21+ feature used in charMeD, explains **where** it's used, and justifies **why** it's a natural fit -- not forced.

---

## Design Patterns

### 1. Composite Pattern

**Where:** `model/` -- The markdown AST (Abstract Syntax Tree).

**What:** Composite lets you treat individual objects and compositions of objects uniformly. A `DocumentNode` (container) and a `TextNode` (leaf) both implement `MarkdownNode`. You can call `children()` on any node -- containers return their children, leaves return an empty list.

**Why it's natural:** Markdown IS a tree. A document contains headings, paragraphs, lists. A list contains list items. A paragraph contains bold, italic, text spans. The Composite pattern is the textbook way to represent tree structures where clients don't need to distinguish between branches and leaves.

**Classes:**
- `MarkdownNode` (sealed interface) -- the component
- `DocumentNode`, `HeadingNode`, `ParagraphNode`, `ListNode`, `ListItemNode`, `BlockquoteNode`, `BoldNode`, `ItalicNode` -- composites (have children)
- `TextNode`, `CodeBlockNode`, `InlineCodeNode`, `LinkNode`, `HorizontalRuleNode` -- leaves

**Example:**

```java
// Uniform traversal -- works for any node, container or leaf
public void printTree(MarkdownNode node, int depth) {
    System.out.println("  ".repeat(depth) + node.nodeType());
    for (MarkdownNode child : node.children()) {
        printTree(child, depth + 1);
    }
}
```

---

### 2. Factory Pattern

**Where:** `parser/NodeFactory` -- Creates AST nodes during parsing.

**What:** Factory centralizes object creation, decoupling the parser from concrete node constructors. The parser says "make me a heading" without knowing the constructor signature.

**Why it's natural:** The parser produces many different node types based on tokens. Scattering `new HeadingNode(...)` throughout the parser couples it to every concrete class. The factory is the single point of change if we modify node construction (add validation, add metadata, change constructors).

**Classes:**
- `NodeFactory` -- factory class with one method per node type

**Example:**

```java
// Parser uses factory instead of direct construction
MarkdownNode heading = factory.createHeading(level, inlineChildren);
MarkdownNode code = factory.createCodeBlock(language, content);
```

---

### 3. Strategy Pattern

**Where:** Three independent uses:
1. `parser/Parser` interface -- pluggable parsing implementations
2. `renderer/Renderer` interface -- pluggable export formats
3. `config/KeyBindingScheme` interface -- pluggable key binding configurations

**What:** Strategy defines a family of algorithms, encapsulates each one, and makes them interchangeable. The client code depends on the interface, not the implementation.

**Why it's natural:**
- **Parsers:** We might want a strict CommonMark parser vs a lenient one. Same interface, different behavior.
- **Renderers:** Exporting to HTML, plain text, or markdown are clearly different strategies for the same operation ("render this AST to a string").
- **Key bindings:** Vim vs Emacs key mappings are different strategies for "what does this key do?"

**Classes:**
- `Parser` → `MarkdownParser` (+ future `StrictParser`, etc.)
- `Renderer` → `AnsiRenderer`, `PlainTextRenderer`, `HtmlRenderer`
- `KeyBindingScheme` → vim bindings (default), potentially emacs bindings

**Example:**

```java
// Swap renderer at runtime without changing any other code
Renderer renderer = switch (format) {
    case "html"      -> new HtmlRenderer();
    case "plaintext" -> new PlainTextRenderer();
    case "markdown"  -> new MarkdownExportVisitor(); // visitor also works
};
String output = renderer.render(document.getAst());
```

---

### 4. Command Pattern

**Where:** `command/` -- Every document mutation (insert, delete, format, replace).

**What:** Command encapsulates a request as an object, allowing parameterization, queuing, and -- critically -- **undo/redo**. Each command stores enough state to reverse itself.

**Why it's natural:** Undo/redo is the killer use case for the Command pattern. A markdown editor needs undo. Every edit must be reversible. Wrapping each edit in a command object with `execute()` and `undo()` is the canonical solution.

**Classes:**
- `EditorCommand` (interface) -- `execute()`, `undo()`, `description()`
- `InsertTextCommand` -- stores inserted text and position, undo removes it
- `DeleteTextCommand` -- stores deleted text and range, undo re-inserts it
- `FormatBoldCommand` -- stores selection range, wraps in `**`, undo removes markers
- `FormatItalicCommand` -- stores selection range, wraps in `*`, undo removes markers
- `ReplaceTextCommand` -- stores old and new text, undo restores old
- `CommandHistory<T>` -- manages undo/redo stacks

**Example:**

```java
// Execute and track for undo
EditorCommand cmd = new InsertTextCommand(document, position, "hello");
history.execute(cmd);  // calls cmd.execute(), pushes to undo stack

// Undo
history.undo();  // pops from undo, calls cmd.undo(), pushes to redo
```

---

### 5. Visitor Pattern

**Where:** `visitor/` -- All operations that traverse the AST.

**What:** Visitor lets you define new operations on a structure without modifying the structure's classes. Each visitor implements a `visit()` method per node type. Nodes accept visitors via double dispatch.

**Why it's natural:** The AST node types are **sealed** -- they're fixed and change rarely (markdown syntax doesn't change). But operations on the AST change frequently: we need word count, search, HTML export, markdown re-serialization, plain text extraction. Each new operation is a new Visitor class, requiring zero changes to any node class.

This is the textbook scenario where Visitor shines. If the node types changed frequently, Visitor would be painful (every visitor breaks). But sealed types guarantee stability.

**Classes:**
- `NodeVisitor` (interface) -- one `visit(XxxNode)` per node type
- `PlainTextVisitor` -- extracts all text content as a flat string
- `HtmlExportVisitor` -- produces HTML output
- `MarkdownExportVisitor` -- re-serializes to markdown
- `WordCountVisitor` -- counts words across text nodes
- `SearchVisitor` -- finds occurrences of a search term, returns positions

**Example:**

```java
// Count words without touching any node class
WordCountVisitor counter = new WordCountVisitor();
document.getAst().accept(counter);
int words = counter.getCount();

// Export to HTML without touching any node class
HtmlExportVisitor exporter = new HtmlExportVisitor();
document.getAst().accept(exporter);
String html = exporter.getOutput();
```

---

### 6. Observer Pattern

**Where:** `event/` -- The EventBus system.

**What:** Observer defines a one-to-many dependency. When a subject changes state, all dependents (observers) are notified. In charMeD, the `EventBus` is the central publish/subscribe mechanism.

**Why it's natural:** Multiple components need to react to the same events:
- Document changes → need to re-render preview, update word count, update dirty flag
- Cursor moves → need to update status bar
- Mode changes → need to update status bar, change key handling

Without Observer, the `DocumentManager` would need to know about every consumer. With Observer, it just publishes events -- whoever cares subscribes.

**Classes:**
- `Event` (abstract) -- base event with timestamp
- `EventBus` -- publish/subscribe hub
- `EventListener<T extends Event>` -- functional interface for handlers
- `DocumentChangedEvent`, `CursorMovedEvent`, `ModeChangedEvent` -- concrete events

**Example:**

```java
// Subscribe with a lambda (functional interface)
eventBus.subscribe(DocumentChangedEvent.class, event -> {
    sendRefreshNotification(event);
});

// Publish -- all subscribers are notified
eventBus.publish(new DocumentChangedEvent(oldContent, newContent, affectedRange));
```

---

### 7. State Pattern

**Where:** `editor/` -- The editor mode system (Normal, Insert, Command).

**What:** State allows an object to alter its behavior when its internal state changes. The object appears to change its class. Each state is a separate class with its own behavior for the same operations.

**Why it's natural:** A vim-like editor has modes. In Normal mode, `j` moves the cursor down. In Insert mode, `j` types the letter "j". In Command mode, `j` is part of a command string. The same input has **completely different behavior** depending on the mode. This is exactly what State pattern is for.

**Classes:**
- `EditorMode` (sealed interface) -- `handleKey()`, `modeName()`
- `NormalMode` -- navigation, mode switching, undo/redo
- `InsertMode` -- text input, `Esc` returns to Normal
- `CommandMode` -- command input, `Enter` executes, `Esc` returns to Normal
- `Editor` -- the context, delegates to current mode

**Example:**

```java
// Same method, different behavior depending on state
public HandleResult processKey(String key) {
    return currentMode.handleKey(this, key);  // delegates to NormalMode, InsertMode, or CommandMode
}

// Mode transition
public void transitionTo(EditorMode newMode) {
    this.currentMode = newMode;
    eventBus.publish(new ModeChangedEvent(newMode.modeName()));
}
```

**Sealed + Pattern Matching:**

```java
// Exhaustive switch -- compiler ensures all modes are handled
String indicator = switch (currentMode) {
    case NormalMode n  -> "NORMAL";
    case InsertMode i  -> "INSERT";
    case CommandMode c -> "COMMAND";
};
```

---

### 8. Builder Pattern

**Where:** Two uses:
1. `document/Document.Builder` -- constructing Document objects
2. `config/EditorConfig.Builder` -- constructing configuration

**What:** Builder separates object construction from representation, allowing step-by-step construction with validation.

**Why it's natural:**
- `Document` has many optional fields (file path, initial cursor, content). A telescoping constructor would be unreadable. Builder gives named, chainable setters.
- `EditorConfig` has many settings with sensible defaults. Builder lets you override only what you care about.

**Example:**

```java
Document doc = new Document.Builder()
    .filePath(Path.of("README.md"))
    .content("# Hello\n\nWorld")
    .cursor(new CursorPosition(0, 0))
    .build();

EditorConfig config = new EditorConfig.Builder()
    .tabSize(2)
    .wrapWidth(100)
    .build();  // everything else uses defaults
```

---

### 9. Decorator Pattern

**Where:** `renderer/StyleDecorator` -- layering styling on top of renderers.

**What:** Decorator attaches additional responsibilities to an object dynamically. Decorators wrap the original object and add behavior before/after delegation.

**Why it's natural:** A renderer produces output. Sometimes you want line numbers. Sometimes you want word wrapping. Sometimes both. These are orthogonal concerns. Rather than creating `HtmlRendererWithLineNumbers`, `HtmlRendererWithWrapping`, `HtmlRendererWithLineNumbersAndWrapping` (combinatorial explosion), you stack decorators.

**Classes:**
- `StyleDecorator` (abstract) -- wraps a `Renderer`, implements `Renderer`
- `LineNumberDecorator extends StyleDecorator`
- Future: `WordWrapDecorator`, `SyntaxHighlightDecorator`, etc.

**Example:**

```java
Renderer base = new PlainTextRenderer();
Renderer withLineNumbers = new LineNumberDecorator(base);
// withLineNumbers.render(ast) adds line numbers to plain text output
```

---

### 10. Adapter Pattern

**Where:** `rpc/DocumentHandler`, `rpc/EditorHandler` -- adapting JSON-RPC to internal API.

**What:** Adapter converts the interface of a class into another interface clients expect. It lets classes work together that couldn't otherwise because of incompatible interfaces.

**Why it's natural:** The Go frontend speaks JSON-RPC (method strings, JSON params). The Java internals speak Java APIs (`editor.processKey()`, `documentManager.open()`). The RPC handlers **adapt** between these two interfaces. This is a classic boundary adapter.

**Classes:**
- `DocumentHandler implements RpcHandler` -- adapts `document/*` RPC methods to `DocumentManager` calls
- `EditorHandler implements RpcHandler` -- adapts `editor/*` RPC methods to `Editor` calls

**Example:**

```java
// RPC handler adapts JSON params to Java method calls
case "document/open" -> {
    String path = params.get("path").getAsString();
    Document doc = documentManager.open(Path.of(path));
    return new RpcResponse(serializeDocument(doc), null);
}
```

---

### 11. Template Method Pattern

**Where:** `model/` -- Default implementations in the node hierarchy.

**What:** Template Method defines the skeleton of an algorithm in a base class, letting subclasses override specific steps without changing the algorithm's structure.

**Why it's natural:** Many node operations follow the same pattern: "process this node, then recursively process children." The base behavior (recursive traversal) is defined once; specific nodes override the "process this node" step.

**Example:**

```java
// Default implementation in a base helper or via interface default method
default String rawText() {
    // Template: concatenate rawText of all children
    return children().stream()
        .map(MarkdownNode::rawText)
        .collect(Collectors.joining());
}

// TextNode overrides -- it's a leaf with actual text
@Override
public String rawText() {
    return this.content;  // no children, just return text
}
```

---

### 12. MVC (Model-View-Controller)

**Where:** The entire two-process architecture.

**What:** MVC separates an application into three concerns: Model (data/logic), View (presentation), Controller (input handling/coordination).

**Why it's natural:** The two-process architecture IS MVC:
- **Model:** Java backend -- Document, AST, Parser, CommandHistory, EventBus
- **View:** Go frontend -- Bubble Tea rendering, Lip Gloss styling, Glamour preview, Bubbles components
- **Controller:** Split between Go (captures input, translates to RPC) and Java (RPC handlers dispatch to model)

This isn't forced -- it's the inherent structure. The JSON-RPC protocol is the boundary between View and Model/Controller.

---

## Summary Table

| # | Pattern | Package | Key Classes | Motivation |
|---|---------|---------|-------------|------------|
| 1 | Composite | `model/` | `MarkdownNode`, all node types | Markdown is a tree |
| 2 | Factory | `parser/` | `NodeFactory` | Decouple parser from node constructors |
| 3 | Strategy | `parser/`, `renderer/`, `config/` | `Parser`, `Renderer`, `KeyBindingScheme` | Pluggable algorithms |
| 4 | Command | `command/` | `EditorCommand`, `CommandHistory` | Undo/redo requires reversible operations |
| 5 | Visitor | `visitor/` | `NodeVisitor`, 5 concrete visitors | Extensible AST operations on sealed types |
| 6 | Observer | `event/` | `EventBus`, `EventListener`, events | Decouple state changes from reactions |
| 7 | State | `editor/` | `EditorMode`, 3 mode classes | Vim-like modes with different key behaviors |
| 8 | Builder | `document/`, `config/` | `Document.Builder`, `EditorConfig.Builder` | Complex construction with defaults |
| 9 | Decorator | `renderer/` | `StyleDecorator` | Composable rendering enhancements |
| 10 | Adapter | `rpc/` | `DocumentHandler`, `EditorHandler` | JSON-RPC ↔ Java API boundary |
| 11 | Template Method | `model/` | Default `rawText()` in nodes | Common traversal with specialized steps |
| 12 | MVC | Architecture | Entire system | Two-process = View + Model/Controller |

---

## Java 21+ Features

### Sealed Interfaces

**Where:** `MarkdownNode`, `EditorMode`

**What:** `sealed` restricts which classes can implement an interface. Combined with pattern matching `switch`, the compiler guarantees exhaustiveness.

```java
public sealed interface MarkdownNode
    permits DocumentNode, HeadingNode, ParagraphNode, ... { }

// Exhaustive switch -- compiler error if a type is missing
String desc = switch (node) {
    case DocumentNode d    -> "document";
    case HeadingNode h     -> "heading level " + h.level();
    case ParagraphNode p   -> "paragraph";
    case CodeBlockNode c   -> "code: " + c.language();
    // ... all types must be covered, no default needed
};
```

**Why:** `MarkdownNode` has a fixed set of types (markdown syntax doesn't change). Sealing prevents rogue implementations and enables the compiler to verify exhaustive handling. `EditorMode` has exactly 3 modes -- sealing encodes this constraint in the type system.

---

### Records

**Where:** `CursorPosition`, `SelectionRange`, `Token`, `RpcResponse`

**What:** Records are immutable data carriers with auto-generated `equals()`, `hashCode()`, `toString()`, and accessor methods.

```java
public record CursorPosition(int line, int column) { }
public record Token(TokenType type, String value, int line, int column) { }
public record RpcResponse(Object result, RpcError error) { }
```

**Why:** These are pure data -- no behavior beyond carrying values. Records enforce immutability and eliminate boilerplate. A `CursorPosition` is a value: two positions at the same line and column are equal, period.

---

### Pattern Matching with `switch`

**Where:** RPC handlers, visitor dispatch, mode-dependent logic

```java
// Pattern matching on sealed types
return switch (node) {
    case HeadingNode h when h.level() == 1 -> renderH1(h);
    case HeadingNode h                     -> renderHn(h);
    case ParagraphNode p                   -> renderParagraph(p);
    case CodeBlockNode c                   -> renderCode(c);
    // ...
};

// Pattern matching with guards
return switch (mode) {
    case NormalMode n  -> handleNormalKey(n, key);
    case InsertMode i  -> handleInsertKey(i, key);
    case CommandMode c -> handleCommandKey(c, key);
};
```

**Why:** Eliminates `instanceof` chains and casts. Combined with sealed types, provides exhaustive, type-safe branching that's concise and readable.

---

### Generics with Bounded Type Parameters

**Where:** `CommandHistory<T extends EditorCommand>`, `EventBus`, `EventListener<T extends Event>`

```java
public class CommandHistory<T extends EditorCommand> {
    private final Deque<T> undoStack = new ArrayDeque<>();
    // T is bounded -- only EditorCommand subtypes allowed
}

public <T extends Event> void subscribe(Class<T> type, EventListener<T> listener) {
    // Type-safe subscription: listener type matches event type
}
```

**Why:** Generics provide compile-time type safety for collections and callbacks. The bound `T extends EditorCommand` ensures the history only holds commands, not arbitrary objects. The EventBus uses generics with wildcards to maintain type safety across event types.

---

### Functional Interfaces + Lambdas

**Where:** `EventListener<T>`, command execution callbacks

```java
@FunctionalInterface
public interface EventListener<T extends Event> {
    void onEvent(T event);
}

// Used with lambdas
eventBus.subscribe(DocumentChangedEvent.class, e -> refreshPreview(e));
eventBus.subscribe(ModeChangedEvent.class, e -> updateStatusBar(e.modeName()));
```

**Why:** The Observer pattern naturally produces single-method interfaces. `@FunctionalInterface` makes them lambda-compatible, yielding concise subscription code without anonymous inner class boilerplate.

---

### Virtual Threads (Project Loom)

**Where:** `RpcServer` -- handling concurrent message processing

```java
Thread.startVirtualThread(() -> {
    processNotification(notification);
});
```

**Why:** Virtual threads are lightweight -- millions can run concurrently without OS thread overhead. The RPC server can handle notifications asynchronously (e.g., background re-parsing after an edit) without blocking the main message loop. This is more natural than explicit thread pool management.

---

### Enhanced Enums with Behavior

**Where:** `NodeType`, `TokenType`

```java
public enum NodeType {
    DOCUMENT("Document", true),
    HEADING("Heading", true),
    TEXT("Text", false),
    // ...
    ;

    private final String displayName;
    private final boolean isContainer;

    NodeType(String displayName, boolean isContainer) {
        this.displayName = displayName;
        this.isContainer = isContainer;
    }

    public String displayName() { return displayName; }
    public boolean isContainer() { return isContainer; }
}
```

**Why:** Enums with fields and methods are more than just constants -- they carry behavior. `NodeType.HEADING.isContainer()` is more expressive and type-safe than checking a separate map or using magic booleans.

---

### Optional

**Where:** `CommandHistory.undo()`, `CommandHistory.redo()`

```java
public Optional<T> undo() {
    if (undoStack.isEmpty()) return Optional.empty();
    T command = undoStack.pop();
    command.undo();
    redoStack.push(command);
    return Optional.of(command);
}
```

**Why:** Returning `null` when there's nothing to undo is a bug magnet. `Optional<T>` makes the "maybe nothing" case explicit and forces callers to handle it.

---

## Four OOP Pillars

### 1. Encapsulation

- `Document` has private fields, accessed only through methods. Internal representation (list of lines) is hidden; consumers work through `getContent()`, `setCursor()`, etc.
- `CommandHistory` hides its undo/redo stacks behind `execute()`, `undo()`, `redo()`.
- `EventBus` hides its listener map behind `subscribe()` and `publish()`.

### 2. Inheritance

- `CharMedException` → `ParseException`, `RenderException`, `DocumentIOException`, `CommandExecutionException` -- exception hierarchy sharing common error code behavior.
- `StyleDecorator extends Object implements Renderer` -- decorators inherit the Renderer contract.
- `Event` → `DocumentChangedEvent`, `CursorMovedEvent`, `ModeChangedEvent` -- event hierarchy sharing timestamp and source.

### 3. Polymorphism

- `MarkdownNode` -- any node can be treated uniformly. `accept(visitor)` dispatches to the right `visit()` method via double dispatch.
- `Renderer` -- `HtmlRenderer`, `PlainTextRenderer`, `AnsiRenderer` all implement `render()` differently.
- `EditorMode` -- `NormalMode`, `InsertMode`, `CommandMode` all implement `handleKey()` differently.
- `EditorCommand` -- `InsertTextCommand`, `DeleteTextCommand`, etc. all implement `execute()` and `undo()` differently.

### 4. Abstraction

- `Parser` interface hides parsing implementation details -- callers only know `parse(String) → MarkdownNode`.
- `Renderer` interface hides output format details -- callers only know `render(MarkdownNode) → String`.
- `EditorCommand` interface hides mutation details -- the history only knows `execute()` and `undo()`.
- `EventListener<T>` hides handler implementation -- the EventBus only knows `onEvent(T)`.

---

## SOLID Principles

| Principle | How charMeD Follows It |
|-----------|----------------------|
| **S**ingle Responsibility | Each class has one reason to change. `Parser` only parses. `WordCountVisitor` only counts words. `RpcServer` only handles protocol framing. `DocumentHandler` only adapts RPC to document operations. |
| **O**pen/Closed | Open for extension via Visitor (add new AST operations), Strategy (add new renderers/parsers), Observer (add new event subscribers). Closed for modification -- none of these require changing existing classes. |
| **L**iskov Substitution | Any `MarkdownNode` can be used where `MarkdownNode` is expected. Any `Renderer` can be used where `Renderer` is expected. Subtypes don't weaken preconditions or strengthen postconditions. |
| **I**nterface Segregation | Small, focused interfaces: `Parser` (1 method), `Renderer` (1 method), `EditorCommand` (3 methods), `EventListener` (1 method), `RpcHandler` (1 method). No fat interfaces. |
| **D**ependency Inversion | `Editor` depends on `EditorMode` (interface), not `NormalMode` (concrete). `CommandHistory` depends on `EditorCommand` (interface), not `InsertTextCommand` (concrete). High-level modules depend on abstractions. |

---

## See Also

- [ARCHITECTURE.md](ARCHITECTURE.md) -- System overview
- [BACKEND.md](BACKEND.md) -- Java backend design
- [FRONTEND.md](FRONTEND.md) -- Go frontend design
- [PROTOCOL.md](PROTOCOL.md) -- JSON-RPC protocol specification
