import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;

public class CreateLogsTable {
    public static void main(String[] args) {
        String url = "jdbc:sqlite:../tukac.db";
        try (Connection conn = DriverManager.getConnection(url);
             Statement stmt = conn.createStatement()) {
            stmt.execute("CREATE TABLE IF NOT EXISTS activity_logs (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "user_id INTEGER, " +
                    "user_name TEXT, " +
                    "action TEXT, " +
                    "details TEXT, " +
                    "ip_address TEXT, " +
                    "timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP);");
            System.out.println("Table activity_logs created successfully.");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
