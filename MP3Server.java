import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Files;
import java.util.Scanner;

/**
 * A MP3 Server for sending mp3 files over a socket connection.
 */
public class MP3Server {

    public static void main(String[] args) {
        Socket s;
        ServerSocket ss = null;

        System.out.println("<Starting the server>");

        try {
            ss = new ServerSocket(5001);
        } catch (IOException e) {
            e.printStackTrace();
        }

        while (true) {
            try {
                s = ss.accept();
                ClientHandler clientHandler = new ClientHandler(s);
                Thread t = new Thread(clientHandler);
                t.start();
                try {
                    t.join();
                } catch (Exception e) {
                    e.printStackTrace();
                }

            } catch (IOException e) {
                System.out.println("<An unexpected exception occurred>");

                System.out.printf("<Exception message: %s>\n", e.getMessage());

                System.out.println("<Stopping the server>");
                e.printStackTrace();
            }

        }


    }

    /**
     * Class - MP3Server.ClientHandler
     * <p>
     * This class implements Runnable, and will contain the logic for handling responses and requests to
     * and from a given client. The threads you create in MP3Server will be constructed using instances
     * of this class.
     */
    static final class ClientHandler implements Runnable {

        private ObjectInputStream inputStream;
        private ObjectOutputStream outputStream;

        public ClientHandler(Socket clientSocket) throws IllegalArgumentException {

            if (clientSocket == null) {
                throw new IllegalArgumentException("clientSocket argument is null");
            } else {
                try {
                    inputStream = new ObjectInputStream(clientSocket.getInputStream());
                    outputStream = new ObjectOutputStream(clientSocket.getOutputStream());
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        /**
         * This method is the start of execution for the thread. See the handout for more details on what
         * to do here.
         */
        public void run() {
            //TODO: Implement run method. Remember to listen for the client's input indefinitely
            try {
                while (true) {
                    SongRequest a = (SongRequest) inputStream.readObject();
                    SongRequest s = a;
                    if (s.isDownloadRequest()) {
                        if (!fileInRecord(s.getArtistName() + " - " + s.getSongName() + ".mp3")) {
                            outputStream.writeObject(new SongHeaderMessage(true, s.getSongName(), s.getArtistName(), -1));
                            outputStream.flush();
                        } else {
                            byte[] song = readSongData(s.getArtistName() + " - " + s.getSongName() + ".mp3");

                            outputStream.writeObject(new SongHeaderMessage(true, s.getSongName(), s.getArtistName(), song.length));
                            outputStream.flush();
                            sendByteArray(song);
                        }
//
                    } else {
                        outputStream.writeObject(new SongHeaderMessage(false));
                        outputStream.flush();
                        sendRecordData();
                    }
                    break;
                }

            } catch (EOFException e) {
                System.out.println("<Client Disconnected>");
            } catch (Exception e) {
                e.printStackTrace();
            }


        }

        /**
         * Searches the record file for the given filename.
         *
         * @param fileName the fileName to search for in the record file
         * @return true if the fileName is present in the record file, false if the fileName is not
         */
        private static boolean fileInRecord(String fileName) {
            File f = new File("record.txt");
            Scanner read = null;
            boolean flag = false;
            try {
                read = new Scanner(f);
            } catch (IOException e) {
                e.printStackTrace();
            }

            while (read.hasNext()) {
                String s = read.nextLine();
                if (s.equals(fileName)) {
                    flag = true;
                }
            }
            read.close();
            return flag;
        }


        /**
         * Read the bytes of a file with the given name into a byte array.
         *
         * @param fileName the name of the file to read
         * @return the byte array containing all bytes of the file, or null if an error occurred
         */
        public static byte[] readSongData(String fileName) {
            File f = new File("songDatabase" + File.separator + fileName);

            byte[] a = null;
            try {
                a = Files.readAllBytes(f.toPath());
            } catch (IOException e) {
                e.printStackTrace();
            }
            return a;
        }

        /**
         * Split the given byte array into smaller arrays of size 1000, and send the smaller arrays
         * to the client using SongDataMessages.
         *
         * @param songData the byte array to send to the client
         */


        private void sendByteArray(byte[] songData) {

            byte[] temp = null;
            int size = songData.length;
            int counter = 0;
            byte[] result = null;
            byte[] send = null;
            int size2 = size / 1000;
            int excess = size % 1000;
            for (int i = 0; i < size / 1000; i++) {
                result = new byte[1000];
                for (int j = 0; j < 1000; j++) {
                    result[j] = songData[1000 * i + j];
                }
                SongDataMessage sdm = new SongDataMessage(result);
                try {
                    outputStream.writeObject(sdm);
                    outputStream.flush();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if (size % 1000 != 0) {
                send = new byte[size % 1000];
                for (int i = 0; i < (size % 1000); i++) {
                    send[i] = songData[((size / 1000) * 1000) + i];
                }
                SongDataMessage sdm = new SongDataMessage(send);
                try {

                    outputStream.writeObject(sdm);
                    outputStream.flush();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }


        /**
         * Read ''record.txt'' line by line again, this time formatting each line in a readable
         * format, and sending it to the client. Send a ''null'' value to the client when done, to
         * signal to the client that you've finished sending the record data.
         */

        private void sendRecordData() {
            File f = new File("record.txt");
            Scanner read = null;
            try {
                read = new Scanner(f);

            } catch (IOException e) {
                e.printStackTrace();
            }

            while (read.hasNext()) {
                String[] s = read.nextLine().split(" - ");
                String result = s[1].substring(0, s[1].length() - 4) + " by: " + s[0] + "\n";
                try {
                    outputStream.writeObject(result);
                    outputStream.flush();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            try {
                outputStream.writeObject(null);
                outputStream.flush();
            } catch (Exception e) {
                e.printStackTrace();
            }

        }
    }
}
