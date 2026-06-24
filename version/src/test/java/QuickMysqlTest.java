import java.sql.*;

public class QuickMysqlTest {
    public static void main(String[] args) {
        String url = "jdbc:mysql://127.0.0.1:3307/code_review?useSSL=false&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true&connectTimeout=5000";
        for (String pwd : new String[]{null, ""}) {
            try {
                Connection c = DriverManager.getConnection(url, "root", pwd);
                System.out.println("OK pwd=" + (pwd == null ? "null" : "empty"));
                c.close();
            } catch (SQLException e) {
                System.out.println("FAIL pwd=" + (pwd == null ? "null" : "empty") + ": " + e.getMessage());
            }
        }
    }
}