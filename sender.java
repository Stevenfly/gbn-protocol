/*
 * SENDER source file - CS 456 A2
 *
 * ID: 20610147
 * Name: Yufei Yang
 *
 * Acknowledgement: I am the sole contributor to this assignment
 * help from:
 *    - https://www.tutorialspoint.com/java/java_multithreading.htm
 */

import java.io.*;
import java.util.*;

public class sender {
    /**
     * Inputs:  <host address of the network emulator>
     *          <UDP port number used by the emulator to receive data from the sender>
     *          <UDP port number used by the sender to receive ACKs from the emulator>
     *          <name of the file to be transferred>
     */
    private static String  host_address;
    private static int     port_n_data;
    private static int     port_n_acks;
    private static String  file_name;


    /**
     * Assign and check arguments
     */
    private static void initialize(String[] args) {
        common.checkError(args.length != 4, "ERROR incorrect number of arguments (expected 4).");

        host_address    = args[0];
        file_name       = args[3];

        try {
            port_n_data = Integer.parseInt(args[1]);
            port_n_acks = Integer.parseInt(args[2]);
        } catch(NumberFormatException e) {
            common.checkError("ERROR found argument in wrong format.");
        }

        common.base = 0;
        common.nextseqnum = 0;
        common.ack_num = 0;
        common.send_success = false;
        common.timeout = false;
    }


    /**
     * generate a list of packets based on the text file provided
     */
    private static ArrayList<packet> generatePackets() {
        ArrayList<packet> packets = new ArrayList<>();

        try {
            FileReader fr = new FileReader(file_name);

            int seq_num = 0;
            char[] buffer = new char[common.BUFFER_SIZE];

            int i;
            while (fr.read(buffer, 0, common.BUFFER_SIZE) != -1) {
                packets.add(packet.createPacket(seq_num, String.valueOf(buffer)));
                seq_num++;
                buffer = new char[common.BUFFER_SIZE];
            }

            fr.close();
        } catch (FileNotFoundException e) {
            common.checkError("ERROR file not found.");
        } catch (IOException e) {
            common.checkError("ERROR read file.");
        } catch (Exception e) {
            common.checkError("ERROR generating packets.");
        }

        return packets;
    }


    /**
     * main method: start here
    */
    public static void main (String[] args) {
        initialize(args);
        ArrayList<packet> packets = generatePackets();

        sendingThread sending_thread = new sendingThread(packets, host_address, port_n_data);
        receivingThread receiving_thread = new receivingThread(packets, host_address, port_n_data, port_n_acks);

        sending_thread.start();
        receiving_thread.start();
    }
}


class sendingThread extends Thread {
    private Thread t;
    private static String seqnum_log_name = "seqnum.log";
    private static PrintWriter seqnum_logger;

    /**
     * Inputs:  <list of packets to send>
     *          <host address of the network emulator>
     *          <UDP port number used by the emulator to receive data from the sender>
     */
    private ArrayList<packet>   packets;
    private String              host_address;
    private int                 port_n;


    /**
     * Constructor
     */
    sendingThread(ArrayList<packet> packets, String host_address, int port_n) {
        this.packets = packets;
        this.host_address = host_address;
        this.port_n = port_n;

        try {
            seqnum_logger = new PrintWriter(seqnum_log_name);
        } catch (IOException e) {
            common.checkError("ERROR failed to creat " + seqnum_log_name);
        }
    }


    public void run() {
        try {
            while (common.nextseqnum < packets.size() || common.ack_num < packets.size()) {
                if (common.timeout) {
                    // Resent packets when timeout
                    for (int i = common.base; i < common.nextseqnum; i++) {
                        packet pckt = packets.get(i);
                        common.sendPacket(host_address, port_n, pckt);
                        seqnum_logger.println(pckt.getSeqNum());
                    }
                    common.timeout = false;
                    common.resetTimer(packets);
                }

                if(common.nextseqnum >= packets.size()) {
                    Thread.sleep(10);
                    continue;
                }

                // within window size
                if (common.nextseqnum < common.base + common.WINDOW_SIZE) {
                    packet pckt = packets.get(common.nextseqnum);

                    common.sendPacket(host_address, port_n, pckt);
                    seqnum_logger.println(pckt.getSeqNum());

                    if (common.nextseqnum == common.base) {
                        common.resetTimer(packets);
                    }

                    common.nextseqnum++;
                } else {
                    // Window is full
                    Thread.sleep(100);
                }

                Thread.sleep(20);
            }
        } catch (InterruptedException e) {
            common.checkError("ERROR sending thread interrupted.");
        }

        common.send_success = true;
        seqnum_logger.close();
    }


    public void start() {
        if (t == null) {
            t = new Thread(this);
            t.start();
        }
    }
}


class receivingThread extends Thread {
    private Thread t;
    private static String ack_log_name = "ack.log";
    private static PrintWriter ack_logger;

    /**
     * Inputs:  <host address of the network emulator>
     *          <UDP port number used by the emulator to receive data from the sender>
     *          <UDP port number used by the sender to receive ACKs from the emulator>
     */
    private ArrayList<packet>   packets;
    private String              host_address;
    private int                 port_n_data;
    private int                 port_n_acks;



    /**
     * Constructor
     */
    receivingThread(ArrayList<packet> packets, String host_address, int port_n_data, int port_n_acks) {
        this.packets = packets;
        this.host_address = host_address;
        this.port_n_data = port_n_data;
        this.port_n_acks = port_n_acks;

        try {
            ack_logger = new PrintWriter(ack_log_name);
        } catch (IOException e) {
            common.checkError("ERROR failed to creat " + ack_log_name);
        }
    }


    public void run() {
        try {
            int last_seq_num = -1;

            while (common.ack_num < packets.size()) {
                packet p = common.receivePacket(port_n_acks);

                if (p.getType() == 0) {     // type == ACK
                    int seq_num = p.getSeqNum();
                    ack_logger.println(seq_num);

                    if (seq_num == last_seq_num % 32 || (last_seq_num == -1 && seq_num != 0)) {
                        // ignore duplicated ack
                        continue;
                    }

                    common.base = common.ack_num + 1;

                    common.ack_num++;

                    if (common.base == common.nextseqnum) {
                        common.stopTimer();
                    } else {
                        common.resetTimer(packets);
                    }

                    last_seq_num = seq_num;
                }
            }

            while(!common.send_success) {
                Thread.sleep(10);
            }

            // Keep sending EOT if not receiving EOT from receiver
            int type = -1;
            while (type != 2) {
                common.sendPacket(host_address, port_n_data, packet.createEOT(packets.size() % 32));
                type = common.receivePacket(port_n_acks).getType();
            }
        } catch (InterruptedException e) {
            common.checkError("ERROR receiving thread interrupted.");
        } catch (Exception e) {
            common.checkError("ERROR creating EOT.");
        }

        ack_logger.close();
    }


    public void start () {
        if (t == null) {
            t = new Thread(this);
            t.start ();
        }
    }
}
