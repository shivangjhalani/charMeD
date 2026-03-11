package ui

import (
	"fmt"
	"strings"

	"github.com/charmbracelet/lipgloss"
)

// StatusBar shows mode, filename, cursor, word count, and dirty indicator.
type StatusBar struct {
	mode      string
	filePath  string
	cursor    CursorPos
	wordCount int
	lineCount int
	dirty     bool
	message   string
	width     int
}

// NewStatusBar creates a new status bar.
func NewStatusBar() StatusBar {
	return StatusBar{
		mode: "NORMAL",
	}
}

// SetWidth sets the status bar width.
func (s *StatusBar) SetWidth(w int) {
	s.width = w
}

// Update updates the status bar state.
func (s *StatusBar) Update(mode, filePath string, cursor CursorPos,
	wordCount, lineCount int, dirty bool) {
	s.mode = mode
	s.filePath = filePath
	s.cursor = cursor
	s.wordCount = wordCount
	s.lineCount = lineCount
	s.dirty = dirty
}

// SetMessage sets a temporary message.
func (s *StatusBar) SetMessage(msg string) {
	s.message = msg
}

// ClearMessage clears the temporary message.
func (s *StatusBar) ClearMessage() {
	s.message = ""
}

// View renders the status bar.
func (s StatusBar) View() string {
	// Mode indicator
	modeStyle := statusModeStyle
	switch strings.ToUpper(s.mode) {
	case "INSERT":
		modeStyle = statusInsertModeStyle
	case "COMMAND":
		modeStyle = statusCommandModeStyle
	}
	mode := modeStyle.Render(fmt.Sprintf(" %s ", strings.ToUpper(s.mode)))

	// File path
	file := s.filePath
	if file == "" {
		file = "[No File]"
	}
	if s.dirty {
		file += " ●"
	}
	fileSection := statusFileStyle.Render(fmt.Sprintf(" %s ", file))

	// Cursor position
	cursorSection := statusCursorStyle.Render(
		fmt.Sprintf(" Ln %d, Col %d ", s.cursor.Line+1, s.cursor.Column+1))

	// Word count
	statsSection := statusCursorStyle.Render(
		fmt.Sprintf(" %d words │ %d lines ", s.wordCount, s.lineCount))

	// Message or gap
	usedWidth := lipgloss.Width(mode) + lipgloss.Width(fileSection) +
		lipgloss.Width(cursorSection) + lipgloss.Width(statsSection)
	gapWidth := s.width - usedWidth
	if gapWidth < 0 {
		gapWidth = 0
	}

	var gap string
	if s.message != "" {
		msgText := messageStyle.Render(fmt.Sprintf(" %s ", s.message))
		remaining := gapWidth - lipgloss.Width(msgText)
		if remaining < 0 {
			remaining = 0
		}
		gap = msgText + statusBarStyle.Render(strings.Repeat(" ", remaining))
	} else {
		gap = statusBarStyle.Render(strings.Repeat(" ", gapWidth))
	}

	return lipgloss.JoinHorizontal(lipgloss.Top,
		mode, fileSection, gap, cursorSection, statsSection)
}
