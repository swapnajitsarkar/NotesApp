import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;


public class NotesApp {
    private static final String NOTES_FILE = "notes.txt";
    private static final String NOTES_INDEX_FILE = "notes_index.txt";
    private static final Scanner scanner = new Scanner(System.in);

    public static void main(String[] args) {
        System.out.println("=== NOTES MANAGER ===");
        System.out.println("Welcome to your personal notes app!");

        createNotesFileIfNotExists();

        while (true) {
            displayMenu();
            int choice = getChoice();

            switch (choice) {
                case 1:
                    addNote();
                    break;
                case 2:
                    viewAllNotes();
                    break;
                case 3:
                    searchNotes();
                    break;
                case 4:
                    deleteNote();
                    break;
                case 5:
                    viewNotesCount();
                    break;
                case 6:
                    clearAllNotes();
                    break;
                case 7:
                    exportNotes();
                    break;
                case 8:
                    System.out.println("Thanks for using Notes Manager! Goodbye!");
                    return;
                default:
                    System.out.println("Invalid choice. Please try again.");
            }
        }
    }

    private static void displayMenu() {
        System.out.println("\n--- MENU ---");
        System.out.println("1. Add Note");
        System.out.println("2. View All Notes");
        System.out.println("3. Search Notes");
        System.out.println("4. Delete Note");
        System.out.println("5. View Notes Count");
        System.out.println("6. Clear All Notes");
        System.out.println("7. Export Notes");
        System.out.println("8. Exit");
        System.out.print("Enter your choice (1-8): ");
    }

    private static int getChoice() {
        try {
            return Integer.parseInt(scanner.nextLine().trim());
        } catch (NumberFormatException e) {
            return -1; // Invalid choice
        }
    }


    private static void createNotesFileIfNotExists() {
        try {
            File notesFile = new File(NOTES_FILE);
            if (!notesFile.exists()) {
                notesFile.createNewFile();
                System.out.println("Created new notes file: " + NOTES_FILE);
            }
        } catch (IOException e) {
            System.err.println("Error creating notes file: " + e.getMessage());
            logException(e);
        }
    }


    private static void addNote() {
        System.out.print("Enter note title: ");
        String title = scanner.nextLine().trim();

        if (title.isEmpty()) {
            System.out.println("Title cannot be empty!");
            return;
        }

        System.out.print("Enter note content: ");
        String content = scanner.nextLine().trim();

        if (content.isEmpty()) {
            System.out.println("Content cannot be empty!");
            return;
        }

        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        String noteEntry = String.format("=== %s ===\nCreated: %s\n%s\n%s\n\n",
                title, timestamp, "-".repeat(50), content);

        // Using try-with-resources for automatic resource management
        try (FileWriter writer = new FileWriter(NOTES_FILE, true)) { // true = append mode
            writer.write(noteEntry);
            writer.flush(); // Ensure data is written immediately
            System.out.println("✓ Note added successfully!");
        } catch (IOException e) {
            System.err.println("✗ Error adding note: " + e.getMessage());
            logException(e);
        }
    }


    private static void viewAllNotes() {
        // Using try-with-resources with BufferedReader for efficient reading
        try (BufferedReader reader = new BufferedReader(new FileReader(NOTES_FILE))) {
            String line;
            boolean hasNotes = false;

            System.out.println("\n--- ALL NOTES ---");

            while ((line = reader.readLine()) != null) {
                System.out.println(line);
                hasNotes = true;
            }

            if (!hasNotes) {
                System.out.println("No notes found. Add some notes first!");
            }

        } catch (FileNotFoundException e) {
            System.err.println("✗ Notes file not found: " + e.getMessage());
        } catch (IOException e) {
            System.err.println("✗ Error reading notes: " + e.getMessage());
            logException(e);
        }
    }


    private static void searchNotes() {
        System.out.print("Enter search keyword: ");
        String keyword = scanner.nextLine().trim().toLowerCase();

        if (keyword.isEmpty()) {
            System.out.println("Search keyword cannot be empty!");
            return;
        }

        try (BufferedReader reader = new BufferedReader(new FileReader(NOTES_FILE))) {
            String line;
            boolean found = false;
            StringBuilder currentNote = new StringBuilder();
            boolean inNote = false;

            System.out.println("\n--- SEARCH RESULTS ---");

            while ((line = reader.readLine()) != null) {
                if (line.startsWith("=== ") && line.endsWith(" ===")) {
                    // Start of a new note
                    if (inNote && currentNote.toString().toLowerCase().contains(keyword)) {
                        System.out.println(currentNote.toString());
                        found = true;
                    }
                    currentNote = new StringBuilder();
                    currentNote.append(line).append("\n");
                    inNote = true;
                } else if (inNote) {
                    currentNote.append(line).append("\n");
                }
            }

            // Check the last note
            if (inNote && currentNote.toString().toLowerCase().contains(keyword)) {
                System.out.println(currentNote.toString());
                found = true;
            }

            if (!found) {
                System.out.println("No notes found containing: " + keyword);
            }

        } catch (IOException e) {
            System.err.println("✗ Error searching notes: " + e.getMessage());
            logException(e);
        }
    }


