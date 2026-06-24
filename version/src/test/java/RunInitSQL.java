import java.sql.*;
import java.nio.file.*;
import java.util.regex.*;

public class RunInitSQL {
    static final String ADMIN_URL = "jdbc:mysql://127.0.0.1:3307/?connectTimeout=5000&allowPublicKeyRetrieval=true";
    static final String USER_URL  = "jdbc:mysql://127.0.0.1:3307/code_review?connectTimeout=5000&allowPublicKeyRetrieval=true";

    public static void main(String[] args) throws Exception {
        String raw = Files.readString(Path.of("E:/Desktop/version/init.sql"));

        try (Connection rootConn = DriverManager.getConnection(ADMIN_URL, "root", "")) {
            System.out.println("Connected to MySQL 3307");
            rootConn.createStatement().execute("USE code_review");
            rootConn.createStatement().execute("SET FOREIGN_KEY_CHECKS=0");

            // Drop all tables (ignore errors)
            rootConn.createStatement().execute("DROP TABLE IF EXISTS issue, class_user, review_task, rule, user, class");

            // Parse SQL into statements (strip line comments, join multi-line, split on ;)
            String cleaned = raw
                .replaceAll("--[^\n]*", "")   // strip -- comments
                .replaceAll("\r\n", "\n")
                .replaceAll("\r", "\n");

            // Split carefully - only on standalone ;
            String[] parts = cleaned.split(";");
            int ok = 0, warn = 0;
            for (String rawStmt : parts) {
                String s = rawStmt.trim();
                if (s.isEmpty()) continue;
                if (s.toUpperCase().startsWith("CREATE DATABASE") || s.toUpperCase().startsWith("SET FOREIGN_KEY_CHECKS")) continue;
                try {
                    rootConn.createStatement().execute(s);
                    ok++;
                    if (s.toUpperCase().startsWith("CREATE TABLE")) System.out.println("  + " + s.substring(13, Math.min(s.indexOf("("), 40)));
                } catch (SQLException e) {
                    warn++;
                    String msg = e.getMessage().split("\n")[0];
                    if (msg.contains("already exists") || msg.contains("doesn't exist")) {
                        // skip table existence noise
                    } else {
                        System.out.println("  WARN: " + msg);
                    }
                }
            }
            rootConn.createStatement().execute("SET FOREIGN_KEY_CHECKS=1");
            System.out.println("Done: " + ok + " ok, " + warn + " warnings");

            // List tables
            ResultSet rs = rootConn.getMetaData().getTables("code_review", null, "%", new String[]{"TABLE"});
            System.out.println("Tables:");
            while (rs.next()) System.out.println("  " + rs.getString("TABLE_NAME"));
            rs.close();

            // Test user
            try {
                DriverManager.setLoginTimeout(5);
                Connection u = DriverManager.getConnection(USER_URL, "codereview", "codereview123");
                System.out.println("codereview/codereview123: OK");
                u.close();
            } catch (SQLException e) {
                System.out.println("codereview/codereview123: FAIL - " + e.getMessage());
            }
        }
    }
}
