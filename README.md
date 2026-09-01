# Task Tracker CLI

A small dependency-free Java command-line application for creating, updating, deleting, listing, and changing the status of tasks. Task data is stored locally in `tasks.json`.

## Requirements

- JDK 17 or newer
- A terminal (PowerShell, Command Prompt, Bash, etc.)

Check Java with:

```bash
java -version
javac -version
```

## Clone and setup

```bash
git clone https://github.com/Ayushnitin/task-tracker-cli.git
cd task-tracker-cli
```

No Maven, Gradle, database, or external JSON library is required.

## Compile

From the repository root:

```bash
javac -d out src/TaskCli.java
```

Run directly with Java:

```bash
java -cp out TaskCli help
```

### Windows shortcut

The repository includes `task-cli.bat`. After compiling, you can run commands such as:

```powershell
.\task-cli.bat list
.\task-cli.bat add "Buy groceries"
```

## Commands

| Command | Purpose |
|---|---|
| `task-cli add "description"` | Add a task with status `todo` |
| `task-cli update <id> "description"` | Update a task description |
| `task-cli delete <id>` | Delete a task |
| `task-cli mark-in-progress <id>` | Mark a task in progress |
| `task-cli mark-done <id>` | Mark a task done |
| `task-cli mark-todo <id>` | Move a task back to todo |
| `task-cli list` | List all tasks |
| `task-cli list todo` | List todo tasks |
| `task-cli list in-progress` | List in-progress tasks |
| `task-cli list done` | List completed tasks |
| `task-cli help` | Show command help |

## Usage examples

```powershell
.\task-cli.bat add "Finish Java assignment"
.\task-cli.bat update 1 "Finish Java CLI assignment"
.\task-cli.bat mark-in-progress 1
.\task-cli.bat list in-progress
.\task-cli.bat mark-done 1
.\task-cli.bat delete 1
```

Descriptions can contain spaces and JSON-sensitive characters. For example:

```powershell
.\task-cli.bat add 'Fix "login" path C:\temp & test'
```

## JSON storage

`tasks.json` is created automatically in the directory where the program is run. It is runtime data and is intentionally ignored by Git.

Each task contains:

```json
{
  "id": 1,
  "description": "Finish Java assignment",
  "status": "todo",
  "createdAt": "2026-09-01 10:30:00",
  "updatedAt": "2026-09-01 10:30:00"
}
```

The application escapes quotes, backslashes, tabs, newlines, carriage returns, and JSON control characters when saving. When loading, it validates JSON syntax and required task fields. Invalid JSON is not silently overwritten; the program prints an error so the data can be corrected. To reset storage manually, replace the file contents with `[]`.

Valid statuses are `todo`, `in-progress`, and `done`. Task IDs must be unique positive integers.

## Validation and error handling

The CLI rejects empty descriptions, invalid or non-positive IDs, unknown commands, invalid status filters, missing task IDs, malformed JSON, non-array JSON roots, missing/wrong field types, duplicate IDs, and invalid stored statuses.

## Test scenarios

The final revision was compiled with JDK 21 and checked individually against the following scenarios:

| Scenario | Expected result | Result |
|---|---|---|
| Compile `TaskCli.java` | Compilation succeeds | PASS |
| List with empty storage | `No tasks found.` | PASS |
| Add normal task | Task created with ID and `todo` | PASS |
| Add special characters | Quotes/backslashes survive save/load | PASS |
| Update existing task | Description and `updatedAt` change | PASS |
| Mark in-progress | Status becomes `in-progress` | PASS |
| Mark done | Status becomes `done` | PASS |
| List by status | Only matching tasks shown | PASS |
| Delete existing task | Task removed | PASS |
| Non-numeric ID | Clear invalid-ID error | PASS |
| Negative ID | Positive-ID validation error | PASS |
| Missing task ID | Not-found error | PASS |
| Invalid list status | Allowed statuses shown | PASS |
| Malformed JSON | Clear malformed-JSON error | PASS |

## Project structure

```text
task-tracker-cli/
├── .gitignore
├── README.md
├── task-cli.bat
└── src/
    └── TaskCli.java
```

Generated `.class` files, IDE metadata, and runtime `tasks.json` are excluded from the final source submission.
