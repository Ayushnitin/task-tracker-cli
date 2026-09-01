import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class TaskCli {
    private static final Path FILE_PATH = Path.of("tasks.json");
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final Set<String> VALID_STATUSES = Set.of("todo", "in-progress", "done");

    public static void main(String[] args) {
        try {
            createFileIfNotExists();
            if (args.length == 0) { printHelp(); return; }
            switch (args[0].toLowerCase()) {
                case "add" -> addTask(args);
                case "update" -> updateTask(args);
                case "delete" -> deleteTask(args);
                case "mark-in-progress" -> changeStatus(args, "in-progress");
                case "mark-done" -> changeStatus(args, "done");
                case "mark-todo" -> changeStatus(args, "todo");
                case "list" -> listTasks(args);
                case "help" -> printHelp();
                default -> { System.out.println("Error: Unknown command '" + args[0] + "'"); printHelp(); }
            }
        } catch (IOException e) { System.out.println("Error: " + e.getMessage()); }
    }

    private static void createFileIfNotExists() throws IOException {
        if (!Files.exists(FILE_PATH)) Files.writeString(FILE_PATH, "[]\n", StandardCharsets.UTF_8);
    }

    private static void addTask(String[] args) throws IOException {
        if (args.length < 2) { System.out.println("Usage: task-cli add \"Task description\""); return; }
        String description = joinArguments(args, 1).trim();
        if (description.isEmpty()) { System.out.println("Error: Task description cannot be empty."); return; }
        List<Task> tasks = loadTasks();
        int id = getNextId(tasks); String now = getCurrentTime();
        tasks.add(new Task(id, description, "todo", now, now)); saveTasks(tasks);
        System.out.println("Task added successfully (ID: " + id + ")");
    }

    private static void updateTask(String[] args) throws IOException {
        if (args.length < 3) { System.out.println("Usage: task-cli update <id> \"New description\""); return; }
        Integer id = parseId(args[1]); if (id == null) return;
        String description = joinArguments(args, 2).trim();
        if (description.isEmpty()) { System.out.println("Error: Task description cannot be empty."); return; }
        List<Task> tasks = loadTasks(); Task task = findTask(tasks, id);
        if (task == null) { System.out.println("Error: Task with ID " + id + " not found."); return; }
        task.description = description; task.updatedAt = getCurrentTime(); saveTasks(tasks);
        System.out.println("Task updated successfully (ID: " + id + ")");
    }

    private static void deleteTask(String[] args) throws IOException {
        if (args.length != 2) { System.out.println("Usage: task-cli delete <id>"); return; }
        Integer id = parseId(args[1]); if (id == null) return;
        List<Task> tasks = loadTasks();
        if (!tasks.removeIf(task -> task.id == id)) { System.out.println("Error: Task with ID " + id + " not found."); return; }
        saveTasks(tasks); System.out.println("Task deleted successfully (ID: " + id + ")");
    }

    private static void changeStatus(String[] args, String status) throws IOException {
        if (args.length != 2) { System.out.println("Usage: task-cli " + args[0] + " <id>"); return; }
        Integer id = parseId(args[1]); if (id == null) return;
        List<Task> tasks = loadTasks(); Task task = findTask(tasks, id);
        if (task == null) { System.out.println("Error: Task with ID " + id + " not found."); return; }
        task.status = status; task.updatedAt = getCurrentTime(); saveTasks(tasks);
        System.out.println("Task " + id + " marked as " + status + ".");
    }

    private static void listTasks(String[] args) throws IOException {
        if (args.length > 2) { System.out.println("Usage: task-cli list [todo|in-progress|done]"); return; }
        String filter = args.length == 2 ? args[1].toLowerCase() : null;
        if (filter != null && !VALID_STATUSES.contains(filter)) {
            System.out.println("Error: Invalid status '" + filter + "'");
            System.out.println("Allowed statuses: todo, in-progress, done"); return;
        }
        List<Task> tasks = loadTasks(); boolean found = false;
        for (Task task : tasks) if (filter == null || task.status.equals(filter)) { printTask(task); found = true; }
        if (!found) System.out.println(filter == null ? "No tasks found." : "No tasks with status '" + filter + "' found.");
    }

    private static void printTask(Task task) {
        System.out.println("--------------------------------");
        System.out.println("ID          : " + task.id);
        System.out.println("Description : " + task.description);
        System.out.println("Status      : " + task.status);
        System.out.println("Created At  : " + task.createdAt);
        System.out.println("Updated At  : " + task.updatedAt);
    }

    private static Task findTask(List<Task> tasks, int id) { for (Task task : tasks) if (task.id == id) return task; return null; }
    private static int getNextId(List<Task> tasks) { int max = 0; for (Task task : tasks) max = Math.max(max, task.id); return max + 1; }
    private static String getCurrentTime() { return LocalDateTime.now().format(DATE_FORMAT); }

    private static Integer parseId(String value) {
        try { int id = Integer.parseInt(value); if (id <= 0) { System.out.println("Error: Task ID must be greater than 0."); return null; } return id; }
        catch (NumberFormatException e) { System.out.println("Error: Invalid task ID '" + value + "'"); return null; }
    }

    private static String joinArguments(String[] args, int startIndex) {
        StringBuilder result = new StringBuilder();
        for (int i = startIndex; i < args.length; i++) { if (i > startIndex) result.append(' '); result.append(args[i]); }
        return result.toString();
    }

    private static void saveTasks(List<Task> tasks) throws IOException {
        StringBuilder json = new StringBuilder("[\n");
        for (int i = 0; i < tasks.size(); i++) {
            Task t = tasks.get(i);
            json.append("  {\n").append("    \"id\": ").append(t.id).append(",\n")
                    .append("    \"description\": \"").append(escapeJson(t.description)).append("\",\n")
                    .append("    \"status\": \"").append(escapeJson(t.status)).append("\",\n")
                    .append("    \"createdAt\": \"").append(escapeJson(t.createdAt)).append("\",\n")
                    .append("    \"updatedAt\": \"").append(escapeJson(t.updatedAt)).append("\"\n  }");
            if (i < tasks.size() - 1) json.append(','); json.append('\n');
        }
        json.append("]\n"); Files.writeString(FILE_PATH, json.toString(), StandardCharsets.UTF_8);
    }

    private static List<Task> loadTasks() throws IOException {
        String json = Files.readString(FILE_PATH, StandardCharsets.UTF_8).trim();
        if (json.isEmpty()) throw new IOException("tasks.json is empty or malformed. Replace it with [] to reset storage.");
        Object root;
        try { root = new JsonParser(json).parse(); }
        catch (IllegalArgumentException e) { throw new IOException("tasks.json contains malformed JSON: " + e.getMessage()); }
        if (!(root instanceof List<?> list)) throw new IOException("tasks.json must contain a JSON array.");
        List<Task> tasks = new ArrayList<>(); Set<Integer> ids = new HashSet<>();
        for (Object value : list) {
            if (!(value instanceof Map<?, ?> rawMap)) throw new IOException("tasks.json contains invalid task data: each item must be an object.");
            Map<String, Object> map = new HashMap<>();
            for (Map.Entry<?, ?> entry : rawMap.entrySet()) {
                if (!(entry.getKey() instanceof String key)) throw new IOException("tasks.json contains invalid object keys.");
                map.put(key, entry.getValue());
            }
            int id = requirePositiveInt(map, "id");
            if (!ids.add(id)) throw new IOException("tasks.json contains duplicate task ID " + id + ".");
            String description = requireString(map, "description"); String status = requireString(map, "status");
            String createdAt = requireString(map, "createdAt"); String updatedAt = requireString(map, "updatedAt");
            if (description.trim().isEmpty()) throw new IOException("tasks.json contains a task with an empty description.");
            if (!VALID_STATUSES.contains(status)) throw new IOException("tasks.json contains invalid status '" + status + "'.");
            tasks.add(new Task(id, description, status, createdAt, updatedAt));
        }
        return tasks;
    }

    private static int requirePositiveInt(Map<String, Object> map, String key) throws IOException {
        Object value = map.get(key);
        if (!(value instanceof Long number) || number <= 0 || number > Integer.MAX_VALUE) throw new IOException("tasks.json field '" + key + "' must be a positive integer.");
        return number.intValue();
    }
    private static String requireString(Map<String, Object> map, String key) throws IOException {
        Object value = map.get(key); if (!(value instanceof String text)) throw new IOException("tasks.json field '" + key + "' must be a string."); return text;
    }

    private static String escapeJson(String text) {
        StringBuilder out = new StringBuilder();
        for (char c : text.toCharArray()) switch (c) {
            case '"' -> out.append("\\\""); case '\\' -> out.append("\\\\"); case '\b' -> out.append("\\b");
            case '\f' -> out.append("\\f"); case '\n' -> out.append("\\n"); case '\r' -> out.append("\\r"); case '\t' -> out.append("\\t");
            default -> { if (c < 0x20) out.append(String.format("\\u%04x", (int)c)); else out.append(c); }
        }
        return out.toString();
    }

    private static void printHelp() {
        System.out.println("TASK TRACKER CLI\n================\n" +
                "task-cli add \"description\"\n" + "task-cli update <id> \"description\"\n" + "task-cli delete <id>\n" +
                "task-cli mark-in-progress <id>\n" + "task-cli mark-done <id>\n" + "task-cli mark-todo <id>\n" +
                "task-cli list\n" + "task-cli list todo\n" + "task-cli list in-progress\n" + "task-cli list done\n" + "task-cli help");
    }

    private static class Task {
        int id; String description; String status; String createdAt; String updatedAt;
        Task(int id, String description, String status, String createdAt, String updatedAt) {
            this.id = id; this.description = description; this.status = status; this.createdAt = createdAt; this.updatedAt = updatedAt;
        }
    }

    private static class JsonParser {
        private final String input; private int pos;
        JsonParser(String input) { this.input = input; }
        Object parse() { skipWhitespace(); Object value = parseValue(); skipWhitespace(); if (pos != input.length()) error("unexpected trailing content"); return value; }
        private Object parseValue() {
            skipWhitespace(); if (pos >= input.length()) error("unexpected end of input"); char c = input.charAt(pos);
            if (c == '"') return parseString(); if (c == '{') return parseObject(); if (c == '[') return parseArray(); if (c == '-' || Character.isDigit(c)) return parseNumber();
            if (input.startsWith("true", pos)) { pos += 4; return Boolean.TRUE; } if (input.startsWith("false", pos)) { pos += 5; return Boolean.FALSE; }
            if (input.startsWith("null", pos)) { pos += 4; return null; } error("unexpected character '" + c + "'"); return null;
        }
        private List<Object> parseArray() {
            expect('['); List<Object> values = new ArrayList<>(); skipWhitespace(); if (peek(']')) { pos++; return values; }
            while (true) { values.add(parseValue()); skipWhitespace(); if (peek(']')) { pos++; return values; } expect(','); }
        }
        private Map<String, Object> parseObject() {
            expect('{'); Map<String, Object> values = new HashMap<>(); skipWhitespace(); if (peek('}')) { pos++; return values; }
            while (true) { skipWhitespace(); if (!peek('"')) error("object key must be a string"); String key = parseString(); skipWhitespace(); expect(':');
                if (values.put(key, parseValue()) != null) error("duplicate key '" + key + "'"); skipWhitespace(); if (peek('}')) { pos++; return values; } expect(','); }
        }
        private Long parseNumber() {
            int start = pos; if (peek('-')) pos++; if (pos >= input.length() || !Character.isDigit(input.charAt(pos))) error("invalid number");
            if (input.charAt(pos) == '0') { pos++; if (pos < input.length() && Character.isDigit(input.charAt(pos))) error("leading zeros are not allowed"); }
            else while (pos < input.length() && Character.isDigit(input.charAt(pos))) pos++;
            if (pos < input.length() && (input.charAt(pos) == '.' || input.charAt(pos) == 'e' || input.charAt(pos) == 'E')) error("task IDs must be integers");
            try { return Long.parseLong(input.substring(start, pos)); } catch (NumberFormatException e) { error("number is out of range"); return 0L; }
        }
        private String parseString() {
            expect('"'); StringBuilder out = new StringBuilder();
            while (pos < input.length()) { char c = input.charAt(pos++); if (c == '"') return out.toString(); if (c < 0x20) error("unescaped control character in string");
                if (c != '\\') { out.append(c); continue; } if (pos >= input.length()) error("unfinished escape sequence"); char e = input.charAt(pos++);
                switch (e) { case '"' -> out.append('"'); case '\\' -> out.append('\\'); case '/' -> out.append('/'); case 'b' -> out.append('\b'); case 'f' -> out.append('\f');
                    case 'n' -> out.append('\n'); case 'r' -> out.append('\r'); case 't' -> out.append('\t'); case 'u' -> out.append(parseUnicodeEscape()); default -> error("invalid escape sequence \\" + e + "'"); }
            } error("unterminated string"); return "";
        }
        private char parseUnicodeEscape() {
            if (pos + 4 > input.length()) error("incomplete unicode escape"); String hex = input.substring(pos, pos + 4); pos += 4;
            try { return (char) Integer.parseInt(hex, 16); } catch (NumberFormatException e) { error("invalid unicode escape \\u" + hex); return 0; }
        }
        private void expect(char expected) { skipWhitespace(); if (pos >= input.length() || input.charAt(pos) != expected) error("expected '" + expected + "'"); pos++; }
        private boolean peek(char c) { return pos < input.length() && input.charAt(pos) == c; }
        private void skipWhitespace() { while (pos < input.length() && Character.isWhitespace(input.charAt(pos))) pos++; }
        private void error(String message) { throw new IllegalArgumentException(message + " at position " + pos); }
    }
}
