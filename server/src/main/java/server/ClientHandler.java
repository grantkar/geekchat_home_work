package server;

import commands.Command;

import java.io.*;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.SQLException;
import java.util.List;

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
                    socket.setSoTimeout(120000);
                    //цикл аутентификации

                    while (true) {
                        String str = in.readUTF();

                        if (str.startsWith(Command.AUTH)) {
                            String[] token = str.split("\\s");
                            String newNick = server.getAuthService()
                                    .getNicknameByLoginAndPassword(token[1], token[2]);
                            login = token[1];
                            if (newNick != null) {
                                if (!server.isLoginAuthenticated(login)) {
                                    nickname = newNick;
                                    sendMsg(Command.AUTH_OK + " " + nickname);
                                    server.subscribe(this);
                                    System.out.println("client " + nickname + " connected " + socket.getRemoteSocketAddress());
                                    socket.setSoTimeout(0);



                                        // создаем файл с названием history_+ ник пользователя в формате txt
                                        File file = new File("fileHistory/histori_"+ nickname + ".txt");
                                        file.createNewFile();

                                        // Создаем экземпляр класса FileWriter Записываем в него всю историю
                                        // которая пришла из базы данных
                                        FileWriter writer = new FileWriter(file,false);
                                        writer.write(SQLHandler.getMessageForNick(nickname));
                                        writer.close();

                                        // Считываем содержимое файла в лист history

                                        List<String> history = Files.readAllLines(Paths.get(file.getName()));
                                        if (history.size()-100<0){  // если в листе меньше 100 строк то отправляем каждую стороку клиенту
                                           for (String s : history) {
                                               sendMsg(s);
                                           }
                                        } else {     // если больше 100, то из общего количество вычитаем 100, и начиная с этой строки
                                                     // отправляем оставшиеся 100 строк клиенту
                                            int label = history.size() - 100;
                                            for (int i = label; i < history.size(); i++) {
                                                sendMsg(history.get(i));
                                            }
                                        }


                                    //==============//
                                   // sendMsg(SQLHandler.getMessageForNick(nickname));
                                    //==============//
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
                            boolean isRegistered = server.getAuthService().registration(token[1], token[2], token[3]);
                            if (isRegistered) {
                                sendMsg(Command.REG_OK);
                            } else {
                                sendMsg(Command.REG_NO);
                            }
                        }
                    }


                    //цикл работы
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

                            //==============//
                            if (str.startsWith(Command.CHNICK)) {
                                String[] token = str.split("\\s+", 2);
                                if (token.length < 2) {
                                    continue;
                                }
                                if (token[1].contains(" ")) {
                                    sendMsg("Ник не может содержать пробелов");
                                    continue;
                                }
                                if (server.getAuthService().changeNick(this.nickname, token[1])) {
                                    sendMsg(Command.YOU_NICK + token[1]);
                                    sendMsg("Ваш ник изменен на " + token[1]);
                                    this.nickname = token[1];
                                    server.broadcastClientList();
                                } else {
                                    sendMsg("Не удалось изменить ник. Ник " + token[1] + " уже существует");
                                }
                            }
                            //==============//
                        } else {
                            server.broadcastMsg(this, str);
                        }
                    }
                } catch (SocketTimeoutException e) {
                    sendMsg(Command.END);
                } catch (RuntimeException e) {
                    System.out.println(e.getMessage());
                } catch (IOException e) {
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
