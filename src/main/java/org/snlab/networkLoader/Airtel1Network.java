package org.snlab.networkLoader;

import org.snlab.network.Device;
import org.snlab.network.Network;
import org.snlab.network.Rule;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.HashSet;
import java.util.Scanner;
import java.util.Set;

public class Airtel1Network {
    public static Network getNetwork() {
        Network n = new Network("Airtel1");
        Set<String> devicenames = new HashSet<>();

        try {
            Scanner in = new Scanner(new File("airtel1/topo.txt"));
            while (in.hasNext()) {
                String[] tokens = in.nextLine().split(" ");

                if (!devicenames.contains(tokens[0]))  {devicenames.add(tokens[0]);n.addDevice(tokens[0]);}
                if (!devicenames.contains(tokens[2]))  {devicenames.add(tokens[1]);n.addDevice(tokens[2]);}
                n.addLink(tokens[0], tokens[1], tokens[2], tokens[3]);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        // Device defaultDevice = n.addDevice("default");
        // n.addLink("default", "default", "default", "default-default");
        for (String name : devicenames) {
            Device device = n.addDevice(name);
            // defaultDevice.addPort(name + "-default");
            // n.addLink(name, name + "-default", "default", name + "-peer-default");
            try {
                Scanner in = new Scanner(new File("dataset/airtel1/fib/" + name));
                while (in.hasNextLine()) {
                    String line = in.nextLine();
                    String[] tokens = line.split(" ");
                    String pn = tokens[3].split("\\.")[0];
                    if (device.getPort(pn) == null) {
                        device.addPort(pn);
                    }
                    long ip = Long.parseLong(tokens[1]);
                    Rule rule = new Rule(device, ip, Integer.parseInt(tokens[2]), device.getPort(pn));
                    device.addInitialRule(rule);
                    n.addInitialRule(rule);
                }
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
        }

        n.getAllDevices().forEach(device -> device.uid = Device.cnt++);
        return n;
    }

    /*
    public static void main(String[] args) {
        System.out.println(getNetwork().getInitialRules().size());
        System.out.println(getNetwork().getAllDevices());
        System.exit(1);
        for (int i = 0; i < 10; i++) {
            Network n = getNetwork();
            JiffyVerifier verifier = new JiffyVerifier(n, new PersistentPorts());
            for (Rule rule : n.getInitialRules()) {
                Changes cgs = verifier.insertMiniBatch(new ArrayList<>(Arrays.asList(rule)));
                verifier.update(cgs);
            }
            System.out.println(verifier.predSize());
        }
    }
     */
}