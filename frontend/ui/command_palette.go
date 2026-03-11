package ui

import (
	"github.com/charmbracelet/bubbles/textinput"
	tea "github.com/charmbracelet/bubbletea"
)

// CommandPalette is the : command input overlay.
type CommandPalette struct {
	input   textinput.Model
	visible bool
}

// CommandExecuteMsg is sent when a command is entered.
type CommandExecuteMsg struct {
	Command string
}

// NewCommandPalette creates a new command palette.
func NewCommandPalette() CommandPalette {
	ti := textinput.New()
	ti.Placeholder = "Enter command..."
	ti.Prompt = ": "
	ti.CharLimit = 256
	ti.Width = 46

	return CommandPalette{
		input: ti,
	}
}

// Show makes the command palette visible and focuses it.
func (c *CommandPalette) Show() {
	c.visible = true
	c.input.SetValue("")
	c.input.Focus()
}

// Hide hides the command palette.
func (c *CommandPalette) Hide() {
	c.visible = false
	c.input.Blur()
}

// IsVisible returns visibility state.
func (c CommandPalette) IsVisible() bool {
	return c.visible
}

// Update handles messages.
func (c *CommandPalette) Update(msg tea.Msg) (tea.Cmd, bool) {
	if !c.visible {
		return nil, false
	}

	switch msg := msg.(type) {
	case tea.KeyMsg:
		switch msg.String() {
		case "esc":
			c.Hide()
			return nil, true
		case "enter":
			cmd := c.input.Value()
			c.Hide()
			return func() tea.Msg {
				return CommandExecuteMsg{Command: cmd}
			}, true
		}
	}

	var cmd tea.Cmd
	c.input, cmd = c.input.Update(msg)
	return cmd, true
}

// View renders the command palette.
func (c CommandPalette) View() string {
	if !c.visible {
		return ""
	}
	return commandPaletteStyle.Render(c.input.View())
}
