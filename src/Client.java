import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;
import java.util.Scanner;

public class Client {
    private static String hostname = "maquina2";
    private static int port = 22222;
    private static boolean myTurn = true;
    private static Scanner keyboard = new Scanner(System.in);
    private static Socket connectionSock;
    private static DataOutputStream serverOutput;

    public static void main(String[] args) {
        try {
            System.out.println("Conectándo al servidor en en puerto: " + port);
            connectionSock = new Socket(hostname, port);

            serverOutput = new DataOutputStream(connectionSock.getOutputStream());

            System.out.println("Connection made.");

            // Start a thread to listen and display data sent by the server
            Acciones jugadas = new Acciones(connectionSock);
            Thread theThread = new Thread(jugadas);
            theThread.start();

            // Read input from the keyboard and send it to everyone else.
            // The only way to quit is to hit control-c, but a quit command
            // could easily be added.
            while (serverOutput != null) {
                String data = keyboard.nextLine();
                if (!myTurn) {
                    System.out.println("Please wait for your turn.");
                } else if ((data.equals("0") || data.equals("1")) || data.equals("2")) {
                    serverOutput.writeBytes(data + "\n");
                } else if (data.equals("quit")) {
                    serverOutput.close();
                    serverOutput = null;
                } else {
                    System.out.println("Invalid input, please try again.");
                }
            }
            System.out.println("Connection lost.");
        } catch (IOException e) {
            System.out.println(e.getMessage());
        }
    }
}

class Acciones implements Runnable {
    private Socket connectionSock = null;
    private boolean myTurn;

    Acciones(Socket sock) {
        this.connectionSock = sock;
        //this.myTurn = myTurn;
    }

    public void run() {
        // Wait for data from the server.  If received, output it.
        try {
            BufferedReader serverInput = new BufferedReader(new InputStreamReader(connectionSock.getInputStream()));
            while (true) {
                if (serverInput == null) {
                    // Connection was lost
                    System.out.println("Closing connection for socket " + connectionSock);
                    connectionSock.close();
                    break;
                }
                // Get data sent from the server

                //System.out.println("A");
                String serverText = serverInput.readLine();
                //System.out.println("B");
                //System.out.println(serverText);

                if (serverText.startsWith("#")) {
                    printBoardFormatted(serverText.substring(1));
                } else if (serverText.startsWith("~")) {
                    //wait
                } else if (serverText.startsWith("+")) {
                    this.myTurn = true;
                } else if (serverText.startsWith("-")) {
                    this.myTurn = false;
                } else {
                    System.out.println(serverText);
                }
            }
        } catch (Exception e) {
            System.out.println("Error: " + e.toString());
        }
    }

    private void printBoardFormatted(String boardData) {
        String[] lines = boardData.split(";");
        //System.out.println(Integer.toString(lines.length));
        String[][] board = new String[3][3];
        for (int i = 0; i < 3; ++i) {
            board[i] = lines[i].split(",");
        }
        for (int k = 0; k < 3; ++k) {
            for (int j = 0; j < 3; ++j) {
                if (board[k][j].equals("1")) {
                    board[k][j] = "X";
                } else if (board[k][j].equals("-1")) {
                    board[k][j] = "O";
                } else {
                    board[k][j] = " ";
                }
            }
        }
        System.out.println("   0   1   2");
        System.out.format("0 %2s |%2s |%2s \n", board[0][0], board[0][1], board[0][2]);
        System.out.println("  ---|---|---");
        System.out.format("1 %2s |%2s |%2s \n", board[1][0], board[1][1], board[1][2]);
        System.out.println("  ---|---|---");
        System.out.format("2 %2s |%2s |%2s \n", board[2][0], board[2][1], board[2][2]);

    }
} // ClientListener for MTClient

