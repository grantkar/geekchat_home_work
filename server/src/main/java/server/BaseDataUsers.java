package server;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class BaseDataUsers implements DataBase {
    private Connection connection;
    private Statement statement;
    private SimpleAuthService simpleAuthService;
    private ResultSet rs;





    private void connect() throws ClassNotFoundException, SQLException {
        Class.forName("org.sqlite.JDBC");
        connection = DriverManager.getConnection("jdbc:sqlite:mybase.db");
        statement = connection.createStatement();
    }

    private void disconnect(){
        try {
            statement.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        try {
            connection.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }


    @Override
    public String getNicknameByLoginAndPassword(String login, String password) throws SQLException, ClassNotFoundException {
        String nickname;
        connect();
        ResultSet rs = statement.executeQuery("SELECT nickname FROM usersData WHERE login = '" + login +
                "' AND password = '" + password + "' ;");
         nickname = rs.getString("nickname");
        disconnect();

        return nickname;
    }

    @Override
    public boolean registration(String login, String password, String nickname) throws SQLException, ClassNotFoundException {
        connect();
        ResultSet rs = statement.executeQuery("SELECT login,password,nickname FROM usersData;");
        while (rs.next()){
            if (rs.getString("login").equals(login)||rs.getString("nickname").equals(nickname)){
                return false;
            }
        }
        String str = String.format("INSERT INTO usersData ( login, password, nickname) VALUES ('%s','%s','%s')",
                login,password,nickname);
        statement.executeUpdate(str);
        return true;
    }


}
