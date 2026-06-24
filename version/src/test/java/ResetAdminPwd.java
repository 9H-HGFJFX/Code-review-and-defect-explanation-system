import java.sql.*;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

public class ResetAdminPwd {
    public static void main(String[] args) throws Exception {
        String url = "jdbc:mysql://127.0.0.1:3307/code_review?useSSL=false&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true&connectTimeout=5000";
        Connection c = DriverManager.getConnection(url, "root", null);
        BCryptPasswordEncoder enc = new BCryptPasswordEncoder(12);
        String hash = enc.encode("admin123");
        System.out.println("New hash: " + hash);
        PreparedStatement ps = c.prepareStatement("UPDATE user SET password_hash=? WHERE username=?");
        ps.setString(1, hash);
        ps.setString(2, "admin");
        int n = ps.executeUpdate();
        System.out.println("Updated " + n + " rows");
        c.close();
    }
}