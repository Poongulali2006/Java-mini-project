import java.sql.*;
import java.util.Scanner;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class CurrencyConverter {

    private static final String DB_URL = "jdbc:mysql://localhost:3306/your_db";
    private static final String USER = "your_username";
    private static final String PASSWORD = "your_password";
    private static final ExecutorService executorService = Executors.newFixedThreadPool(5);

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);

        System.out.println("Enter from currency (e.g., USD):");
        String fromCurrency = scanner.nextLine().trim();
        System.out.println("Enter to currency (e.g., EUR):");
        String toCurrency = scanner.nextLine().trim();
        System.out.println("Enter amount:");
        double amount = scanner.nextDouble();

        // Fetch exchange rate and store in database
        fetchAndStoreExchangeRate(fromCurrency, toCurrency);

        // Fetch conversion rate from the database and perform the conversion
        double rate = getExchangeRateFromDB(fromCurrency, toCurrency);
        if (rate != -1) {
            double convertedAmount = amount * rate;
            System.out.printf("Converted amount: %.2f %s\n", convertedAmount, toCurrency);
        } else {
            System.out.println("Conversion rate not available.");
        }

        // Shutdown executor service
        try {
            executorService.shutdown();
            if (!executorService.awaitTermination(5, TimeUnit.SECONDS)) {
                executorService.shutdownNow();
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private static void fetchAndStoreExchangeRate(String fromCurrency, String toCurrency) {
        executorService.submit(() -> {
            try {
                double rate = fetchRateFromAPI(fromCurrency, toCurrency);
                saveExchangeRateToDB(fromCurrency, toCurrency, rate);
                System.out.printf("Fetched and saved rate for %s to %s: %.6f\n", fromCurrency, toCurrency, rate);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    private static double fetchRateFromAPI(String fromCurrency, String toCurrency) {
        // Replace this with actual API call logic. Here we use a random number for demonstration.
        return Math.random() * (1.5 - 0.5) + 0.5;
    }

    private static void saveExchangeRateToDB(String fromCurrency, String toCurrency, double rate) {
        String sql = "INSERT INTO exchange_rates (from_currency, to_currency, rate) VALUES (?, ?, ?)";
        try (Connection conn = DriverManager.getConnection(DB_URL, USER, PASSWORD);
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, fromCurrency);
            stmt.setString(2, toCurrency);
            stmt.setDouble(3, rate);
            stmt.executeUpdate();

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private static double getExchangeRateFromDB(String fromCurrency, String toCurrency) {
        String sql = "SELECT rate FROM exchange_rates WHERE from_currency = ? AND to_currency = ? ORDER BY timestamp DESC LIMIT 1";
        try (Connection conn = DriverManager.getConnection(DB_URL, USER, PASSWORD);
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, fromCurrency);
            stmt.setString(2, toCurrency);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return rs.getDouble("rate");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return -1; // Return -1 if rate not found
    }
}
