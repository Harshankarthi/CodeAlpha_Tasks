import java.io.*;
import java.nio.file.*;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;


public class HotelReservationSystem {
    // file names
    private static final String ROOMS_FILE = "rooms.csv";
    private static final String RESERVATIONS_FILE = "reservations.csv";
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ISO_LOCAL_DATE;

    public static void main(String[] args) {
        ReservationManager manager = new ReservationManager(ROOMS_FILE, RESERVATIONS_FILE);
        manager.loadData(); // create sample data if needed

        Scanner sc = new Scanner(System.in);
        boolean running = true;

        System.out.println("Welcome to the Hotel Reservation System (Console)");
        while (running) {
            System.out.println("\nChoose an option:");
            System.out.println("1) Search available rooms");
            System.out.println("2) Book a room");
            System.out.println("3) Cancel a reservation");
            System.out.println("4) View reservations");
            System.out.println("5) View all rooms");
            System.out.println("6) Exit");
            System.out.print("Enter choice: ");
            String choice = sc.nextLine().trim();

            try {
                switch (choice) {
                    case "1":
                        handleSearch(manager, sc);
                        break;
                    case "2":
                        handleBooking(manager, sc);
                        break;
                    case "3":
                        handleCancel(manager, sc);
                        break;
                    case "4":
                        handleViewReservations(manager, sc);
                        break;
                    case "5":
                        manager.printAllRooms();
                        break;
                    case "6":
                        running = false;
                        System.out.println("Goodbye!");
                        break;
                    default:
                        System.out.println("Invalid choice. Try again.");
                }
            } catch (Exception e) {
                System.out.println("Error: " + e.getMessage());
            }
        }

        sc.close();
    }

    private static void handleSearch(ReservationManager manager, Scanner sc) {
        System.out.println("--- Search Available Rooms ---");
        LocalDate checkIn = readDate(sc, "Enter check-in date (YYYY-MM-DD): ");
        LocalDate checkOut = readDate(sc, "Enter check-out date (YYYY-MM-DD): ");
        if (!checkOut.isAfter(checkIn)) {
            System.out.println("Check-out must be after check-in.");
            return;
        }
        System.out.print("Filter by room type (Standard/Deluxe/Suite) or press ENTER for any: ");
        String type = sc.nextLine().trim();
        if (type.isEmpty()) type = null;

        List<Room> avail = manager.searchAvailable(checkIn, checkOut, type);
        if (avail.isEmpty()) {
            System.out.println("No rooms available for the selected dates and filter.");
        } else {
            System.out.println("Available rooms:");
            for (Room r : avail) {
                long nights = Duration.between(checkIn.atStartOfDay(), checkOut.atStartOfDay()).toDays();
                double total = nights * r.pricePerNight;
                System.out.printf("RoomID: %s | Type: %s | Price/night: %.2f | Total (for %d nights): %.2f%n",
                        r.id, r.type, r.pricePerNight, nights, total);
            }
        }
    }

