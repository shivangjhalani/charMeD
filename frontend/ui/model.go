package ui

import (
	"encoding/json"
	"fmt"
	"os"

	tea "github.com/charmbracelet/bubbletea"
	"github.com/charmbracelet/lipgloss"

	"github.com/charmed/frontend/rpc"
)

func init() {
	// Prevent termenv/glamour from sending OSC queries to detect terminal
	// background color. Responses leak into stdin and appear as garbled text.
	os.Setenv("GLAMOUR_STYLE", "dark")
}

// Pane identifies the active pane.
type Pane int

const (
	PaneEditor Pane = iota
	PanePreview
)

// Model is the main Bubble Tea model orchestrating all UI components.
type Model struct {
	rpc            *rpc.Client
	editorPane     EditorPane
	previewPane    PreviewPane
	statusBar      StatusBar
	commandPalette CommandPalette
	fileTree       FileTree

	activePane    Pane
	filePath      string
	ready         bool
	quitting      bool
	message       string
	width, height int
}

// NewModel creates the root model.
func NewModel(client *rpc.Client) Model {
	return Model{
		rpc:            client,
		editorPane:     NewEditorPane(),
		previewPane:    NewPreviewPane(),
		statusBar:      NewStatusBar(),
		commandPalette: NewCommandPalette(),
		fileTree:       NewFileTree(),
		activePane:     PaneEditor,
	}
}

// Init runs initial commands.
func (m Model) Init() tea.Cmd {
	return tea.Batch(
		tea.SetWindowTitle("charMeD — Terminal Markdown Editor"),
	)
}

// Update is the main message handler.
func (m Model) Update(msg tea.Msg) (tea.Model, tea.Cmd) {
	var cmds []tea.Cmd

	switch msg := msg.(type) {
	case tea.WindowSizeMsg:
		m.width = msg.Width
		m.height = msg.Height
		m.ready = true
		m.recalcLayout()
		// Re-render preview on resize (like Glow)
		cmds = append(cmds, m.previewPane.ReRender())

	case tea.KeyMsg:
		// Force quit always works
		if msg.String() == "ctrl+c" {
			m.quitting = true
			return m, tea.Quit
		}

		// Editor pane handling
		if m.activePane == PaneEditor {
			// Forward all keys directly to editor
			cmd := m.editorPane.Update(msg)
			cmds = append(cmds, cmd)
			content := m.editorPane.textarea.Value()
			cmds = append(cmds, m.previewPane.SetContent(content))
			return m, tea.Batch(cmds...)
		}

		// Command palette intercepts when visible
		if m.commandPalette.IsVisible() {
			cmd, handled := m.commandPalette.Update(msg)
			if handled {
				cmds = append(cmds, cmd)
				return m, tea.Batch(cmds...)
			}
		}

		// File tree intercepts when visible
		if m.fileTree.IsVisible() {
			cmd, handled := m.fileTree.Update(msg)
			if handled {
				cmds = append(cmds, cmd)
				return m, tea.Batch(cmds...)
			}
		}

		// Global key handling
		switch msg.String() {
		case "ctrl+b":
			m.fileTree.Toggle()
			m.recalcLayout()
			return m, nil
		case ":":
			m.commandPalette.Show()
			return m, nil
		case "tab":
			if m.activePane == PaneEditor {
				m.activePane = PanePreview
			} else {
				m.activePane = PaneEditor
			}
			m.editorPane.SetFocused(m.activePane == PaneEditor)
			m.previewPane.SetFocused(m.activePane == PanePreview)
			return m, nil
		case "q":
			m.quitting = true
			return m, tea.Quit
		}

		// Navigation handling for Preview
		if m.activePane == PanePreview {
			switch msg.String() {
			case "j", "down", "k", "up", "pgup", "pgdown", "ctrl+u", "ctrl+d":
				cmd := m.previewPane.Update(msg)
				cmds = append(cmds, cmd)
			}
		}

	case ContentRenderedMsg:
		m.previewPane.ApplyRendered(msg.Content)

	case CommandExecuteMsg:
		cmds = append(cmds, m.executeCommand(msg.Command))

	case FileSelectedMsg:
		cmds = append(cmds, m.openFile(msg.Path))

	case rpc.NotificationMsg:
		m.handleNotification(msg)

	case fileOpenedMsg:
		m.editorPane.SetContent(msg.content)
		m.filePath = msg.path
		m.statusBar.SetMessage(fmt.Sprintf("Opened %s", msg.path))
		cmds = append(cmds, m.previewPane.SetContent(msg.content))
	}

	return m, tea.Batch(cmds...)
}

