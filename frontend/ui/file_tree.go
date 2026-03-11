package ui

import (
	"fmt"
	"os"
	"path/filepath"
	"strings"

	"github.com/charmbracelet/bubbles/list"
	tea "github.com/charmbracelet/bubbletea"
)

// FileTree provides a file browser overlay.
type FileTree struct {
	list    list.Model
	visible bool
	cwd     string
}

// FileSelectedMsg is sent when a file is selected.
type FileSelectedMsg struct {
	Path string
}

type fileItem struct {
	name  string
	path  string
	isDir bool
}

func (f fileItem) Title() string {
	if f.isDir {
		return "📁 " + f.name
	}
	return "📄 " + f.name
}
func (f fileItem) Description() string { return f.path }
func (f fileItem) FilterValue() string { return f.name }

// NewFileTree creates a new file tree.
func NewFileTree() FileTree {
	delegate := list.NewDefaultDelegate()
	l := list.New(nil, delegate, 30, 20)
	l.Title = "Files"
	l.SetShowHelp(false)

	cwd, _ := os.Getwd()

	return FileTree{
		list: l,
		cwd:  cwd,
	}
}

// Show makes the file tree visible and loads files.
func (f *FileTree) Show() {
	f.visible = true
	f.loadFiles()
}

// Hide hides the file tree.
func (f *FileTree) Hide() {
	f.visible = false
}

// Toggle toggles visibility.
func (f *FileTree) Toggle() {
	if f.visible {
		f.Hide()
	} else {
		f.Show()
	}
}

// IsVisible returns visibility state.
func (f FileTree) IsVisible() bool {
	return f.visible
}

// SetSize sets the file tree dimensions.
func (f *FileTree) SetSize(w, h int) {
	f.list.SetWidth(w)
	f.list.SetHeight(h - 2)
}

func (f *FileTree) loadFiles() {
	entries, err := os.ReadDir(f.cwd)
	if err != nil {
		return
	}

	var items []list.Item
	// Parent directory
	if f.cwd != "/" {
		items = append(items, fileItem{
			name:  "..",
			path:  filepath.Dir(f.cwd),
			isDir: true,
		})
	}

	for _, entry := range entries {
		name := entry.Name()
		if strings.HasPrefix(name, ".") {
			continue // skip hidden files
		}
		ext := filepath.Ext(name)
		// Show only markdown files and directories
		if entry.IsDir() || ext == ".md" || ext == ".markdown" || ext == ".txt" {
			items = append(items, fileItem{
				name:  name,
				path:  filepath.Join(f.cwd, name),
				isDir: entry.IsDir(),
			})
		}
	}

	f.list.SetItems(items)
}

// Update handles messages.
func (f *FileTree) Update(msg tea.Msg) (tea.Cmd, bool) {
	if !f.visible {
		return nil, false
	}

	switch msg := msg.(type) {
	case tea.KeyMsg:
		switch msg.String() {
		case "esc", "ctrl+b":
			f.Hide()
			return nil, true
		case "enter":
			item, ok := f.list.SelectedItem().(fileItem)
			if !ok {
				return nil, true
			}
			if item.isDir {
				f.cwd = item.path
				f.loadFiles()
				return nil, true
			}
			f.Hide()
			return func() tea.Msg {
				return FileSelectedMsg{Path: item.path}
			}, true
		}
	}

	var cmd tea.Cmd
	f.list, cmd = f.list.Update(msg)
	return cmd, true
}

// View renders the file tree.
func (f FileTree) View(height int) string {
	if !f.visible {
		return ""
	}
	header := titleStyle.Render(fmt.Sprintf("📂 %s", filepath.Base(f.cwd)))
	return fileTreeStyle.Height(height - 2).Render(
		fmt.Sprintf("%s\n%s", header, f.list.View()))
}
