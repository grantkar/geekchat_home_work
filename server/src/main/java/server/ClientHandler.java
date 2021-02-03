package server;

import commands.Command;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.sql.SQLException;

public class ClientHandler {
    private Server server;
    private Socket socket;
    private DataInputStream in;
    private DataOutputStream out;
    private String nickname;
    private String login;

    public ClientHandler(Server server, Socket socket) {
        try {
            this.server = server;
            this.socket = socket;
            in = new DataInputStream(socket.getInputStream());
            out = new DataOutputStream(socket.getOutputStream());

            new Thread(() -> {
                try {

                    //цикл аутентификации

                    socket.setSoTimeout(120000);   //  Включаем таймер на выброс ошибки SocketTimeoutException в случае бездействия соккета 120сек
                    while (true) {
                        String str = in.readUTF();

                        if (str.startsWith(Command.AUTH)) {
                            String[] token = str.split("\\s");
                           // String newNick = server.getAuthService()
                           //         .getNicknameByLoginAndPassword(token[1], token[2]);

                            /*
                            Отправляю в базу данных на SELECT никнейма по ПАРАМЕТРАМ логин и пароль в полной
                             аналогии с таким же методом который мы писали выше, и так же выскакивает ошибка
                             EOFException (client.Controller.lambda$connect$3(Controller.java:103) в in.readUTF() в
                             Controllere
                             */
                             String newNick = server.getDataBaseAuth().getNicknameByLoginAndPassword(token[1], token[2]);

                            login = token[1];
                            if (newNick != null) {
                                if (!server.isLoginAuthenticated(login)) {
                                    nickname = newNick;
                                    sendMsg(Command.AUTH_OK + " " + nickname);
                                    server.subscribe(this);
                                    System.out.println("client " + nickname + " connected " + socket.getRemoteSocketAddress());
                                    break;
                                } else {
                                    sendMsg("С этим логином уже авторизовались");
                                }
                            } else {
                                sendMsg("Неверный логин / пароль");
                            }
                        }

                        if (str.equals(Command.END)) {
                            sendMsg(Command.END);
                            throw new RuntimeException("client disconnected");
                        }

                        if (str.startsWith(Command.REG)) {
                            String[] token = str.split("\\s");
                            if (token.length < 4) {
                                continue;
                            }
                            //boolean isRegistered = server.getAuthService().registration(token[1],token[2],token[3]);

                         /*    Отправляю в метод регистрации поделенное сообщение, он должен добавить несуществующую запись
                             в базу но получаю ошибку EOFException (client.Controller.lambda$connect$3(Controller.java:103) in.readUTF())
                             уже всю голову сломал над этим никак не получается сделать, в методе получения ника из данных пороля
                            и логина выскакивае та же самая ошибка, хотя когда команды в базу без привязки к чату делаю все работает   */
                            boolean isRegistered = server.getDataBaseAuth().registration(token[1],token[2],token[3]);
                            if (isRegistered) {
                                sendMsg(Command.REG_OK);
                            } else {
                                sendMsg(Command.REG_NO);
                            }
                        }
                    }

                    //цикл работы
                    socket.setSoTimeout(0);  //  Обнуляем таймер, если пройденна аутентификация
                    while (true) {

                        String str = in.readUTF();

                        if (str.startsWith("/")) {
                            if (str.equals(Command.END)) {
                                sendMsg(Command.END);
                                break;
                            }
                            if (str.startsWith(Command.PRV_MSG)) {
                                String[] token = str.split("\\s", 3);
                                if (token.length < 3) {
                                    continue;
                                }
                                server.privateMsg(this, token[1], token[2]);
                            }

                        } else {
                            server.broadcastMsg(this, str);
                        }
                    }

                } catch (RuntimeException e) {
                    System.out.println(e.getMessage());
                } catch (SocketTimeoutException e){      // Ловим SocketTimeoutException и отправляем сообщение на закрытие соккета
                       sendMsg(Command.END);
                } catch (IOException e) {
                    e.printStackTrace();
                } catch (SQLException e) {
                    e.printStackTrace();
                } catch (ClassNotFoundException e) {
                    e.printStackTrace();
                } finally {
                    server.unsubscribe(this);
                    System.out.println("client disconnected");
                    try {
                        socket.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }).start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void sendMsg(String msg) {
        try {
            out.writeUTF(msg);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public String getNickname() {
        return nickname;
    }

    public String getLogin() {
        return login;
    }
}
