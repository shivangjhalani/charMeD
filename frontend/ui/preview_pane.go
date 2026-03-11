package ui

import (
	"fmt"
	"strings"

	"github.com/charmbracelet/bubbles/viewport"
	tea "github.com/charmbracelet/bubbletea"
	"github.com/charmbracelet/glamour"
)

// ContentRenderedMsg is sent when async Glamour rendering completes.
// Follows the same pattern as Glow's contentRenderedMsg.
type ContentRenderedMsg struct {
	Content string
}

// PreviewPane renders markdown via Glamour in a scrollable viewport.
// Architecture mirrors Glow's pager: async render, cache raw markdown,
// re-render on resize.
type PreviewPane struct {
	viewport viewport.Model
	rawMd    string // cached raw markdown for re-render on resize
	rendered string
	focused  bool
	width    int
	height   int
}

// NewPreviewPane creates a new preview pane.
func NewPreviewPane() PreviewPane {
	vp := viewport.New(40, 20)
	vp.HighPerformanceRendering = false
	return PreviewPane{
		viewport: vp,
	}
}

// SetSize resizes the preview pane and triggers a re-render.
func (p *PreviewPane) SetSize(w, h int) {
	p.width = w
	p.height = h
	p.viewport.Width = w - 4   // border + padding
	p.viewport.Height = h - 4  // border + padding + header
}

// SetContent caches raw markdown and triggers an async render.
// Returns a tea.Cmd (like Glow's renderWithGlamour).
func (p *PreviewPane) SetContent(rawMarkdown string) tea.Cmd {
	p.rawMd = rawMarkdown
	return p.renderAsync()
}

// renderAsync renders markdown in a goroutine — prevents UI blocking.
// Direct port of Glow's renderWithGlamour pattern.
func (p *PreviewPane) renderAsync() tea.Cmd {
	width := p.viewport.Width
	md := p.rawMd
	return func() tea.Msg {
		rendered, err := glamourRender(md, width)
		if err != nil {
			return ContentRenderedMsg{Content: md} // fallback to raw
		}
		return ContentRenderedMsg{Content: rendered}
	}
}

// glamourRender creates a fresh TermRenderer per call (like Glow).
func glamourRender(markdown string, width int) (string, error) {
	if width <= 0 {
		width = 80
	}
	r, err := glamour.NewTermRenderer(
		glamour.WithStylePath("dark"),
		glamour.WithWordWrap(width),
	)
	if err != nil {
		return "", fmt.Errorf("create glamour renderer: %w", err)
	}
	out, err := r.Render(markdown)
	if err != nil {
		return "", fmt.Errorf("render markdown: %w", err)
	}
	return strings.TrimSpace(out), nil
}

// ApplyRendered sets the rendered content in the viewport.
func (p *PreviewPane) ApplyRendered(content string) {
	p.rendered = content
	p.viewport.SetContent(content)
}

// ReRender triggers a fresh render (e.g., on resize). Returns a tea.Cmd.
func (p *PreviewPane) ReRender() tea.Cmd {
	if p.rawMd == "" {
		return nil
	}
	return p.renderAsync()
}

// SetFocused sets the focus state.
func (p *PreviewPane) SetFocused(focused bool) {
	p.focused = focused
}

// Update handles messages.
func (p *PreviewPane) Update(msg tea.Msg) tea.Cmd {
	var cmd tea.Cmd
	p.viewport, cmd = p.viewport.Update(msg)
	return cmd
}

// View renders the preview pane.
func (p PreviewPane) View(width, height int) string {
	style := previewBorderStyle
	if p.focused {
		style = previewActiveBorderStyle
	}

	header := titleStyle.Render("👁 Preview")
	body := p.viewport.View()

	content := fmt.Sprintf("%s\n%s", header, body)
	return style.Width(width - 2).Height(height - 2).Render(content)
}
