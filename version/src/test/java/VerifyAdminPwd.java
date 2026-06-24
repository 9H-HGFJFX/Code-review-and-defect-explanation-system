import java.sql.*;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

public class VerifyAdminPwd {
    public static void main(String[] args) throws Exception {
        String url = "jdbc:mysql://127.0.0.1:3307/code_review?useSSL=false&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true&connectTimeout=5000";
        Connection c = DriverManager.getConnection(url, "root", null);
        PreparedStatement ps = c.prepareStatement("SELECT password_hash FROM user WHERE username=?");
        ps.setString(1, "admin");
        ResultSet rs = ps.executeQuery();
        if (rs.next()) {
            String hash = rs.getString(1);
            System.out.println("Hash: " + hash);
            BCryptPasswordEncoder enc = new BCryptPasswordEncoder(12);
            System.out.println("admin123 match: " + enc.matches("admin123", hash));
            System.out.println("admin match: " + enc.matches("admin", hash));
        } else {
            System.out.println("admin not found");
        }
        c.close();
    }
}