    private static void handleBooking(ReservationManager manager, Scanner sc) {
        System.out.println("--- Book a Room ---");
        LocalDate checkIn = readDate(sc, "Enter check-in date (YYYY-MM-DD): ");
        LocalDate checkOut = readDate(sc, "Enter check-out date (YYYY-MM-DD): ");
        if (!checkOut.isAfter(checkIn)) {
            System.out.println("Check-out must be after check-in.");
            return;
        }
        System.out.print("Filter by room type (Standard/Deluxe/Suite) or press ENTER for any: ");
        String type = sc.nextLine().trim();
        if (type.isEmpty()) type = null;

        List<Room> avail = manager.searchAvailable(checkIn, checkOut, type);
        if (avail.isEmpty()) {
            System.out.println("No available rooms.");
            return;
        }
        System.out.println("Available rooms:");
        for (Room r : avail) {
            System.out.printf("- %s : %s (%.2f per night)%n", r.id, r.type, r.pricePerNight);
        }
        System.out.print("Enter RoomID to book: ");
        String roomId = sc.nextLine().trim();

        Optional<Room> chosen = avail.stream().filter(r -> r.id.equalsIgnoreCase(roomId)).findFirst();
        if (!chosen.isPresent()) {
            System.out.println("Invalid RoomID or room not available.");
            return;
        }
        System.out.print("Enter guest name: ");
        String guestName = sc.nextLine().trim();

        long nights = Duration.between(checkIn.atStartOfDay(), checkOut.atStartOfDay()).toDays();
        double total = nights * chosen.get().pricePerNight;
        System.out.printf("Total price for %d nights: %.2f%n", nights, total);

        PaymentSimulator payment = new PaymentSimulator();
        System.out.print("Enter (simulated) card number to pay (any digits) or press ENTER to cancel: ");
        String card = sc.nextLine().trim();
        if (card.isEmpty()) {
            System.out.println("Booking cancelled by user.");
            return;
        }
        boolean paid = payment.processPayment(card, total);
        if (!paid) {
            System.out.println("Payment failed. Booking not completed.");
            return;
        }

        Reservation res = manager.createReservation(guestName, chosen.get(), checkIn, checkOut, total);
        if (res != null) {
            System.out.printf("Booking successful. Reservation ID: %s%n", res.id);
            System.out.println("Use this ID to cancel or view your reservation.");
        } else {
            System.out.println("Failed to create reservation. It might have been taken just now.");
        }
    }

    private static void handleCancel(ReservationManager manager, Scanner sc) {
        System.out.println("--- Cancel Reservation ---");
        System.out.print("Enter Reservation ID: ");
        String id = sc.nextLine().trim();
        boolean ok = manager.cancelReservation(id);
        if (ok) {
            System.out.println("Reservation cancelled successfully.");
        } else {
            System.out.println("Reservation not found or already cancelled.");
        }
    }

    private static void handleViewReservations(ReservationManager manager, Scanner sc) {
        System.out.println("--- View Reservations ---");
        System.out.println("1) View all reservations");
        System.out.println("2) View by guest name");
        System.out.println("3) View by reservation ID");
        System.out.print("Choice: ");
        String c = sc.nextLine().trim();
        switch (c) {
            case "1":
                manager.printAllReservations();
                break;
            case "2":
                System.out.print("Enter guest name: ");
                String name = sc.nextLine().trim();
                manager.printReservationsByGuest(name);
                break;
            case "3":
                System.out.print("Enter reservation ID: ");
                String id = sc.nextLine().trim();
                manager.printReservationById(id);
                break;
            default:
                System.out.println("Invalid option.");
        }
    }

    private static LocalDate readDate(Scanner sc, String prompt) {
        while (true) {
            System.out.print(prompt);
            String line = sc.nextLine().trim();
            try {
                return LocalDate.parse(line, DATE_FMT);
            } catch (Exception e) {
                System.out.println("Invalid date. Use format YYYY-MM-DD.");
            }
        }
    }
}

/* -------------------------
   Domain classes & manager
   ------------------------- */

class Room {
    String id;
    String type; // Standard, Deluxe, Suite
    double pricePerNight;

    Room(String id, String type, double pricePerNight) {
        this.id = id;
        this.type = type;
        this.pricePerNight = pricePerNight;
    }

    static Room fromCsv(String line) {
        // roomId,roomType,pricePerNight
        String[] parts = line.split(",", -1);
        return new Room(parts[0], parts[1], Double.parseDouble(parts[2]));
    }

    String toCsv() {
        return String.join(",", id, type, Double.toString(pricePerNight));
    }
}

class Reservation {
    String id; // UUID
    String guestName;
    String roomId;
    String roomType;
    LocalDate checkIn;
    LocalDate checkOut;
    double totalPrice;
    String status; // ACTIVE or CANCELLED

    Reservation(String id, String guestName, String roomId, String roomType, LocalDate checkIn, LocalDate checkOut, double totalPrice, String status) {
        this.id = id;
        this.guestName = guestName;
        this.roomId = roomId;
        this.roomType = roomType;
        this.checkIn = checkIn;
        this.checkOut = checkOut;
        this.totalPrice = totalPrice;
        this.status = status;
    }

