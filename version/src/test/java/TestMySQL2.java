import java.sql.*;

public class TestMySQL2 {
    public static void main(String[] args) throws Exception {
        // Try anonymous access
        String[] users = {"root", "codereview", "mysql", "Administrator"};
        String[] passwords = {"", "root", "codereview123"};
        for (String user : users) {
            for (String pwd : passwords) {
                try {
                    String url = "jdbc:mysql://127.0.0.1:3306/?connectTimeout=2000";
                    Connection c = DriverManager.getConnection(url, user, pwd);
                    System.out.println("SUCCESS: user='" + user + "' pwd='" + pwd + "'");
                    // Check what databases exist
                    DatabaseMetaData meta = c.getMetaData();
                    System.out.println("  Catalog: " + meta.getDatabaseProductName() + " " + meta.getDatabaseProductVersion());
                    c.close();
                } catch (SQLException e) {
                    System.out.println("FAIL user='" + user + "' pwd='" + pwd + "': " + e.getMessage());
                }
            }
        }
    }
}
