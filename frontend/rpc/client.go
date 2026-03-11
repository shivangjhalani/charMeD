package rpc

import (
	"bufio"
	"encoding/json"
	"fmt"
	"io"
	"strconv"
	"strings"
	"sync"
	"sync/atomic"

	tea "github.com/charmbracelet/bubbletea"
)

// Request is a JSON-RPC 2.0 request.
type Request struct {
	JSONRPC string      `json:"jsonrpc"`
	ID      int64       `json:"id"`
	Method  string      `json:"method"`
	Params  interface{} `json:"params,omitempty"`
}

// Response is a JSON-RPC 2.0 response.
type Response struct {
	JSONRPC string          `json:"jsonrpc"`
	ID      int64           `json:"id"`
	Result  json.RawMessage `json:"result,omitempty"`
	Error   *RPCError       `json:"error,omitempty"`
}

// RPCError is a JSON-RPC error object.
type RPCError struct {
	Code    int    `json:"code"`
	Message string `json:"message"`
}

func (e *RPCError) Error() string {
	return fmt.Sprintf("RPC error %d: %s", e.Code, e.Message)
}

// Notification is a JSON-RPC 2.0 notification from the server.
type Notification struct {
	JSONRPC string          `json:"jsonrpc"`
	Method  string          `json:"method"`
	Params  json.RawMessage `json:"params"`
}

// NotificationMsg wraps a notification as a Bubble Tea message.
type NotificationMsg struct {
	Method string
	Params json.RawMessage
}

// Client communicates with the Java backend over stdin/stdout using
// Content-Length framed JSON-RPC 2.0.
type Client struct {
	writer io.Writer
	reader *bufio.Reader
	id     atomic.Int64
	mu     sync.Mutex // protects writer
}

// NewClient creates a new RPC client connected to the backend's pipes.
func NewClient(writer io.Writer, reader io.Reader) *Client {
	return &Client{
		writer: writer,
		reader: bufio.NewReader(reader),
	}
}

// Call sends a request and waits for the response.
func (c *Client) Call(method string, params interface{}) (json.RawMessage, error) {
	id := c.id.Add(1)
	req := Request{
		JSONRPC: "2.0",
		ID:      id,
		Method:  method,
		Params:  params,
	}

	body, err := json.Marshal(req)
	if err != nil {
		return nil, fmt.Errorf("marshal request: %w", err)
	}

	c.mu.Lock()
	if err := c.writeMessage(body); err != nil {
		c.mu.Unlock()
		return nil, fmt.Errorf("write request: %w", err)
	}
	c.mu.Unlock()

	// Read response (simplified: assumes responses come in order)
	for {
		msg, err := c.readMessage()
		if err != nil {
			return nil, fmt.Errorf("read response: %w", err)
		}

		// Check if it's a response (has "id" field)
		var resp Response
		if err := json.Unmarshal(msg, &resp); err == nil && resp.ID == id {
			if resp.Error != nil {
				return nil, resp.Error
			}
			return resp.Result, nil
		}
		// If not our response, it might be a notification — skip for now
	}
}

// Notify sends a notification (no response expected).
func (c *Client) Notify(method string, params interface{}) error {
	req := struct {
		JSONRPC string      `json:"jsonrpc"`
		Method  string      `json:"method"`
		Params  interface{} `json:"params,omitempty"`
	}{
		JSONRPC: "2.0",
		Method:  method,
		Params:  params,
	}

	body, err := json.Marshal(req)
	if err != nil {
		return fmt.Errorf("marshal notification: %w", err)
	}

	c.mu.Lock()
	defer c.mu.Unlock()
	return c.writeMessage(body)
}

// ListenForNotifications reads notifications from the backend and sends them
// as Bubble Tea messages. Should be run in a goroutine.
func (c *Client) ListenForNotifications(p *tea.Program) {
	for {
		msg, err := c.readMessage()
		if err != nil {
			return // EOF or error — backend died
		}

		var notif Notification
		if err := json.Unmarshal(msg, &notif); err != nil {
			continue
		}

		// Only forward notifications (no "id" field)
		if notif.Method != "" {
			p.Send(NotificationMsg{
				Method: notif.Method,
				Params: notif.Params,
			})
		}
	}
}

// Initialize sends the "initialize" request to the backend.
func (c *Client) Initialize() (json.RawMessage, error) {
	return c.Call("initialize", map[string]interface{}{
		"clientVersion": "0.1.0",
	})
}

// Shutdown sends the "shutdown" request to the backend.
func (c *Client) Shutdown() error {
	_, err := c.Call("shutdown", nil)
	return err
}

// --- Content-Length framed I/O ---

func (c *Client) writeMessage(body []byte) error {
	header := fmt.Sprintf("Content-Length: %d\r\n\r\n", len(body))
	if _, err := io.WriteString(c.writer, header); err != nil {
		return err
	}
	_, err := c.writer.Write(body)
	return err
}

func (c *Client) readMessage() ([]byte, error) {
	// Read Content-Length header
	for {
		line, err := c.reader.ReadString('\n')
		if err != nil {
			return nil, err
		}
		line = strings.TrimSpace(line)
		if strings.HasPrefix(line, "Content-Length:") {
			lengthStr := strings.TrimSpace(strings.TrimPrefix(line, "Content-Length:"))
			length, err := strconv.Atoi(lengthStr)
			if err != nil {
				return nil, fmt.Errorf("invalid Content-Length: %s", lengthStr)
			}

			// Skip to blank line
			for {
				sep, err := c.reader.ReadString('\n')
				if err != nil {
					return nil, err
				}
				if strings.TrimSpace(sep) == "" {
					break
				}
			}

			// Read body
			body := make([]byte, length)
			_, err = io.ReadFull(c.reader, body)
			if err != nil {
				return nil, fmt.Errorf("read body: %w", err)
			}
			return body, nil
		}
	}
}
