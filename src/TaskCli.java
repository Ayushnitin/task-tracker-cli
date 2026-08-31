import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TaskCli {

    private static final Path FILE_PATH = Path.of("tasks.json");

    private static final DateTimeFormatter DATE_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public static void main(String[] args) {

        try {
            createFileIfNotExists();

            if (args.length == 0) {
                printHelp();
                return;
            }

            String command = args[0].toLowerCase();

            switch (command) {

                case "add":
                    addTask(args);
                    break;

                case "update":
                    updateTask(args);
                    break;

                case "delete":
                    deleteTask(args);
                    break;

                case "mark-in-progress":
                    changeStatus(args, "in-progress");
                    break;

                case "mark-done":
                    changeStatus(args, "done");
                    break;

                case "mark-todo":
                    changeStatus(args, "todo");
                    break;

                case "list":
                    listTasks(args);
                    break;

                case "help":
                    printHelp();
                    break;

                default:
                    System.out.println("Error: Unknown command '" + command + "'");
                    printHelp();
            }

        } catch (Exception e) {
            System.out.println("Error: " + e.getMessage());
        }
    }

    // ---------------------------------------------------------
    // CREATE JSON FILE
    // ---------------------------------------------------------

    private static void createFileIfNotExists() throws IOException {

        if (!Files.exists(FILE_PATH)) {
            Files.writeString(
                    FILE_PATH,
                    "[]",
                    StandardCharsets.UTF_8
            );
        }
    }

    // ---------------------------------------------------------
    // ADD TASK
    // ---------------------------------------------------------

    private static void addTask(String[] args) throws IOException {

        if (args.length < 2) {
            System.out.println("Usage:");
            System.out.println("task-cli add \"Task description\"");
            return;
        }

        String description = joinArguments(args, 1);

        if (description.trim().isEmpty()) {
            System.out.println("Error: Task description cannot be empty.");
            return;
        }

        List<Task> tasks = loadTasks();

        int newId = getNextId(tasks);

        String currentTime = getCurrentTime();

        Task task = new Task(
                newId,
                description,
                "todo",
                currentTime,
                currentTime
        );

        tasks.add(task);

        saveTasks(tasks);

        System.out.println(
                "Task added successfully (ID: " + newId + ")"
        );
    }

    // ---------------------------------------------------------
    // UPDATE TASK
    // ---------------------------------------------------------

    private static void updateTask(String[] args) throws IOException {

        if (args.length < 3) {
            System.out.println("Usage:");
            System.out.println(
                    "task-cli update <id> \"New description\""
            );
            return;
        }

        Integer id = parseId(args[1]);

        if (id == null) {
            return;
        }

        String newDescription = joinArguments(args, 2);

        if (newDescription.trim().isEmpty()) {
            System.out.println(
                    "Error: Task description cannot be empty."
            );
            return;
        }

        List<Task> tasks = loadTasks();

        Task task = findTask(tasks, id);

        if (task == null) {
            System.out.println(
                    "Error: Task with ID " + id + " not found."
            );
            return;
        }

        task.description = newDescription;
        task.updatedAt = getCurrentTime();

        saveTasks(tasks);

        System.out.println(
                "Task updated successfully (ID: " + id + ")"
        );
    }

    // ---------------------------------------------------------
    // DELETE TASK
    // ---------------------------------------------------------

    private static void deleteTask(String[] args) throws IOException {

        if (args.length != 2) {
            System.out.println("Usage:");
            System.out.println("task-cli delete <id>");
            return;
        }

        Integer id = parseId(args[1]);

        if (id == null) {
            return;
        }

        List<Task> tasks = loadTasks();

        boolean removed = tasks.removeIf(
                task -> task.id == id
        );

        if (!removed) {
            System.out.println(
                    "Error: Task with ID " + id + " not found."
            );
            return;
        }

        saveTasks(tasks);

        System.out.println(
                "Task deleted successfully (ID: " + id + ")"
        );
    }

    // ---------------------------------------------------------
    // CHANGE STATUS
    // ---------------------------------------------------------

    private static void changeStatus(
            String[] args,
            String status
    ) throws IOException {

        if (args.length != 2) {
            System.out.println(
                    "Usage: task-cli mark-" +
                            status +
                            " <id>"
            );
            return;
        }

        Integer id = parseId(args[1]);

        if (id == null) {
            return;
        }

        List<Task> tasks = loadTasks();

        Task task = findTask(tasks, id);

        if (task == null) {
            System.out.println(
                    "Error: Task with ID " + id + " not found."
            );
            return;
        }

        task.status = status;
        task.updatedAt = getCurrentTime();

        saveTasks(tasks);

        System.out.println(
                "Task " + id +
                        " marked as " +
                        status + "."
        );
    }

    // ---------------------------------------------------------
    // LIST TASKS
    // ---------------------------------------------------------

    private static void listTasks(String[] args)
            throws IOException {

        String filter = null;

        if (args.length > 2) {
            System.out.println(
                    "Usage: task-cli list [todo|in-progress|done]"
            );
            return;
        }

        if (args.length == 2) {

            filter = args[1].toLowerCase();

            if (!filter.equals("todo")
                    && !filter.equals("in-progress")
                    && !filter.equals("done")) {

                System.out.println(
                        "Error: Invalid status '" +
                                filter + "'"
                );

                System.out.println(
                        "Allowed statuses: todo, in-progress, done"
                );

                return;
            }
        }

        List<Task> tasks = loadTasks();

        boolean found = false;

        for (Task task : tasks) {

            if (filter == null ||
                    task.status.equals(filter)) {

                printTask(task);

                found = true;
            }
        }

        if (!found) {

            if (filter == null) {
                System.out.println("No tasks found.");
            } else {
                System.out.println(
                        "No tasks with status '" +
                                filter + "' found."
                );
            }
        }
    }

    // ---------------------------------------------------------
    // PRINT SINGLE TASK
    // ---------------------------------------------------------

    private static void printTask(Task task) {

        System.out.println("--------------------------------");
        System.out.println("ID          : " + task.id);
        System.out.println(
                "Description : " + task.description
        );
        System.out.println(
                "Status      : " + task.status
        );
        System.out.println(
                "Created At  : " + task.createdAt
        );
        System.out.println(
                "Updated At  : " + task.updatedAt
        );
    }

    // ---------------------------------------------------------
    // FIND TASK
    // ---------------------------------------------------------

    private static Task findTask(
            List<Task> tasks,
            int id
    ) {

        for (Task task : tasks) {

            if (task.id == id) {
                return task;
            }
        }

        return null;
    }

    // ---------------------------------------------------------
    // GENERATE NEXT ID
    // ---------------------------------------------------------

    private static int getNextId(List<Task> tasks) {

        int maxId = 0;

        for (Task task : tasks) {

            if (task.id > maxId) {
                maxId = task.id;
            }
        }

        return maxId + 1;
    }

    // ---------------------------------------------------------
    // GET CURRENT DATE AND TIME
    // ---------------------------------------------------------

    private static String getCurrentTime() {

        return LocalDateTime.now().format(DATE_FORMAT);
    }

    // ---------------------------------------------------------
    // PARSE ID
    // ---------------------------------------------------------

    private static Integer parseId(String value) {

        try {

            int id = Integer.parseInt(value);

            if (id <= 0) {
                System.out.println(
                        "Error: Task ID must be greater than 0."
                );

                return null;
            }

            return id;

        } catch (NumberFormatException e) {

            System.out.println(
                    "Error: Invalid task ID '" +
                            value + "'"
            );

            return null;
        }
    }

    // ---------------------------------------------------------
    // JOIN COMMAND-LINE ARGUMENTS
    // ---------------------------------------------------------

    private static String joinArguments(
            String[] args,
            int startIndex
    ) {

        StringBuilder result = new StringBuilder();

        for (int i = startIndex; i < args.length; i++) {

            if (i > startIndex) {
                result.append(" ");
            }

            result.append(args[i]);
        }

        return result.toString();
    }

    // ---------------------------------------------------------
    // SAVE TASKS TO JSON
    // ---------------------------------------------------------

    private static void saveTasks(
            List<Task> tasks
    ) throws IOException {

        StringBuilder json = new StringBuilder();

        json.append("[\n");

        for (int i = 0; i < tasks.size(); i++) {

            Task task = tasks.get(i);

            json.append("  {\n");

            json.append("    \"id\": ")
                    .append(task.id)
                    .append(",\n");

            json.append("    \"description\": \"")
                    .append(escapeJson(task.description))
                    .append("\",\n");

            json.append("    \"status\": \"")
                    .append(escapeJson(task.status))
                    .append("\",\n");

            json.append("    \"createdAt\": \"")
                    .append(escapeJson(task.createdAt))
                    .append("\",\n");

            json.append("    \"updatedAt\": \"")
                    .append(escapeJson(task.updatedAt))
                    .append("\"\n");

            json.append("  }");

            if (i < tasks.size() - 1) {
                json.append(",");
            }

            json.append("\n");
        }

        json.append("]");

        Files.writeString(
                FILE_PATH,
                json.toString(),
                StandardCharsets.UTF_8
        );
    }

    // ---------------------------------------------------------
    // LOAD TASKS FROM JSON
    // ---------------------------------------------------------

    private static List<Task> loadTasks()
            throws IOException {

        List<Task> tasks = new ArrayList<>();

        String json = Files.readString(
                FILE_PATH,
                StandardCharsets.UTF_8
        ).trim();

        if (json.isEmpty() || json.equals("[]")) {
            return tasks;
        }

        List<String> objects = extractJsonObjects(json);

        for (String object : objects) {

            try {

                int id = Integer.parseInt(
                        extractNumber(object, "id")
                );

                String description =
                        extractString(
                                object,
                                "description"
                        );

                String status =
                        extractString(
                                object,
                                "status"
                        );

                String createdAt =
                        extractString(
                                object,
                                "createdAt"
                        );

                String updatedAt =
                        extractString(
                                object,
                                "updatedAt"
                        );

                tasks.add(
                        new Task(
                                id,
                                description,
                                status,
                                createdAt,
                                updatedAt
                        )
                );

            } catch (Exception e) {

                throw new IOException(
                        "tasks.json contains invalid data."
                );
            }
        }

        return tasks;
    }

    // ---------------------------------------------------------
    // EXTRACT JSON OBJECTS
    // ---------------------------------------------------------

    private static List<String> extractJsonObjects(
            String json
    ) {

        List<String> objects = new ArrayList<>();

        boolean insideString = false;
        boolean escaped = false;

        int depth = 0;
        int start = -1;

        for (int i = 0; i < json.length(); i++) {

            char c = json.charAt(i);

            if (insideString) {

                if (escaped) {

                    escaped = false;

                } else if (c == '\\') {

                    escaped = true;

                } else if (c == '"') {

                    insideString = false;
                }

                continue;
            }

            if (c == '"') {

                insideString = true;

            } else if (c == '{') {

                if (depth == 0) {
                    start = i;
                }

                depth++;

            } else if (c == '}') {

                depth--;

                if (depth == 0 && start != -1) {

                    objects.add(
                            json.substring(start, i + 1)
                    );

                    start = -1;
                }
            }
        }

        return objects;
    }

    // ---------------------------------------------------------
    // EXTRACT NUMBER FROM JSON
    // ---------------------------------------------------------

    private static String extractNumber(
            String json,
            String key
    ) {

        Pattern pattern = Pattern.compile(
                "\"" +
                        Pattern.quote(key) +
                        "\"\\s*:\\s*(\\d+)"
        );

        Matcher matcher = pattern.matcher(json);

        if (!matcher.find()) {
            throw new IllegalArgumentException();
        }

        return matcher.group(1);
    }

    // ---------------------------------------------------------
    // EXTRACT STRING FROM JSON
    // ---------------------------------------------------------

    private static String extractString(
            String json,
            String key
    ) {

        Pattern pattern = Pattern.compile(
                "\"" +
                        Pattern.quote(key) +
                        "\"\\s*:\\s*\"((?:\\\\.|[^\"\\\\])*)\""
        );

        Matcher matcher = pattern.matcher(json);

        if (!matcher.find()) {
            throw new IllegalArgumentException();
        }

        return unescapeJson(matcher.group(1));
    }

    // ---------------------------------------------------------
    // JSON ESCAPING
    // ---------------------------------------------------------

    private static String escapeJson(String text) {

        return text
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }

    private static String unescapeJson(String text) {

        StringBuilder result = new StringBuilder();

        boolean escaped = false;

        for (int i = 0; i < text.length(); i++) {

            char c = text.charAt(i);

            if (!escaped) {

                if (c == '\\') {
                    escaped = true;
                } else {
                    result.append(c);
                }

            } else {

                switch (c) {

                    case 'n':
                        result.append('\n');
                        break;

                    case 'r':
                        result.append('\r');
                        break;

                    case 't':
                        result.append('\t');
                        break;

                    case '"':
                        result.append('"');
                        break;

                    case '\\':
                        result.append('\\');
                        break;

                    default:
                        result.append(c);
                }

                escaped = false;
            }
        }

        if (escaped) {
            result.append('\\');
        }

        return result.toString();
    }

    // ---------------------------------------------------------
    // HELP
    // ---------------------------------------------------------

    private static void printHelp() {

        System.out.println();
        System.out.println("TASK TRACKER CLI");
        System.out.println("================");
        System.out.println();

        System.out.println(
                "task-cli add \"description\""
        );

        System.out.println(
                "task-cli update <id> \"description\""
        );

        System.out.println(
                "task-cli delete <id>"
        );

        System.out.println(
                "task-cli mark-in-progress <id>"
        );

        System.out.println(
                "task-cli mark-done <id>"
        );

        System.out.println(
                "task-cli mark-todo <id>"
        );

        System.out.println(
                "task-cli list"
        );

        System.out.println(
                "task-cli list todo"
        );

        System.out.println(
                "task-cli list in-progress"
        );

        System.out.println(
                "task-cli list done"
        );

        System.out.println();
    }

    // ---------------------------------------------------------
    // TASK MODEL
    // ---------------------------------------------------------

    static class Task {

        int id;
        String description;
        String status;
        String createdAt;
        String updatedAt;

        Task(
                int id,
                String description,
                String status,
                String createdAt,
                String updatedAt
        ) {

            this.id = id;
            this.description = description;
            this.status = status;
            this.createdAt = createdAt;
            this.updatedAt = updatedAt;
        }
    }
}