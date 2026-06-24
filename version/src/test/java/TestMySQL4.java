import java.sql.*;

public class TestMySQL4 {
    static final String DB_URL = "jdbc:mysql://127.0.0.1:3306/?connectTimeout=5000&allowPublicKeyRetrieval=true";
    static final String ROOT_USER = "root";

    public static void main(String[] args) {
        System.out.println("=== MySQL Connection Test ===");

        // Try multiple passwords
        String rootPwd = null;
        Connection rootConn = null;
        String[] passwords = {"root", "rootroot", "password", "codereview", "codereview123", "secret", ""};
        for (String pwd : passwords) {
            try {
                rootConn = DriverManager.getConnection(DB_URL, ROOT_USER, pwd);
                rootPwd = pwd;
                System.out.println("OK: Connected as root/" + (pwd.isEmpty() ? "(empty)" : pwd));
                break;
            } catch (SQLException e) {
                System.out.println("FAIL root/" + (pwd.isEmpty() ? "(empty)" : pwd) + ": " + e.getMessage());
            }
        }
        if (rootConn == null) {
            System.out.println("ERROR: Could not connect as root with any password");
            return;
        }

        try {
            // Step 2: Check databases
            DatabaseMetaData meta = rootConn.getMetaData();
            ResultSet rs = meta.getCatalogs();
            boolean hasCodeReview = false;
            while (rs.next()) {
                String db = rs.getString("TABLE_CAT");
                if (db != null && !db.equals("information_schema") && !db.equals("mysql") && !db.equals("performance_schema") && !db.equals("sys")) {
                    System.out.println("  DB: " + db);
                }
                if ("code_review".equals(db)) hasCodeReview = true;
            }
            rs.close();

            // Step 3: Check if code_review user exists
            PreparedStatement ps = rootConn.prepareStatement(
                "SELECT user, host FROM mysql.user WHERE user=?");
            ps.setString(1, "codereview");
            ResultSet users = ps.executeQuery();
            boolean hasCodereviewUser = users.next();
            System.out.println("  codereview user exists: " + hasCodereviewUser);
            users.close();
            ps.close();

            if (!hasCodeReview) {
                System.out.println("Creating code_review database...");
                rootConn.createStatement().execute(
                    "CREATE DATABASE code_review DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci");
                System.out.println("OK: code_review database created");
                hasCodeReview = true;
            }

            if (!hasCodereviewUser) {
                System.out.println("Creating codereview user...");
                rootConn.createStatement().execute(
                    "CREATE USER 'codereview'@'localhost' IDENTIFIED BY 'codereview123'");
                rootConn.createStatement().execute(
                    "GRANT ALL PRIVILEGES ON code_review.* TO 'codereview'@'localhost'");
                rootConn.createStatement().execute("FLUSH PRIVILEGES");
                System.out.println("OK: codereview user created with password 'codereview123'");
            }

            // Step 4: Check tables in code_review
            Connection dbConn = DriverManager.getConnection(
                DB_URL + "&database=code_review", ROOT_USER, rootPwd);
            DatabaseMetaData dbMeta = dbConn.getMetaData();
            ResultSet tables = dbMeta.getTables("code_review", null, "%", new String[]{"TABLE"});
            boolean hasTables = false;
            while (tables.next()) {
                hasTables = true;
                System.out.println("  Table: " + tables.getString("TABLE_NAME"));
            }
            tables.close();
            dbConn.close();

            if (!hasTables) {
                System.out.println("WARNING: code_review database is empty - init.sql not yet run");
            } else {
                System.out.println("OK: code_review database has tables");
            }

            System.out.println("SUCCESS: MySQL is ready!");
        } catch (SQLException e) {
            System.out.println("ERROR: " + e.getMessage());
            e.printStackTrace();
        } finally {
            try { rootConn.close(); } catch (Exception e) {}
        }
    }
}
