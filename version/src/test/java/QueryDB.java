import java.sql.*;
import java.util.*;
import java.io.*;

public class QueryDB {
    static final String URL = "jdbc:mysql://127.0.0.1:3307/code_review?useSSL=false&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true&connectTimeout=5000";
    static Connection c;

    static List<Map<String,String>> query(String sql) throws Exception {
        Statement s = c.createStatement();
        ResultSet rs = s.executeQuery(sql);
        ResultSetMetaData md = rs.getMetaData();
        int n = md.getColumnCount();
        List<Map<String,String>> rows = new ArrayList<>();
        while (rs.next()) {
            Map<String,String> row = new LinkedHashMap<>();
            for (int i = 1; i <= n; i++) row.put(md.getColumnLabel(i), rs.getString(i));
            rows.add(row);
        }
        rs.close(); s.close();
        return rows;
    }

    public static void main(String[] args) throws Exception {
        c = DriverManager.getConnection(URL, "root", null);

        List<Map<String,String>> classes = query("SELECT id,name,teacher_id,IFNULL(description,'') AS description FROM class ORDER BY id");
        List<Map<String,String>> rules = query("SELECT id,rule_id,name,description,category,severity,IFNULL(languages,'') AS languages,IFNULL(pattern,'') AS pattern,enabled,priority FROM review_rule ORDER BY priority");
        List<Map<String,String>> tasks = query("SELECT id,title,IFNULL(description,'') AS description,class_id,submitter_id,status,IFNULL(deadline,'') AS deadline,IFNULL(create_time,'') AS create_time FROM review_task ORDER BY id");

        PrintWriter pw = new PrintWriter(new OutputStreamWriter(new FileOutputStream("E:\\Desktop\\version\\db_data.json"), "UTF-8"));
        pw.println("{\"classes\":" + toJson(classes) + ",\"rules\":" + toJson(rules) + ",\"tasks\":" + toJson(tasks) + "}");
        pw.close();
        System.out.println("Saved db_data.json (" + classes.size() + " classes, " + rules.size() + " rules, " + tasks.size() + " tasks)");
        c.close();
    }

    static String toJson(List<Map<String,String>> rows) {
        StringBuilder sb = new StringBuilder("[");
        boolean first = true;
        for (Map<String,String> row : rows) {
            if (!first) sb.append(",");
            first = false;
            sb.append("{");
            boolean firstField = true;
            for (Map.Entry<String,String> e : row.entrySet()) {
                if (!firstField) sb.append(",");
                firstField = false;
                sb.append("\"").append(e.getKey().toLowerCase()).append("\":");
                String v = e.getValue();
                if (v == null) sb.append("null");
                else sb.append("\"").append(v.replace("\\","\\\\").replace("\"","\\\"")).append("\"");
            }
            sb.append("}");
        }
        sb.append("]");
        return sb.toString();
    }
}