    static Reservation fromCsv(String line) {
        // id,guestName,roomId,roomType,checkIn,checkOut,totalPrice,status
        String[] p = line.split(",", -1);
        return new Reservation(
                p[0],
                p[1],
                p[2],
                p[3],
                LocalDate.parse(p[4]),
                LocalDate.parse(p[5]),
                Double.parseDouble(p[6]),
                p[7]
        );
    }

    String toCsv() {
        return String.join(",",
                id,
                guestName,
                roomId,
                roomType,
                checkIn.toString(),
                checkOut.toString(),
                Double.toString(totalPrice),
                status
        );
    }

    boolean overlaps(LocalDate start, LocalDate end) {
        // reservation occupies nights [checkIn, checkOut)
        // overlap exists if max(start, checkIn) < min(end, checkOut)
        LocalDate maxStart = checkIn.isAfter(start) ? checkIn : start;
        LocalDate minEnd = checkOut.isBefore(end) ? checkOut : end;
        return maxStart.isBefore(minEnd);
    }
}

class ReservationManager {
    private Map<String, Room> rooms = new HashMap<>();
    private Map<String, Reservation> reservations = new HashMap<>();
    private final String roomsFile;
    private final String reservationsFile;

    ReservationManager(String roomsFile, String reservationsFile) {
        this.roomsFile = roomsFile;
        this.reservationsFile = reservationsFile;
    }

    void loadData() {
        try {
            loadRooms();
            loadReservations();
        } catch (Exception e) {
            System.out.println("Error loading data: " + e.getMessage());
        }
    }

    private void loadRooms() throws IOException {
        Path p = Paths.get(roomsFile);
        if (!Files.exists(p)) {
            createSampleRooms();
            return;
        }
        List<String> lines = Files.readAllLines(p);
        for (String line : lines) {
            if (line.trim().isEmpty()) continue;
            Room r = Room.fromCsv(line);
            rooms.put(r.id, r);
        }
    }

    private void createSampleRooms() throws IOException {
        // create sample rooms (id, type, price)
        List<Room> sample = Arrays.asList(
                new Room("R101", "Standard", 3000),
                new Room("R102", "Standard", 3000),
                new Room("R201", "Deluxe", 4500),
                new Room("R202", "Deluxe", 4500),
                new Room("S301", "Suite", 9000)
        );
        for (Room r : sample) rooms.put(r.id, r);
        saveRooms();
        System.out.println("Sample rooms created in " + roomsFile);
    }

