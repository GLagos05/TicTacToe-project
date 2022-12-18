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
        // Espera la conexión de un cliente
        try {
            System.out.println("Esperando conexiones de jugadores en el puerto 22222");
            serverSock = new ServerSocket(port);
            // Esto es un loop infinito, el usuario debe terminar el proceso
            // Usando Ctrl+C
            InterfazTablero game = new InterfazTablero();

            int playerID = 1;

            for (int i = 0; i < 2; ++i) {
                connectionSock = serverSock.accept();
                // Añade un socket a la lista
                this.socketList[i] = connectionSock;
                // Envía al controlador el socket del servidor y la lista de sockets

                System.out.println("Player " + Integer.toString(i + 1) + " connected successfully.");

                Controlador controlador = new Controlador(connectionSock, this.socketList, game, playerID);
                Thread hilo2 = new Thread(controlador);
                hilo2.start();
                playerID -= 2;
            }

            System.out.println("Partida iniciada!!");

            Socket connectionSock = serverSock.accept();

            for (int i = 0; i < this.socketList.length; ++i) {
                socketList[i].close();
            }

            // se cierran los sockets
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

    //Reinicia el juego
    public void resetGame() {
        this.currentBoard = new int[3][3];
        this.playerMove = 1;
    }

    //envía movimientos
    public boolean submitMove(int i, int j) {
        if (this.currentBoard[i][j] != 0) {
            return false;
        } else {
            this.currentBoard[i][j] = this.playerMove;
            this.playerMove = -this.playerMove;
            return true;
        }
    }

    //imprime estado
    public String printState() {
        String output = "#";
        for (int i = 0; i < 3; ++i) {
            output += Integer.toString(this.currentBoard[i][0]) + "," + Integer.toString(this.currentBoard[i][1]) + "," + Integer.toString(this.currentBoard[i][2]) + ";";
        }
        return output;
    }

    //verifica si el jugador ganó
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

            BufferedReader playerInput = new BufferedReader(new InputStreamReader(this.connectionSock.getInputStream()));

            switch (this.playerID) {
                case -1:
                    enviarMensaje("\nYou are player 'O', you will go second." + "\r\n");
                    enviarMensaje("-" + "\r\n");
                    break;
                case 1:
                    enviarMensaje("\nYou are player 'X', you will go first." + "\r\n");
                    enviarMensaje("+" + "\r\n");
                    break;
                default:
                    break;
            }

            while (this.partida.checkWin() == 0) {
                enviarMensaje(this.partida.printState() + "\r\n");
                String playerSym = "";
                int playerIndex = 1;
                int indexInverse = 0;
                if (this.partida.playerMove == this.playerID) {
                    // Mi turno
                    enviarMensaje("Ingresa una fila (0-2): " + "\r\n");
                    String row = playerInput.readLine().trim();
                    enviarMensaje("Ingresa una columna (0-2): " + "\r\n");
                    String col = playerInput.readLine().trim();
                    if (!(this.partida.submitMove(Integer.parseInt(row), Integer.parseInt(col)))) {
                        enviarMensaje("Movimiento inválido" + "\r\n");
                    } else {
                        enviarMensaje("-" + "\r\n");
                    }
                } else {
                    // turno del oponente
                    enviarMensaje("Por favor, espera el movimiento del gato " + "\r\n");
                    while (this.partida.playerMove != this.playerID) {
                        Thread.sleep(500);
                    }
                    enviarMensaje("+" + "\r\n");
                }
            }

            enviarMensaje(this.partida.printState());

            int verificarResultado = this.partida.checkWin();
            enviarMensaje(Integer.toString(verificarResultado) + "\r\n");
            if (verificarResultado == this.playerID) {
                enviarMensaje("GAME OVER! HAS GANADO!" + "\r\n");
            } else if (verificarResultado == 2) {
                enviarMensaje("GAME OVER! HAS EMPATADO!" + "\r\n");
            } else {
                enviarMensaje("GAME OVER! HAS PERDIDO!" + "\r\n");
            }
        } catch (IOException e) {
            System.out.println(e.getMessage());
        } catch (InterruptedException z) {
            System.out.println(z.getMessage());
        }
    }

    private void enviarMensaje(String mensaje) { //0 = O, 1 = X, 2 = both
        try {
            DataOutputStream clientOutput = new DataOutputStream(this.connectionSock.getOutputStream());
            clientOutput.writeBytes(mensaje);
        } catch (IOException e) {
            System.out.println(e.getMessage());
        }
    }


}


