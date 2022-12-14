import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;

public class Server {
    private Socket[] socketList;
    private ServerSocket serverSock;
    private Socket connectionSock;
    private int port = 22222;

    public Server() {
        socketList = new Socket[2];
    }

    private void getConnection() {
        // Wait for a connection from the client
        try {
            System.out.println("Waiting for player connections on port 7654.");
            serverSock = new ServerSocket(port);
            // This is an infinite loop, the user will have to shut it down
            // using control-c
            InterfazTablero game = new InterfazTablero();

            int playerID = 1;

            for (int i = 0; i < 2; ++i) {
                connectionSock = serverSock.accept();
                // Add this socket to the list
                this.socketList[i] = connectionSock;
                // Send to ClientHandler the socket and arraylist of all sockets

                System.out.println("Player " + Integer.toString(i + 1) + " connected successfully.");

                Controlador handler = new Controlador(connectionSock, this.socketList, game, playerID);
                Thread theThread = new Thread(handler);
                theThread.start();
                playerID -= 2;
            }

            System.out.println("Game running...");

            Socket connectionSock = serverSock.accept();

            for (int i = 0; i < this.socketList.length; ++i) {
                socketList[i].close();
            }

            // Will never get here, but if the above loop is given
            // an exit condition then we'll go ahead and close the socket
            //serverSock.close();
        } catch (IOException e) {
            System.out.println(e.getMessage());
        }
    }


    public static void main(String[] args) {
        Server server = new Server();
        server.getConnection();
    }
}

class InterfazTablero {

    public InterfazTablero() {
        this.currentBoard = new int[3][3];
        this.playerMove = 1;
    }

    public volatile int playerMove; //X = 1, O = -1
    private int[][] currentBoard; //3x3

    public void resetGame() {
        this.currentBoard = new int[3][3];
        this.playerMove = 1;
    }

    public boolean submitMove(int i, int j) {
        if (this.currentBoard[i][j] != 0) {
            return false;
        } else {
            this.currentBoard[i][j] = this.playerMove;
            this.playerMove = -this.playerMove;
            return true;
        }
    }

    public String printState() {
        String output = "#";
        for (int i = 0; i < 3; ++i) {
            output += Integer.toString(this.currentBoard[i][0]) + "," + Integer.toString(this.currentBoard[i][1]) + "," + Integer.toString(this.currentBoard[i][2]) + ";";
        }
        //output += Integer.toString(this.playerMove);
        return output;
    }

    public int checkWin() {
        boolean cats = true;
        for (int i = 0; i < 3; ++i) {
            if ((this.currentBoard[i][0] == this.currentBoard[i][1] && this.currentBoard[i][0] == this.currentBoard[i][2]) && this.currentBoard[i][0] != 0) {
                return this.currentBoard[i][0];
            }
        }
        for (int i = 0; i < 3; ++i) {
            if ((this.currentBoard[0][i] == this.currentBoard[1][i] && this.currentBoard[0][i] == this.currentBoard[2][i]) && this.currentBoard[0][i] != 0) {
                return this.currentBoard[0][i];
            }
        }
        if ((this.currentBoard[0][0] == this.currentBoard[1][1] && this.currentBoard[0][0] == this.currentBoard[2][2]) && this.currentBoard[0][0] != 0) {
            return this.currentBoard[0][0];
        }
        if ((this.currentBoard[2][0] == this.currentBoard[1][1] && this.currentBoard[2][0] == this.currentBoard[0][2]) && this.currentBoard[2][0] != 0) {
            return this.currentBoard[2][0];
        }
        for (int i = 0; i < 3; ++i) {
            for (int j = 0; j < 3; ++j) {
                if (this.currentBoard[i][j] == 0) {
                    cats = false;
                }
            }
        }
        if (cats) {
            return 2;
        } else {
            return 0;
        }
    }

}

class Controlador implements Runnable {

    public Socket connectionSock;
    public Socket[] socketList;
    public InterfazTablero partida;
    public int playerID;

    public Controlador(Socket sock, Socket[] socketList, InterfazTablero partida, int playerID) {
        this.connectionSock = sock;
        this.socketList = socketList;    // Keep reference to master list
        this.partida = partida;
        this.playerID = playerID;
    }

    public void run() {
        try {
            //TTTInterface game = new TTTInterface();

            BufferedReader playerInput = new BufferedReader(new InputStreamReader(this.connectionSock.getInputStream()));

            switch (this.playerID) {
                case -1:
                    sendMessage("\nYou are player 'O', you will go second." + "\r\n");
                    sendMessage("-" + "\r\n");
                    break;
                case 1:
                    sendMessage("\nYou are player 'X', you will go first." + "\r\n");
                    sendMessage("+" + "\r\n");
                    break;
                default:
                    break;
            }

            while (this.partida.checkWin() == 0) {
                sendMessage(this.partida.printState() + "\r\n");
                String playerSym = "";
                int playerIndex = 1;
                int indexInverse = 0;
                if (this.partida.playerMove == this.playerID) {
                    // my turn
                    sendMessage("Pleaae enter a row (0-2): " + "\r\n");
                    String row = playerInput.readLine().trim();
                    sendMessage("Pleaae enter a column (0-2): " + "\r\n");
                    String col = playerInput.readLine().trim();
                    if (!(this.partida.submitMove(Integer.parseInt(row), Integer.parseInt(col)))) {
                        sendMessage("Invalid move." + "\r\n");
                    } else {
                        sendMessage("-" + "\r\n");
                    }
                } else {
                    // other player's turn
                    sendMessage("Please wait for opponent's move." + "\r\n");
                    while (this.partida.playerMove != this.playerID) {
                        Thread.sleep(500);
                    }
                    sendMessage("+" + "\r\n");
                }
            }

            sendMessage(this.partida.printState());

            int checkResult = this.partida.checkWin();
            sendMessage(Integer.toString(checkResult) + "\r\n");
            if (checkResult == this.playerID) {
                sendMessage("GAME OVER! YOU WIN!" + "\r\n");
            } else if (checkResult == 2) {
                sendMessage("GAME OVER! TIE GAME!" + "\r\n");
            } else {
                sendMessage("GAME OVER! YOU LOSE!" + "\r\n");
            }
        } catch (IOException e) {
            System.out.println(e.getMessage());
        } catch (InterruptedException z) {
            System.out.println(z.getMessage());
        }
    }

    private void sendMessage(String message) { //0 = O, 1 = X, 2 = both
        try {
            DataOutputStream clientOutput = new DataOutputStream(this.connectionSock.getOutputStream());
            clientOutput.writeBytes(message);
            //System.out.println(message);
        } catch (IOException e) {
            System.out.println(e.getMessage());
        }
    }


}


