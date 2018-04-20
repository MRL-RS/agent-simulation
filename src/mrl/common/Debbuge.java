package mrl.common;

import java.io.*;

/**
 * @author vahid hooshangi
 */
public class Debbuge {
    public static String readFile(String filename) throws IOException {
        File file = new File(filename);
        int len = (int) file.length();
        byte[] bytes = new byte[len];
        FileInputStream fis = null;
        try {
            fis = new FileInputStream(file);
            assert len == fis.read(bytes);
        } catch (IOException e) {
            close(fis);
            throw e;
        }
        return new String(bytes, "UTF-8");
    }

    public static void writeFile(String filename, String text) throws IOException {
        FileOutputStream fos = null;
        try {
            fos = new FileOutputStream(filename);
            fos.write(text.getBytes("UTF-8"));
        } catch (IOException e) {
            close(fos);
            throw e;
        }
    }

    public static void close(Closeable closeable) {
        try {
            closeable.close();
        } catch (IOException ignored) {
        }
    }


    public static void fileAppending(String txt) {

        try {

            OutputStreamWriter writer = new OutputStreamWriter(
                    new FileOutputStream("messageDebug.txt", true), "UTF-8");
            BufferedWriter fbw = new BufferedWriter(writer);
            fbw.write(txt);
            fbw.newLine();
            fbw.close();
        } catch (Exception e) {
            System.out.println("Error: " + e.getMessage());
        }
    }

    public static void waterCoolingFileAppending(String txt) { //by Sajjad

        try {

            OutputStreamWriter writer = new OutputStreamWriter(
                    new FileOutputStream("waterCooling.txt", true), "UTF-8");
            BufferedWriter fbw = new BufferedWriter(writer);
            fbw.write(txt);
            fbw.newLine();
            fbw.close();
        } catch (Exception e) {
            System.out.println("Error: " + e.getMessage());
        }
    }
}
