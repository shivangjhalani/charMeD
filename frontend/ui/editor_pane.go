package ui

import (
	"fmt"
	"strings"

	"github.com/charmbracelet/bubbles/textarea"
	tea "github.com/charmbracelet/bubbletea"
)

// EditorPane wraps bubbles/textarea for the raw markdown editor.
type EditorPane struct {
	textarea textarea.Model
	content  string
	lines    []string
	cursor   CursorPos
	focused  bool
}

// CursorPos mirrors the backend's CursorPosition.
type CursorPos struct {
	Line   int `json:"line"`
	Column int `json:"column"`
}

// NewEditorPane creates a new editor pane.
func NewEditorPane() EditorPane {
	ta := textarea.New()
	ta.Placeholder = "Start typing markdown..."
	ta.ShowLineNumbers = true
	ta.CharLimit = 0 // no limit
	ta.SetWidth(40)
	ta.SetHeight(20)
	// Do NOT call ta.Focus() here — textarea must stay blurred in NORMAL mode.
	// Focus is managed exclusively by INSERT/NORMAL mode transitions in model.go.

	return EditorPane{
		textarea: ta,
		focused:  true,
		lines:    []string{""},
		cursor:   CursorPos{0, 0},
	}
}

// SetSize resizes the editor pane.
func (e *EditorPane) SetSize(w, h int) {
	e.textarea.SetWidth(w - 2)  // account for border
	e.textarea.SetHeight(h - 2)
}

// SetContent updates the editor with new content.
func (e *EditorPane) SetContent(content string) {
	e.content = content
	e.lines = strings.Split(content, "\n")
	e.textarea.SetValue(content)
}

// SetFocused sets the focus state.
func (e *EditorPane) SetFocused(focused bool) {
	e.focused = focused
	// Only controls border styling. Textarea focus is managed by mode transitions.
}

// Update handles messages.
func (e *EditorPane) Update(msg tea.Msg) tea.Cmd {
	var cmd tea.Cmd
	e.textarea, cmd = e.textarea.Update(msg)
	return cmd
}

// View renders the editor pane.
func (e EditorPane) View(width, height int) string {
	style := editorBorderStyle
	if e.focused {
		style = editorActiveBorderStyle
	}

	header := titleStyle.Render("📝 Editor")
	body := e.textarea.View()

	content := fmt.Sprintf("%s\n%s", header, body)
	return style.Width(width - 2).Height(height - 2).Render(content)
}
