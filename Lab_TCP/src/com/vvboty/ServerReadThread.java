package com.vvboty;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.net.Socket;
import java.security.DigestOutputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.Time;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Timer;

public class ServerReadThread implements Runnable {
    private Socket clientSocket;
    private InputStream clientInputStream;

    ServerReadThread(Socket socket){
        this.clientSocket = socket;
        try {
            clientInputStream = socket.getInputStream();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void run(){
        System.out.println(Thread.currentThread().getName() + ": started downloading file");
        Timer timer = new Timer();

        //My packet: fileNameSize[2] | fileName[fileNameSize] | fileSize[8] | file[fileSize] |
        try {

            byte[] fileNameSize = new byte[2];
            clientInputStream.read(fileNameSize, 0, 2);

            byte[] fileName = new byte[new BigInteger(fileNameSize).intValue()];
            clientInputStream.read(fileName, 0, fileName.length);

            byte[] fileSize = new byte[8];
            clientInputStream.read(fileSize, 0, 8);

            long bytesRemain = new BigInteger(fileSize).longValue();
            byte[] fileBuffer = new byte [4096];

            File file = new File("uploads/" + new String(fileName).trim());
            file.createNewFile();

            DigestOutputStream outputStream = new DigestOutputStream(new FileOutputStream(file), MessageDigest.getInstance("MD5"));

            int bytesRead;
            final ArrayList<Integer> bytesReadForTimer = new ArrayList<>();
            bytesReadForTimer.add(0);

            timer.schedule(new SpeedTesterTask(bytesReadForTimer), 0, 3000);

            do{
                bytesRead = clientInputStream.read(fileBuffer, 0, bytesRemain < 4096L ? Math.toIntExact(bytesRemain) : 4096);
                bytesRemain -= bytesRead;
                bytesReadForTimer.set(0, bytesReadForTimer.get(0) + bytesRead);
                outputStream.write(fileBuffer, 0, bytesRead);
            }while (bytesRemain > 0);

            byte[] md5digest = new byte[16];
            clientInputStream.read(md5digest, 0, 16);

            if (Arrays.equals(md5digest, outputStream.getMessageDigest().digest())) {
                System.out.println("File downloaded successfuly.");
            } else {
                System.out.println("Error: file invalid!");
            }
            timer.cancel();
            outputStream.close();
            clientSocket.getOutputStream().write(1);
            clientSocket.getOutputStream().flush();
            clientSocket.close();

        } catch (IOException | NoSuchAlgorithmException e) {
            System.out.println(Thread.currentThread().getName() + " got an error! Connection will be closed.");

            timer.cancel();
            try {
                clientSocket.close();
            } catch (IOException ioException) { }
        }
    }
}
