import java.sql.*;

public class TestMySQL5 {
    static final String BASE_URL = "jdbc:mysql://127.0.0.1:3306/?connectTimeout=5000&allowPublicKeyRetrieval=true";
    static final String[] users = {"root", "codereview", "book_user"};
    static final String[] passwords = {"", "root", "rootroot", "password", "codereview", "codereview123", "secret"};

    public static void main(String[] args) {
        System.out.println("Testing all user/password combinations over TCP...");
        for (String user : users) {
            for (String pwd : passwords) {
                try {
                    Connection c = DriverManager.getConnection(BASE_URL, user, pwd);
                    System.out.println("SUCCESS: " + user + "/" + (pwd.isEmpty() ? "(empty)" : pwd));
                    // Check DB
                    DatabaseMetaData m = c.getMetaData();
                    ResultSet rs = m.getCatalogs();
                    while (rs.next()) {
                        String db = rs.getString("TABLE_CAT");
                        if (db != null && !db.equals("information_schema") && !db.equals("mysql") && !db.equals("performance_schema") && !db.equals("sys")) {
                            System.out.println("  DB: " + db);
                        }
                    }
                    c.close();
                    System.out.println("DONE - found working credentials!");
                    return;
                } catch (SQLException e) {
                    // Only print first failure per user
                    if (pwd.equals(passwords[0])) {
                        System.out.println("FAIL " + user + "/" + (pwd.isEmpty() ? "(empty)" : pwd) + ": " + e.getMessage());
                    }
                }
            }
        }
        System.out.println("No working credentials found.");
    }
}
