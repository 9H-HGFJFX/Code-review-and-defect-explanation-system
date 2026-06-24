import java.sql.*;

public class TestMySQL {
    public static void main(String[] args) {
        String[] passwords = {"", "root", "rootroot", "password", "codereview", "codereview123", "admin"};
        for (String pwd : passwords) {
            try {
                String url = "jdbc:mysql://127.0.0.1:3306/?connectTimeout=3000";
                DriverManager.setLoginTimeout(3);
                Connection c = DriverManager.getConnection(url, "root", pwd);
                System.out.println("SUCCESS with password: '" + pwd + "'");
                c.close();
                break;
            } catch (SQLException e) {
                System.out.println("FAIL '" + pwd + "': " + e.getMessage());
            }
        }
    }
}
