package server;

import java.sql.SQLException;

public interface DataBase {
    String getNicknameByLoginAndPassword(String login, String password) throws SQLException, ClassNotFoundException;
    boolean registration(String login, String password, String nickname) throws SQLException, ClassNotFoundException;
}
