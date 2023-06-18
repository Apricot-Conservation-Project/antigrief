package main;

import arc.util.Log;
import arc.util.Nullable;

import java.sql.*;
import java.util.HashMap;

public class DBInterface {
    public Connection conn = null;

    private PreparedStatement preparedStatement = null;

    public void connect(String db, String username, String password) {
        // SQLite connection string
        String url = "jdbc:mysql://127.0.0.1:3306/" + db + "?useSSL=false";
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            conn = DriverManager.getConnection(url, username, password);
            Log.info("Connected to database successfully");
        } catch (SQLException | ClassNotFoundException e) {
            Log.err(e);
        }
    }

    public void addEmptyRow(String table, String key, Object val) {
        addEmptyRow(table, new String[] { key }, new Object[] { val });
    }

    public void addEmptyRow(String table, String keys[], Object vals[]) {
        String sql = "INSERT INTO " + table + " (";
        String keyString = "";
        String valString = "";
        for (int i = 0; i < keys.length; i++) {
            keyString += keys[i] + (i < keys.length - 1 ? ", " : "");
            valString += vals[i] + (i < keys.length - 1 ? ", " : "");
        }
        sql += keyString + ") VALUES(" + valString + ")";
        try {
            preparedStatement = conn.prepareStatement(sql);
            preparedStatement.executeUpdate();
        } catch (SQLException e) {
            Log.err(e);
        }
    }

    @Nullable
    public HashMap<String, Object> loadRow(String table, String key, Object val) {
        return loadRow(table, new String[] { key }, new Object[] { val });

    }

    @Nullable
    public HashMap<String, Object> loadRow(String table, String keys[], Object vals[]) {
        HashMap<String, Object> returnedVals = new HashMap<String, Object>();

        // if (!hasRow(table, keys, vals))
        //     addEmptyRow(table, keys, vals);
        String sql = "SELECT * FROM " + table + " WHERE ";
        for (int i = 0; i < keys.length; i++) {
            sql += keys[i] + " = " + vals[i] + (i < keys.length - 1 ? " AND " : "");
        }
        try {
            preparedStatement = conn.prepareStatement(sql);
            ResultSet rs = preparedStatement.executeQuery();
            if (rs.next() == false){
                return null;
            };
            ResultSetMetaData rsmd = rs.getMetaData();
            for (int i = 1; i <= rsmd.getColumnCount(); i++) { // ONE INDEXED? REALLY?
                returnedVals.put(rsmd.getColumnName(i), rs.getObject(rsmd.getColumnName(i)));
            }
            rs.close();
        } catch (SQLException e) {
            Log.err(e);
        }

        return returnedVals;
    }
}