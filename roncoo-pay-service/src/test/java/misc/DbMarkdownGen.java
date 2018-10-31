package misc;

import java.io.IOException;
import java.sql.*;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

/**
 * db结构转markdown说明
 */
public class DbMarkdownGen {

    public static void main(String[] args) throws IOException, ClassNotFoundException {
        Properties prop = new Properties();
        prop.load(DbMarkdownGen.class.getClassLoader().getResourceAsStream("jdbc.properties"));
        Class.forName(prop.getProperty("jdbc.driver"));
        Connection conn = null;
        Statement stmt = null;
        ResultSet rs = null;
        try {
            conn = DriverManager.getConnection(prop.getProperty("jdbc.url") + "&useInformationSchema=true ", prop.getProperty("jdbc.username"), prop.getProperty("jdbc.password"));
            stmt = conn.createStatement();

            Map<String, String> tables = new HashMap<>();
            String[] types = {"TABLE"};
//            System.out.println(conn.getCatalog());
            rs = conn.getMetaData().getTables(null, null, "%", types);
            while (rs.next()) {
                tables.put(rs.getString("TABLE_NAME"), rs.getString("REMARKS"));
            }
            rs.close();

//            xx表
//            | 字段名 | 字段类型 | 备注 |
//            | :----: | :------: | :--: |
//            |        |          |      |
            for (String tableName : tables.keySet()) {
                rs = conn.getMetaData().getColumns(conn.getCatalog(), conn.getCatalog(), tableName, "%");
                System.out.println(tableName + ": " + tables.get(tableName));
                System.out.println("| 字段名 | 字段类型 | 备注 |");
                System.out.println("| :----: | :------: | :--: |");
                while (rs.next()) {
                    String columnName = rs.getString("COLUMN_NAME");
                    String columnType = rs.getString("TYPE_NAME");
                    int columnSize = rs.getInt("COLUMN_SIZE");
                    String comment = rs.getString("REMARKS");
//                    System.out.println(columnName + ":" + columnType + "(" + columnSize + "):" + comment);
                    System.out.println("|" + columnName + "|" + columnType + "(" + columnSize + ")|" + comment + "|");
//                    ResultSetMetaData rsmd = rs.getMetaData();
//                    for (int i = 1; i <= rsmd.getColumnCount(); i++) {
//                        System.out.println(rsmd.getColumnName(i) + ":" + rs.getString(i));
//                    }
                }
                rs.close();
                System.out.println("");
            }


        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            if (rs != null) {
                try {
                    rs.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
            if (stmt != null) {
                try {
                    stmt.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
            if (conn != null) {
                try {
                    conn.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
