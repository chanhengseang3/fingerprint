package com.construction.db;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class DbConnector {

    private static final String DB_URL = "jdbc:sqlite:data.db";

    public static Connection connection;
    public static Statement statement;

    public DbConnector() {
        if (connection == null) {
            try {
                Class.forName("org.sqlite.JDBC");
                connection = DriverManager.getConnection(DB_URL);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        if (statement == null) {
            try {
                statement = connection.createStatement();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    public boolean insert(SubConstructor subConstructor) {
        String sql = String.format("insert into sub_constructor(id,base64) values(%s,'%s')", subConstructor.getId(), subConstructor.getBase64());
        try {
            statement.execute(sql);
            return true;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public SubConstructor getById(final String id) {
        String query = String.format("select * from sub_constructor where id = %s", id);
        try {
            ResultSet rs = statement.executeQuery(query);
            return new SubConstructor()
                    .setId(rs.getInt("id"))
                    .setBase64(rs.getString("base64"));
        } catch (SQLException e) {
            throw new RuntimeException("query error");
        }
    }

    public List<SubConstructor> getAll() {
        String query = "select * from sub_constructor";
        try {
            ResultSet rs = statement.executeQuery(query);
            List<SubConstructor> subConstructors = new ArrayList<>();
            while (rs.next()) {
                subConstructors.add(new SubConstructor()
                        .setId(rs.getInt("id"))
                        .setBase64(rs.getString("base64")));
            }
            return subConstructors;
        } catch (SQLException e) {
            e.printStackTrace();
            throw new RuntimeException("query all error");
        }
    }

    public boolean idExists(final String id) {
        String query = String.format("select * from sub_constructor where id = %s", id);
        try {
            ResultSet rs = statement.executeQuery(query);
            return rs.next();
        } catch (SQLException e) {
            throw new RuntimeException("query error");
        }
    }

    public boolean deleteById(final String id) {
        String sql = String.format("delete from sub_constructor where id = %s", id);
        try {
            statement.execute(sql);
            return true;
        } catch (SQLException e) {
            throw new RuntimeException("delete error");
        }
    }

    public boolean deleteAll() {
        String sql = "delete from sub_constructor";
        try {
            statement.execute(sql);
            return true;
        } catch (SQLException e) {
            throw new RuntimeException("delete error");
        }
    }

    public void close() {
        if (statement != null) {
            try {
                statement.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
        if (connection != null) {
            try {
                connection.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }
}
