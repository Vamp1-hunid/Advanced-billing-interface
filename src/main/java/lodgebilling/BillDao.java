package lodgebilling;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import lodgebilling.model.Bill;
import lodgebilling.model.BillInput;
import lodgebilling.model.RoomType;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Data access: stores the bills and hands them out. It hides how bills are
 * kept, so the model and controller never touch the storage directly.
 *
 * Bills are kept in memory while the app runs and are also saved to a file in
 * the user's home folder (.lodgebilling/receipts.tsv), so they are still there
 * the next time the app starts. The file is outside the project folder on
 * purpose, so guest details are never committed to Git by accident.
 */
public class BillDao {

    private final ObservableList<Bill> bills = FXCollections.observableArrayList();
    private final Path file;

    private int nextReceiptNumber = 1;

    /** Uses the normal save file and loads any receipts saved earlier. */
    public BillDao() {
        this(Path.of(System.getProperty("user.home"), ".lodgebilling", "receipts.tsv"));
    }

    /** Uses the given save file (handy for testing). */
    public BillDao(Path file) {
        this.file = file;
        load();
    }

    /** Every saved bill; the list is observable so views can follow changes. */
    public ObservableList<Bill> findAll() {
        return bills;
    }

    /** Creates, stores and returns a new bill with the next number (R001, R002, ...). */
    public Bill save(BillInput input) {
        Bill bill = Bill.of(String.format("R%03d", nextReceiptNumber++), input);
        bills.add(bill);
        writeFile();
        return bill;
    }

    public void delete(Bill bill) {
        bills.remove(bill);
        writeFile();
    }

    /** The saved bill for this guest name (ignoring case and extra spaces), or null if none. */
    public Bill findByGuest(String name) {
        String wanted = normalise(name);
        for (Bill b : bills) {
            if (normalise(b.getGuestName()).equals(wanted)) {
                return b;
            }
        }
        return null;
    }

    private static String normalise(String name) {
        return name.trim().replaceAll("\\s+", " ").toLowerCase();
    }

    // ---------- Saving and loading ----------
    // One bill per line, fields separated by tabs:
    // receipt, issued at, name, room type, nights, extras, paid, notes

    private void load() {
        if (!Files.exists(file)) {
            return;
        }
        try {
            for (String line : Files.readAllLines(file, StandardCharsets.UTF_8)) {
                Bill bill = parse(line);
                if (bill != null) {
                    bills.add(bill);
                    nextReceiptNumber = Math.max(nextReceiptNumber, receiptNumberOf(bill) + 1);
                }
            }
        } catch (IOException e) {
            System.err.println("Could not read saved receipts: " + e.getMessage());
        }
    }

    /** Turns one saved line back into a bill, or null if the line is damaged. */
    private static Bill parse(String line) {
        if (line.isBlank()) {
            return null;
        }
        try {
            String[] f = line.split("\t", -1);
            return new Bill(f[0], unescape(f[2]), RoomType.valueOf(f[3]), Integer.parseInt(f[4]),
                    new BigDecimal(f[5]), new BigDecimal(f[6]), unescape(f[7]),
                    LocalDateTime.parse(f[1]));
        } catch (RuntimeException e) {
            System.err.println("Skipped a damaged line in the receipts file.");
            return null;
        }
    }

    /** R007 gives 7, so numbering carries on after the highest saved receipt. */
    private static int receiptNumberOf(Bill bill) {
        try {
            return Integer.parseInt(bill.getReceiptNumber().substring(1));
        } catch (RuntimeException e) {
            return 0;
        }
    }

    private void writeFile() {
        List<String> lines = new ArrayList<>();
        for (Bill b : bills) {
            lines.add(String.join("\t",
                    b.getReceiptNumber(),
                    b.getIssuedAt().toString(),
                    escape(b.getGuestName()),
                    b.getRoomType().name(),
                    String.valueOf(b.getNights()),
                    b.getExtras().toPlainString(),
                    b.getPaid().toPlainString(),
                    escape(b.getNotes())));
        }
        try {
            Files.createDirectories(file.getParent());
            // Write to a temporary file first, so a crash never leaves a half-written receipts file
            Path temp = file.resolveSibling(file.getFileName() + ".tmp");
            Files.write(temp, lines, StandardCharsets.UTF_8);
            Files.move(temp, file, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            System.err.println("Could not save receipts: " + e.getMessage());
        }
    }

    /** Keeps tabs and line breaks in names and notes from breaking the file layout. */
    private static String escape(String text) {
        return text.replace("\\", "\\\\").replace("\t", "\\t").replace("\n", "\\n").replace("\r", "\\r");
    }

    private static String unescape(String text) {
        StringBuilder out = new StringBuilder();
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c == '\\' && i + 1 < text.length()) {
                char next = text.charAt(++i);
                out.append(switch (next) {
                    case 't' -> '\t';
                    case 'n' -> '\n';
                    case 'r' -> '\r';
                    default -> next;
                });
            } else {
                out.append(c);
            }
        }
        return out.toString();
    }
}
