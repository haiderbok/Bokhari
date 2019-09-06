/**
 * @author Muhammad Bokhari
 * @author Logan Mohrs
 * @version 04/12/2019 L13
 */

import java.io.*;
import java.net.Socket;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;

/**
 * An MP3 Client to request .mp3 files from a server and receive them over the socket connection.
 */
public class MP3Client {

    public static void main(String[] args) {
        //TODO: Implement main
        Socket serverConnection = new Socket();
        String input = "";
        ObjectOutputStream oos = null;
        ObjectInputStream objectInputStream = null;
        Scanner s = new Scanner(System.in);


        while (!input.equals("exit")) {
            try {
                serverConnection = new Socket("localhost", 5001);
                oos = new ObjectOutputStream(serverConnection.getOutputStream());
            } catch (IOException e) {
                e.printStackTrace();
                System.out.println("<An unexpected exception occurred>");

                System.out.printf("<Exception message: %s>\n", e.getMessage());

            }
            System.out.println("Do you want to see a list of available songs or request to download a song? (List/Download)");
            input = s.nextLine();

            while (!input.equals("Download") && !input.equals("List") && !input.equals("exit")) {
                System.out.println("Wrong input : Re-Enter your request");
                input = s.nextLine();
            }
            if (input.equals("exit")) {
                try {
                    oos.close();
                    serverConnection.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
                break;
            }

            if (input.equals("List")) {
                SongRequest songRequest = new SongRequest(false);
                try {
                    oos.writeObject(songRequest);
                    oos.flush();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            if (input.equals("Download")) {
                System.out.println("Input song name and artist name in this format: (Song Name/Artist Name)");
                String[] song = s.nextLine().split("/");
                try {
                    SongRequest songRequest = new SongRequest(true, song[0], song[1]);
                    oos.writeObject(songRequest);
                    oos.flush();
                } catch (Exception e) {
                    System.out.println("Invalid Input");
                    input = "";
                    continue;
                }
            }
            ResponseListener responseListener = new ResponseListener(serverConnection);
            Thread t = new Thread(responseListener);
            t.start();
            try {
                t.join();
            } catch (Exception e) {
                e.printStackTrace();
            }
            input = "";
        }
    }
}


/**
 * This class implements Runnable, and will contain the logic for listening for
 * server responses. The threads you create in MP3Server will be constructed using
 * instances of this class.
 */
final class ResponseListener implements Runnable {

    private ObjectInputStream ois;

    public ResponseListener(Socket clientSocket) {
        //TODO: Implement constructor
        try {
            ois = new ObjectInputStream(clientSocket.getInputStream());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Listens for a response from the server.
     * <p>
     * Continuously tries to read a SongHeaderMessage. Gets the artist name, song name, and file size from that header,
     * and if the file size is not -1, that means the file exists. If the file does exist, the method then subsequently
     * waits for a series of SongDataMessages, takes the byte data from those data messages and writes it into a
     * properly named file.
     */
    public void run() {
        //TODO: Implement run

        try {
            Object a = ois.readObject();
            byte[] lame;

            if (a != null && a instanceof SongHeaderMessage) {
                SongHeaderMessage songHeaderMessage = (SongHeaderMessage) a;
                if (songHeaderMessage.isSongHeader()) {
                    File file = new File("savedSongs" + File.separator + songHeaderMessage.getArtistName() + " - " + songHeaderMessage.getSongName() + ".mp3");

                    int size = songHeaderMessage.getFileSize();
                    if (size < 0) {
                        return;
                    }
                    byte[] array = new byte[size];
                    //byte[] lame;
                    SongDataMessage sdm;
                    int count = 0;

                    for (int i = 0; i < size / 1000; i++) {
                        sdm = (SongDataMessage) ois.readObject();
                        lame = sdm.getData();
                        for (int j = 0; j < 1000; j++) {
                            array[(i * 1000) + j] = lame[j];
                        }
                    }
                    SongDataMessage shm = (SongDataMessage) ois.readObject();
                    byte[] two = shm.getData();

                    for (int i = 0; i < size % 1000; i++) {
                        array[((size / 1000) * 1000) + i] = two[i];
                    }
                    writeByteArrayToFile(array, file.getAbsolutePath());


                } else {
                    //loop waiting for strings,
                    //read object
                    //if not null and instance of string,
                    //keep reading in objects and casting them to strings until you get a null object
                    //print out strings

                    while (true) {
                        Object b = ois.readObject();

                        if (b != null && b instanceof String) {
                            String list = (String) b;
                            System.out.println(list);
                        } else {
                            break;
                        }
                    }
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Writes the given array of bytes to a file whose name is given by the fileName argument.
     *
     * @param songBytes the byte array to be written
     * @param fileName  the name of the file to which the bytes will be written
     */
    private void writeByteArrayToFile(byte[] songBytes, String fileName) {
        FileOutputStream fileOutputStream;
        try {
            fileOutputStream = new FileOutputStream(fileName);
            fileOutputStream.write(songBytes);
            fileOutputStream.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }
}
