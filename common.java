/*
 * common class with helper methods
 *
 * ID: 20610147
 * Name: Yufei Yang
 *
 * Acknowledgement: I am the sole contributor to this assignment
 * help from:
 *    - https://docs.oracle.com/javase/tutorial/networking/datagrams/clientServer.html
 *    - http://www.iitk.ac.in/esc101/05Aug/tutorial/essential/threads/timer.html
 */

import java.io.*;
import java.net.*;
import java.util.*;

public class common {
    /**
     * Constants
     */
    public static final int WINDOW_SIZE = 10;
    public static final int BUFFER_SIZE = 500;


    /**
     * Variables
     */
    public static int base;
    public static int nextseqnum;
    public static int ack_num;              // the num of valid acks
    public static boolean send_success;     // all packets are sent successfully
    public static boolean timeout;
    public static Timer timer;


    /**
     * Function to put out a message on error exits (when condition is true). Take a boolean and a String as input.
     *    If only a String is given, then default condition is true.
     */
    public static void checkError(String msg) {
        checkError(true, msg);
    }
    public static void checkError(boolean condition, String msg) {
        if (condition) {
            System.err.println(msg);
            System.exit(1);
        }
    }


    /**
     * Function to send packet through UDP connection.
     */
    public static void sendPacket(String host_address, int port_n, packet p) {
        try {
            DatagramSocket socket = new DatagramSocket();
            InetAddress address = InetAddress.getByName(host_address);

            byte[] data = p.getUDPdata();

            DatagramPacket pckt = new DatagramPacket(data, data.length, address, port_n);
            socket.send(pckt);
            socket.close();
        } catch (UnknownHostException e) {
            checkError("ERROR unkown host.");
        } catch (IOException e) {
            checkError("ERROR sending packet.");
        }
    }


    /**
     * Function to receive packet through UDP connection.
     */
    public static packet receivePacket(int port_n) {
        try {
            DatagramSocket socket = new DatagramSocket(port_n);
            byte[] data = new byte[512];

            DatagramPacket pckt = new DatagramPacket(data, data.length);
            socket.receive(pckt);
            socket.close();

            return packet.parseUDPdata(data);
        } catch (IOException e) {
            checkError("ERROR receiving packet.");
        } catch (Exception e) {
            return null;
        }

        return null;
    }


    /**
     * Function to reset timer.
     */
    public static synchronized void resetTimer(ArrayList<packet> packets) {
        stopTimer();

        timer = new Timer();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                if (ack_num == packets.size()) {
                    return;
                }
                timeout = true;
            }
        }, 200);
    }


    /**
     * Function to stop timer.
     */
    public static synchronized void stopTimer() {
        if (timer != null) {
            timer.cancel();
            timer.purge();
        }
    }
}
