import java.sql.*;

public class TestMySQL3 {
    public static void main(String[] args) throws Exception {
        String[] combos = {
            "codereview", "codereview123",
            "codereview", "",
            "root", "",
            "root", "root",
        };
        for (int i = 0; i < combos.length; i += 2) {
            String user = combos[i];
            String pwd = combos[i + 1];
            try {
                String url = "jdbc:mysql://127.0.0.1:3306/?connectTimeout=3000&useSSL=false&allowPublicKeyRetrieval=true";
                Connection c = DriverManager.getConnection(url, user, pwd);
                System.out.println("SUCCESS: user='" + user + "'");
                DatabaseMetaData meta = c.getMetaData();
                System.out.println("  MySQL version: " + meta.getDatabaseProductName() + " " + meta.getDatabaseProductVersion());
                // Check if code_review exists
                ResultSet rs = c.getMetaData().getCatalogs();
                while (rs.next()) {
                    String cat = rs.getString("TABLE_CAT");
                    if (cat != null && cat.contains("code")) System.out.println("  DB: " + cat);
                }
                c.close();
                break;
            } catch (SQLException e) {
                System.out.println("FAIL user='" + user + "' pwd='" + pwd + "': " + e.getMessage());
            }
        }
    }
}
