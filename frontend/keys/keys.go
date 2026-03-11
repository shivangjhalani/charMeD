package keys

import "github.com/charmbracelet/bubbles/key"

// KeyMap holds all application key bindings.
type KeyMap struct {
	// Global
	Quit     key.Binding
	ForceQuit key.Binding
	Help     key.Binding

	// Pane navigation
	SwitchPane  key.Binding
	ToggleTree  key.Binding

	// Editor (normal mode)
	InsertMode  key.Binding
	CommandMode key.Binding

	// Scrolling
	ScrollUp   key.Binding
	ScrollDown key.Binding
	PageUp     key.Binding
	PageDown   key.Binding
}

// DefaultKeyMap returns the default key bindings.
func DefaultKeyMap() KeyMap {
	return KeyMap{
		Quit:     key.NewBinding(key.WithKeys("q"), key.WithHelp("q", "quit")),
		ForceQuit: key.NewBinding(key.WithKeys("ctrl+c"), key.WithHelp("ctrl+c", "force quit")),
		Help:     key.NewBinding(key.WithKeys("?"), key.WithHelp("?", "help")),

		SwitchPane:  key.NewBinding(key.WithKeys("tab"), key.WithHelp("tab", "switch pane")),
		ToggleTree:  key.NewBinding(key.WithKeys("ctrl+b"), key.WithHelp("ctrl+b", "file tree")),

		InsertMode:  key.NewBinding(key.WithKeys("i"), key.WithHelp("i", "insert mode")),
		CommandMode: key.NewBinding(key.WithKeys(":"), key.WithHelp(":", "command mode")),

		ScrollUp:   key.NewBinding(key.WithKeys("k", "up"), key.WithHelp("k/↑", "scroll up")),
		ScrollDown: key.NewBinding(key.WithKeys("j", "down"), key.WithHelp("j/↓", "scroll down")),
		PageUp:     key.NewBinding(key.WithKeys("pgup", "ctrl+u"), key.WithHelp("pgup", "page up")),
		PageDown:   key.NewBinding(key.WithKeys("pgdown", "ctrl+d"), key.WithHelp("pgdn", "page down")),
	}
}
