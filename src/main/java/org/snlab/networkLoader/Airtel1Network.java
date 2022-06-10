package org.snlab.networkLoader;

import org.jgrapht.alg.util.Pair;
import org.snlab.network.Device;
import org.snlab.network.Network;
import org.snlab.network.Port;
import org.snlab.network.Rule;

import java.io.*;
import java.util.ArrayList;
import java.util.Scanner;

public class Airtel1Network {
    public static Network getNetwork() throws IOException {
        Scanner in = new Scanner(new File("dataset/Airtel1/airtel1.csv"));

        Network network = new Network("Airtel1");
        network.updateSequence = new ArrayList<>();
        while (in.hasNextLine()) {
            String line = in.nextLine();
            char ch = line.charAt(0);
            String[] tokens = line.split(","); // 0: prefix, 1: device, 2: egress, 3: priority

            int prefx = Integer.parseInt(tokens[0].split("/")[1]);
            long dstIp = 0;
            for (String str : tokens[0].substring(1).split("/")[0].split("\\.")) {
                dstIp = (dstIp << 8) + Long.parseLong(str);
            }

            Device device = network.getDevice(tokens[1]);
            Port outPort = device.getPort(tokens[2]);
            outPort.setPeer(network.getDevice(tokens[2]).getPort("default"));
            int priority = Integer.parseInt(tokens[3]);

            Rule rule = new Rule(device, dstIp, prefx, priority, outPort);
            if (ch == '+') network.updateSequence.add(new Pair<>(true, rule));
            if (ch == '-') network.updateSequence.add(new Pair<>(false, rule));
        }

        Device.cnt = 0;
        network.getAllDevices().forEach(device -> device.uid = Device.cnt++);
        return network;
    }
}