package ui

import "github.com/charmbracelet/lipgloss"

// Catppuccin Mocha-inspired palette.
var (
	// Base colors
	colorBase     = lipgloss.Color("#1e1e2e")
	colorSurface0 = lipgloss.Color("#313244")
	colorSurface1 = lipgloss.Color("#45475a")
	colorOverlay0 = lipgloss.Color("#6c7086")
	colorText     = lipgloss.Color("#cdd6f4")
	colorSubtext0 = lipgloss.Color("#a6adc8")
	colorSubtext1 = lipgloss.Color("#bac2de")

	// Accent colors
	colorBlue    = lipgloss.Color("#89b4fa")
	colorGreen   = lipgloss.Color("#a6e3a1")
	colorPeach   = lipgloss.Color("#fab387")
	colorRed     = lipgloss.Color("#f38ba8")
	colorMauve   = lipgloss.Color("#cba6f7")
	colorYellow  = lipgloss.Color("#f9e2af")
	colorTeal    = lipgloss.Color("#94e2d5")
	colorLavender = lipgloss.Color("#b4befe")

	// Styles
	titleStyle = lipgloss.NewStyle().
			Foreground(colorMauve).
			Bold(true).
			Padding(0, 1)

	statusBarStyle = lipgloss.NewStyle().
			Background(colorSurface0).
			Foreground(colorText).
			Padding(0, 1)

	statusFileStyle = lipgloss.NewStyle().
			Background(colorSurface1).
			Foreground(colorSubtext1).
			Padding(0, 1)

	statusCursorStyle = lipgloss.NewStyle().
			Background(colorSurface0).
			Foreground(colorSubtext0).
			Padding(0, 1)

	editorBorderStyle = lipgloss.NewStyle().
			Border(lipgloss.RoundedBorder()).
			BorderForeground(colorSurface1).
			Padding(0, 1)

	editorActiveBorderStyle = lipgloss.NewStyle().
			Border(lipgloss.RoundedBorder()).
			BorderForeground(colorBlue).
			Padding(0, 1)

	previewBorderStyle = lipgloss.NewStyle().
			Border(lipgloss.RoundedBorder()).
			BorderForeground(colorSurface1).
			Padding(0, 1)

	previewActiveBorderStyle = lipgloss.NewStyle().
			Border(lipgloss.RoundedBorder()).
			BorderForeground(colorTeal).
			Padding(0, 1)

	commandPaletteStyle = lipgloss.NewStyle().
			Border(lipgloss.RoundedBorder()).
			BorderForeground(colorMauve).
			Background(colorSurface0).
			Foreground(colorText).
			Padding(0, 1).
			Width(50)

	fileTreeStyle = lipgloss.NewStyle().
			Border(lipgloss.RoundedBorder()).
			BorderForeground(colorSurface1).
			Foreground(colorSubtext0).
			Padding(0, 1)

	messageStyle = lipgloss.NewStyle().
			Foreground(colorYellow).
			Italic(true)

	errorStyle = lipgloss.NewStyle().
			Foreground(colorRed).
			Bold(true)

	helpStyle = lipgloss.NewStyle().
			Foreground(colorOverlay0)

	lineNumberStyle = lipgloss.NewStyle().
			Foreground(colorOverlay0).
			Width(4).
			Align(lipgloss.Right)

	cursorLineStyle = lipgloss.NewStyle().
			Background(colorSurface0)
)
