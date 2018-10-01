/*
 * RECEIVER source file - CS 456 A2
 *
 * ID: 20610147
 * Name: Yufei Yang
 *
 * Acknowledgement: I am the sole contributor to this assignment
 */

import java.io.*;
import java.util.*;

public class receiver {
    /**
     * Inputs:  <hostname for the network emulator>
     *          <UDP port number used by the link emulator to receive ACKs from the receiver>
     *          <UDP port number used by the receiver to receive data from the emulator>
     *          <name of the file into which the received data is written>
     */
    private static String  host_address;
    private static int     port_n_acks;
    private static int     port_n_data;
    private static String  file_name;

    /**
     * Constants
     */
    private static String arrival_log_name = "arrival.log";

    /**
     * Variables
     */
    private static PrintWriter output_writer;
    private static PrintWriter arrival_logger;


    /**
     * Assign and check arguments
     */
    private static void initialize(String[] args) {
        common.checkError(args.length != 4, "ERROR incorrect number of arguments (expected 4).");

        host_address    = args[0];
        file_name       = args[3];

        try {
            port_n_acks = Integer.parseInt(args[1]);
            port_n_data = Integer.parseInt(args[2]);
        } catch(NumberFormatException e) {
            common.checkError("ERROR found argument in wrong format.");
        }

        try {
            output_writer = new PrintWriter(file_name);
            arrival_logger = new PrintWriter(arrival_log_name);
        } catch (IOException e) {
            common.checkError("ERROR failed to creat files.");
        }
    }


    /**
     * Deal with received packet
     */
    private static void processPackets() {
        try {
            int last_seq_num = -1;

            while (true) {
                packet pckt = common.receivePacket(port_n_data);

                if (pckt.getType() == 2) {
                    break;
                }

                int seq_num = pckt.getSeqNum();

                arrival_logger.println(seq_num);

                if (last_seq_num == -1 || seq_num == (last_seq_num + 1) % 32) {
                    output_writer.print(new String(pckt.getData()));
                    last_seq_num = seq_num;
                }

                common.sendPacket(host_address, port_n_acks, packet.createACK(last_seq_num));
            }

            // send EOT
            common.sendPacket(host_address, port_n_acks, packet.createEOT((last_seq_num+1) % 32));

        } catch (IOException e) {
            common.checkError("ERROR failed to receive packet.");
        } catch (Exception e) {
            common.checkError("ERROR failed to create packet.");
        }
    }


    /**
     * main method: start here
     */
    public static void main (String[] args) {
        initialize(args);
        processPackets();

        output_writer.close();
        arrival_logger.close();
    }
}