// View renders the full UI.
func (m Model) View() string {
	if m.quitting {
		return ""
	}
	if !m.ready {
		return "\n  Loading charMeD..."
	}

	// Layout: optional file tree | editor | preview
	var panes []string

	if m.fileTree.IsVisible() {
		treeWidth := 30
		panes = append(panes, m.fileTree.View(m.height-1))
		editorW := (m.width - treeWidth) / 2
		previewW := m.width - treeWidth - editorW

		m.editorPane.SetSize(editorW, m.height-1)
		m.previewPane.SetSize(previewW, m.height-1)

		panes = append(panes,
			m.editorPane.View(editorW, m.height-1),
			m.previewPane.View(previewW, m.height-1))
	} else {
		editorW := m.width / 2
		previewW := m.width - editorW

		panes = append(panes,
			m.editorPane.View(editorW, m.height-1),
			m.previewPane.View(previewW, m.height-1))
	}

	mainArea := lipgloss.JoinHorizontal(lipgloss.Top, panes...)

	// Command palette overlay (centered)
	if m.commandPalette.IsVisible() {
		// We just append it — in a real app we'd overlay
		mainArea = lipgloss.JoinVertical(lipgloss.Left,
			mainArea, m.commandPalette.View())
	}

	// Status bar at bottom
	m.statusBar.SetWidth(m.width)
	m.statusBar.Update(m.filePath,
		m.editorPane.cursor,
		0, len(m.editorPane.lines),
		false)

	return lipgloss.JoinVertical(lipgloss.Left,
		mainArea, m.statusBar.View())
}

func (m *Model) recalcLayout() {
	contentHeight := m.height - 1 // status bar

	if m.fileTree.IsVisible() {
		treeWidth := 30
		m.fileTree.SetSize(treeWidth, contentHeight)
		editorW := (m.width - treeWidth) / 2
		previewW := m.width - treeWidth - editorW
		m.editorPane.SetSize(editorW, contentHeight)
		m.previewPane.SetSize(previewW, contentHeight)
	} else {
		editorW := m.width / 2
		previewW := m.width - editorW
		m.editorPane.SetSize(editorW, contentHeight)
		m.previewPane.SetSize(previewW, contentHeight)
	}
}

// --- Command handling ---

func (m *Model) executeCommand(cmd string) tea.Cmd {
	switch cmd {
	case "w":
		return m.saveFile()
	case "q":
		m.quitting = true
		return tea.Quit
	case "wq":
		return tea.Sequence(m.saveFile(), tea.Quit)
	default:
		m.statusBar.SetMessage(fmt.Sprintf("Unknown command: %s", cmd))
		return nil
	}
}

func (m *Model) saveFile() tea.Cmd {
	if m.rpc == nil || m.filePath == "" {
		m.statusBar.SetMessage("No file path — use :w <path>")
		return nil
	}
	return func() tea.Msg {
		params := map[string]interface{}{"path": m.filePath}
		_, err := m.rpc.Call("document/save", params)
		if err != nil {
			return nil // ignore for now
		}
		return nil
	}
}

type fileOpenedMsg struct {
	content string
	path    string
}

func (m *Model) openFile(path string) tea.Cmd {
	if m.rpc == nil {
		// Direct file read fallback (no backend)
		return func() tea.Msg {
			data, err := readFileContent(path)
			if err != nil {
				return nil
			}
			return fileOpenedMsg{content: data, path: path}
		}
	}
	return func() tea.Msg {
		params := map[string]interface{}{"path": path}
		result, err := m.rpc.Call("document/open", params)
		if err != nil {
			return nil
		}
		var res struct {
			Content string `json:"content"`
		}
		json.Unmarshal(result, &res)
		return fileOpenedMsg{content: res.Content, path: path}
	}
}

func readFileContent(path string) (string, error) {
	data, err := readFile(path)
	if err != nil {
		return "", err
	}
	return string(data), nil
}

func readFile(path string) ([]byte, error) {
	return func() ([]byte, error) {
		return nil, fmt.Errorf("not implemented without backend")
	}()
}

func (m *Model) handleNotification(notif rpc.NotificationMsg) {
	switch notif.Method {
	case "ui/refresh":
		var params struct {
			Content string    `json:"content"`
			Cursor  CursorPos `json:"cursor"`
		}
		json.Unmarshal(notif.Params, &params)
		m.editorPane.SetContent(params.Content)
		m.editorPane.cursor = params.Cursor

	case "ui/statusBar" :
		var params struct {
			FilePath  string    `json:"filePath"`
			Dirty     bool      `json:"dirty"`
			WordCount int       `json:"wordCount"`
			LineCount int       `json:"lineCount"`
			Cursor    CursorPos `json:"cursor"`
		}
		json.Unmarshal(notif.Params, &params)
		m.statusBar.Update(params.FilePath,
			params.Cursor, params.WordCount, params.LineCount, params.Dirty)

	case "ui/message":
		var params struct {
			Text string `json:"text"`
		}
		json.Unmarshal(notif.Params, &params)
		m.statusBar.SetMessage(params.Text)
	}
}
