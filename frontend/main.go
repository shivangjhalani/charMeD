package main

import (
	"fmt"
	"os"
	"os/exec"
	"path/filepath"

	tea "github.com/charmbracelet/bubbletea"

	"github.com/charmed/frontend/rpc"
	"github.com/charmed/frontend/ui"
)

func main() {
	// Locate the backend JAR relative to the frontend binary
	execPath, _ := os.Executable()
	execDir := filepath.Dir(execPath)
	jarPath := filepath.Join(execDir, "..", "backend", "build", "libs", "backend-all.jar")

	// Also try relative to CWD (for development)
	if _, err := os.Stat(jarPath); os.IsNotExist(err) {
		jarPath = filepath.Join("backend", "build", "libs", "backend-all.jar")
	}

	var client *rpc.Client
	var backendCmd *exec.Cmd

	// Spawn Java backend
	if _, err := os.Stat(jarPath); err == nil {
		backendCmd = exec.Command("java", "--enable-preview", "-jar", jarPath)
		backendCmd.Stderr = os.Stderr

		stdin, err := backendCmd.StdinPipe()
		if err != nil {
			fmt.Fprintf(os.Stderr, "Error creating stdin pipe: %v\n", err)
			os.Exit(1)
		}
		stdout, err := backendCmd.StdoutPipe()
		if err != nil {
			fmt.Fprintf(os.Stderr, "Error creating stdout pipe: %v\n", err)
			os.Exit(1)
		}

		if err := backendCmd.Start(); err != nil {
			fmt.Fprintf(os.Stderr, "Error starting backend: %v\n", err)
			os.Exit(1)
		}

		// Create RPC client over the pipes
		client = rpc.NewClient(stdin, stdout)

		// Send initialize
		if _, err := client.Initialize(); err != nil {
			fmt.Fprintf(os.Stderr, "Warning: initialize failed: %v\n", err)
		}
	} else {
		fmt.Fprintf(os.Stderr, "Warning: backend JAR not found at %s, running without backend\n", jarPath)
	}

	// Create the Bubble Tea model
	model := ui.NewModel(client)

	// Create and run the program
	p := tea.NewProgram(model,
		tea.WithAltScreen(),
		tea.WithMouseCellMotion(),
	)

	// Start listening for backend notifications in a goroutine
	if client != nil {
		go client.ListenForNotifications(p)
	}

	// Open file from args
	if len(os.Args) > 1 {
		path := os.Args[1]
		absPath, _ := filepath.Abs(path)
		p.Send(ui.FileSelectedMsg{Path: absPath})
	}

	// Run
	if _, err := p.Run(); err != nil {
		fmt.Fprintf(os.Stderr, "Error: %v\n", err)
		os.Exit(1)
	}

	// Shutdown backend
	if client != nil {
		_ = client.Shutdown()
	}
	if backendCmd != nil {
		_ = backendCmd.Wait()
	}
}