    private void saveRooms() throws IOException {
        List<String> lines = new ArrayList<>();
        for (Room r : rooms.values()) lines.add(r.toCsv());
        Files.write(Paths.get(roomsFile), lines, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
    }

    private void loadReservations() throws IOException {
        Path p = Paths.get(reservationsFile);
        if (!Files.exists(p)) {
            // create empty file
            Files.write(p, new byte[0], StandardOpenOption.CREATE);
            return;
        }
        List<String> lines = Files.readAllLines(p);
        for (String line : lines) {
            if (line.trim().isEmpty()) continue;
            Reservation r = Reservation.fromCsv(line);
            reservations.put(r.id, r);
        }
    }

    private synchronized void saveReservations() {
        try {
            List<String> lines = new ArrayList<>();
            for (Reservation r : reservations.values()) lines.add(r.toCsv());
            Files.write(Paths.get(reservationsFile), lines, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        } catch (IOException e) {
            System.out.println("Failed to save reservations: " + e.getMessage());
        }
    }

    List<Room> searchAvailable(LocalDate checkIn, LocalDate checkOut, String typeFilter) {
        List<Room> result = new ArrayList<>();
        for (Room room : rooms.values()) {
            if (typeFilter != null && !typeFilter.isEmpty() && !room.type.equalsIgnoreCase(typeFilter)) continue;
            boolean occupied = false;
            for (Reservation res : reservations.values()) {
                if (!res.status.equalsIgnoreCase("ACTIVE")) continue;
                if (!res.roomId.equals(room.id)) continue;
                if (res.overlaps(checkIn, checkOut)) {
                    occupied = true;
                    break;
                }
            }
            if (!occupied) result.add(room);
        }
        // sort by price then id
        result.sort(Comparator.comparingDouble((Room r) -> r.pricePerNight).thenComparing(r -> r.id));
        return result;
    }

    Reservation createReservation(String guestName, Room room, LocalDate checkIn, LocalDate checkOut, double totalPrice) {
        // Re-check availability to avoid race conditions
        List<Room> avail = searchAvailable(checkIn, checkOut, room.type);
        boolean stillAvailable = avail.stream().anyMatch(r -> r.id.equals(room.id));
        if (!stillAvailable) return null;

        String id = UUID.randomUUID().toString();
        Reservation r = new Reservation(id, guestName, room.id, room.type, checkIn, checkOut, totalPrice, "ACTIVE");
        reservations.put(id, r);
        saveReservations();
        return r;
    }

    boolean cancelReservation(String reservationId) {
        Reservation r = reservations.get(reservationId);
        if (r == null) return false;
        if (!r.status.equalsIgnoreCase("ACTIVE")) return false;
        r.status = "CANCELLED";
        saveReservations();
        return true;
    }

    void printAllRooms() {
        System.out.println("--- Rooms ---");
        List<Room> list = new ArrayList<>(rooms.values());
        list.sort(Comparator.comparing(r -> r.id));
        for (Room r : list) {
            System.out.printf("RoomID: %s | Type: %s | Price/night: %.2f%n", r.id, r.type, r.pricePerNight);
        }
    }

    void printAllReservations() {
        System.out.println("--- All Reservations ---");
        if (reservations.isEmpty()) {
            System.out.println("No reservations.");
            return;
        }
        List<Reservation> list = new ArrayList<>(reservations.values());
        list.sort(Comparator.comparing(r -> r.checkIn));
        for (Reservation r : list) printReservationSummary(r);
    }

    void printReservationsByGuest(String guestName) {
        System.out.println("--- Reservations for: " + guestName + " ---");
        boolean found = false;
        for (Reservation r : reservations.values()) {
            if (r.guestName.equalsIgnoreCase(guestName)) {
                printReservationSummary(r);
                found = true;
            }
        }
        if (!found) System.out.println("No reservations found for that guest.");
    }

    void printReservationById(String id) {
        Reservation r = reservations.get(id);
        if (r == null) {
            System.out.println("No reservation with ID: " + id);
        } else {
            printReservationSummary(r);
        }
    }

    private void printReservationSummary(Reservation r) {
        System.out.printf("ID: %s | Guest: %s | Room: %s (%s) | %s -> %s | Total: %.2f | Status: %s%n",
                r.id, r.guestName, r.roomId, r.roomType, r.checkIn, r.checkOut, r.totalPrice, r.status);
    }
}

class PaymentSimulator {
    private final Random rnd = new Random();

    /**
     * Simulate a payment. This is NOT real payment processing.
     * We'll simulate a small chance of failure.
     *
     * @param cardNumber dummy input
     * @param amount     amount to charge
     * @return true if "payment succeeded"
     */
    boolean processPayment(String cardNumber, double amount) {
        // Basic validation of card digits length
        if (cardNumber == null || !cardNumber.matches("\\d{6,19}")) {
            System.out.println("Card number appears invalid (must be 6-19 digits).");
            return false;
        }
        System.out.printf("Processing simulated payment of %.2f...%n", amount);
        // Simulate processing delay (no Thread.sleep cause blocking in console is okay but small)
        try { Thread.sleep(600); } catch (InterruptedException ignored) {}
        // 90% success rate
        boolean success = rnd.nextDouble() < 0.9;
        if (success) System.out.println("Payment approved.");
        else System.out.println("Payment declined by simulated gateway.");
        return success;
    }
}