    private static void deleteNote() {
        System.out.print("Enter title of note to delete: ");
        String titleToDelete = scanner.nextLine().trim();

        if (titleToDelete.isEmpty()) {
            System.out.println("Title cannot be empty!");
            return;
        }

        List<String> allLines = new ArrayList<>();
        boolean noteFound = false;

        // Read all content
        try (BufferedReader reader = new BufferedReader(new FileReader(NOTES_FILE))) {
            String line;
            boolean skipNote = false;

            while ((line = reader.readLine()) != null) {
                if (line.startsWith("=== ") && line.endsWith(" ===")) {
                    String noteTitle = line.substring(4, line.length() - 4);
                    if (noteTitle.equalsIgnoreCase(titleToDelete)) {
                        noteFound = true;
                        skipNote = true;
                        // Skip this note and its content
                        while ((line = reader.readLine()) != null && !line.isEmpty()) {
                            // Skip all lines until empty line (end of note)
                        }
                        continue;
                    } else {
                        skipNote = false;
                    }
                }

                if (!skipNote) {
                    allLines.add(line);
                }
            }
        } catch (IOException e) {
            System.err.println("✗ Error reading notes for deletion: " + e.getMessage());
            return;
        }

        if (!noteFound) {
            System.out.println("Note with title '" + titleToDelete + "' not found!");
            return;
        }

        // Rewrite file without deleted note
        try (FileWriter writer = new FileWriter(NOTES_FILE, false)) { // false = overwrite mode
            for (String line : allLines) {
                writer.write(line + "\n");
            }
            System.out.println("✓ Note deleted successfully!");
        } catch (IOException e) {
            System.err.println("✗ Error deleting note: " + e.getMessage());
            logException(e);
        }
    }


    private static void viewNotesCount() {
        int noteCount = 0;

        try (BufferedReader reader = new BufferedReader(new FileReader(NOTES_FILE))) {
            String line;

            while ((line = reader.readLine()) != null) {
                if (line.startsWith("=== ") && line.endsWith(" ===")) {
                    noteCount++;
                }
            }

            System.out.println("Total notes: " + noteCount);

        } catch (IOException e) {
            System.err.println("✗ Error counting notes: " + e.getMessage());
            logException(e);
        }
    }


    private static void clearAllNotes() {
        System.out.print("Are you sure you want to delete ALL notes? (y/N): ");
        String confirmation = scanner.nextLine().trim().toLowerCase();

        if (!confirmation.equals("y") && !confirmation.equals("yes")) {
            System.out.println("Operation cancelled.");
            return;
        }

        try (FileWriter writer = new FileWriter(NOTES_FILE, false)) { // false = overwrite mode
            // Writing nothing effectively clears the file
            System.out.println("✓ All notes cleared successfully!");
        } catch (IOException e) {
            System.err.println("✗ Error clearing notes: " + e.getMessage());
            logException(e);
        }
    }


    private static void exportNotes() {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        String exportFileName = "notes_backup_" + timestamp + ".txt";

        try {
            // Using NIO Files for modern file operations
            Path source = Paths.get(NOTES_FILE);
            Path target = Paths.get(exportFileName);

            Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING);

            System.out.println("✓ Notes exported to: " + exportFileName);

        } catch (IOException e) {
            System.err.println("✗ Error exporting notes: " + e.getMessage());
            logException(e);
        }
    }


    private static void logException(Exception e) {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));

        try (FileWriter errorWriter = new FileWriter("error_log.txt", true)) {
            errorWriter.write("=== ERROR LOG ===\n");
            errorWriter.write("Timestamp: " + timestamp + "\n");
            errorWriter.write("Exception: " + e.getClass().getSimpleName() + "\n");
            errorWriter.write("Message: " + e.getMessage() + "\n");
            errorWriter.write("Stack Trace:\n");

            // Write stack trace
            for (StackTraceElement element : e.getStackTrace()) {
                errorWriter.write("\tat " + element.toString() + "\n");
            }

            errorWriter.write("\n" + "-".repeat(50) + "\n\n");

        } catch (IOException logError) {
            System.err.println("Failed to log exception: " + logError.getMessage());
        }
    }
}