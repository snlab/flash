package org.snlab.networkLoader;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.Scanner;

import org.snlab.flash.ModelManager.Ports.PersistentPorts;
import org.snlab.flash.ModelManager.Changes;
import org.snlab.flash.ModelManager.InverseModel;
import org.snlab.network.Device;
import org.snlab.network.Network;
import org.snlab.network.Rule;

public class I2Network {
    static public String[] devicenames = {"atla", "chic", "hous", "kans", "losa", "newy32aoa", "salt", "seat", "wash"};

    public static Network getNetwork() {
        Network n = getTopo();

        for (String name : devicenames) {
            Device device = n.getDevice(name);
            try {
                Scanner in = new Scanner(new File("dataset/I2/" + name + "apnotcomp"));
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
        return n;
    }
    
    public static Network getTopo() {
        Network n = new Network("Internet2");

        for (String name : devicenames) {
            n.addDevice(name);
        }

        n.addLink("chic", "xe-0/1/0", "newy32aoa", "xe-0/1/3");
        n.addLink("chic", "xe-1/0/1", "kans", "xe-0/1/0");
        n.addLink("chic", "xe-1/1/3", "wash", "xe-6/3/0");
        n.addLink("hous", "xe-3/1/0", "losa", "ge-6/0/0");
        n.addLink("kans", "ge-6/0/0", "salt", "ge-6/1/0");
        n.addLink("chic", "xe-1/1/2", "atla", "xe-0/1/3");
        n.addLink("seat", "xe-0/0/0", "salt", "xe-0/1/1");
        n.addLink("chic", "xe-1/0/2", "kans", "xe-0/0/3");
        n.addLink("hous", "xe-1/1/0", "kans", "xe-1/0/0");
        n.addLink("seat", "xe-0/1/0", "losa", "xe-0/0/0");
        n.addLink("salt", "xe-0/0/1", "losa", "xe-0/1/3");
        n.addLink("seat", "xe-1/0/0", "salt", "xe-0/1/3");
        n.addLink("newy32aoa", "et-3/0/0-0", "wash", "et-3/0/0-0");
        n.addLink("newy32aoa", "et-3/0/0-1", "wash", "et-3/0/0-1");
        n.addLink("chic", "xe-1/1/1", "atla", "xe-0/0/0");
        n.addLink("losa", "xe-0/1/0", "seat", "xe-2/1/0");
        n.addLink("hous", "xe-0/1/0", "losa", "ge-6/1/0");
        n.addLink("atla", "xe-0/0/3", "wash", "xe-1/1/3");
        n.addLink("hous", "xe-3/1/0", "kans", "ge-6/2/0");
        n.addLink("atla", "ge-6/0/0", "hous", "xe-0/0/0");
        n.addLink("chic", "xe-1/0/3", "kans", "xe-1/0/3");
        n.addLink("losa", "xe-0/0/3", "salt", "xe-0/1/0");
        n.addLink("atla", "ge-6/1/0", "hous", "xe-1/0/0");
        n.addLink("atla", "xe-1/0/3", "wash", "xe-0/0/0");
        n.addLink("chic", "xe-2/1/3", "wash", "xe-0/1/3");
        n.addLink("atla", "xe-1/0/1", "wash", "xe-0/0/3");
        n.addLink("kans", "xe-0/1/1", "salt", "ge-6/0/0");
        n.addLink("chic", "xe-1/1/0", "newy32aoa", "xe-0/0/0");

        n.getAllDevices().forEach(device -> device.uid = Device.cnt++);
        return n;
    }

    public static void main(String[] args) {
        System.out.println(getNetwork().getInitialRules().size());
        // System.exit(1);
        for (int i = 0; i < 10; i++) {
            Network n = getNetwork();
            InverseModel verifier = new InverseModel(n, new PersistentPorts());
            long s = System.nanoTime();
            Changes cgs = verifier.insertMiniBatch(n.getInitialRules());
            verifier.update(cgs);
            System.out.println(verifier.predSize() + " " + (System.nanoTime() - s));
        }
    }
}