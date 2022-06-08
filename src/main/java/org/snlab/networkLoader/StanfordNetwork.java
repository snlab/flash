package org.snlab.networkLoader;

import org.snlab.network.ACLUse;
import org.snlab.network.Device;
import org.snlab.network.Network;
import org.snlab.network.Rule;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.*;

public class StanfordNetwork {
    public static Network getNetwork() {
        Network n = new Network("Stanford");
        String [] devicenames = {"bbra_rtr", "bbrb_rtr", "boza_rtr", "bozb_rtr", "coza_rtr", "cozb_rtr", "goza_rtr",
                "gozb_rtr", "poza_rtr", "pozb_rtr", "roza_rtr", "rozb_rtr", "soza_rtr", "sozb_rtr", "yoza_rtr", "yozb_rtr"};

//        Device defaultDevice = n.addDevice("default");
//        n.addLink("default", "default", "default", "default-default");
        for (String name : devicenames) {
            Device device = n.addDevice(name);
//            defaultDevice.addPort(name + "-default");
//            n.addLink(name, name + "-default", "default", name + "-peer-default");
            try {
                // Scanner in = new Scanner(new File("stconfig/" + name + "_config.txt"));
                Scanner in = new Scanner(new File("dataset/Stanford/st/" + name + "ap"));
                while (in.hasNextLine()) {
                    String line = in.nextLine();
                    String[] tokens = line.split(" ");
                    if (tokens[0].equals("fw")) {
                        String pn = tokens[3].split("\\.")[0];
                        if (device.getPort(pn) == null) {
                            device.addPort(pn);
                        }
                        long ip = Long.parseLong(tokens[1]);
                        Rule rule = new Rule(device, ip, Integer.parseInt(tokens[2]), device.getPort(pn));
                        device.addInitialRule(rule);
                        n.addInitialRule(rule);
                    } else if (tokens[0].equals("acl")) {
                        for (int i = 0; i < (tokens.length - 2)/2; i ++)
                        {
                            n.addACL(new ACLUse(tokens[1], tokens[(i+1)*2], tokens[(i+1)*2+1]));
                            //System.out.println(tokens[1] + " " + tokens[(i+1)*2] + " " + tokens[(i+1)*2+1]);
                        }
                    }
                }
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
        }

        n.addLink("bbra_rtr","te7/3","goza_rtr","te2/1");
        n.addLink("bbra_rtr","te7/3","pozb_rtr","te3/1");
        n.addLink("bbra_rtr","te1/3","bozb_rtr","te3/1");
        n.addLink("bbra_rtr","te1/3","yozb_rtr","te2/1");
        n.addLink("bbra_rtr","te1/3","roza_rtr","te2/1");
        n.addLink("bbra_rtr","te1/4","boza_rtr","te2/1");
        n.addLink("bbra_rtr","te1/4","rozb_rtr","te3/1");
        n.addLink("bbra_rtr","te6/1","gozb_rtr","te3/1");
        n.addLink("bbra_rtr","te6/1","cozb_rtr","te3/1");
        n.addLink("bbra_rtr","te6/1","poza_rtr","te2/1");
        n.addLink("bbra_rtr","te6/1","soza_rtr","te2/1");
        n.addLink("bbra_rtr","te7/2","coza_rtr","te2/1");
        n.addLink("bbra_rtr","te7/2","sozb_rtr","te3/1");
        n.addLink("bbra_rtr","te6/3","yoza_rtr","te1/3");
        n.addLink("bbra_rtr","te7/1","bbrb_rtr","te7/1");
        n.addLink("bbrb_rtr","te7/4","yoza_rtr","te7/1");
        n.addLink("bbrb_rtr","te1/1","goza_rtr","te3/1");
        n.addLink("bbrb_rtr","te1/1","pozb_rtr","te2/1");
        n.addLink("bbrb_rtr","te6/3","bozb_rtr","te2/1");
        n.addLink("bbrb_rtr","te6/3","roza_rtr","te3/1");
        n.addLink("bbrb_rtr","te6/3","yozb_rtr","te1/1");
        n.addLink("bbrb_rtr","te1/3","boza_rtr","te3/1");
        n.addLink("bbrb_rtr","te1/3","rozb_rtr","te2/1");
        n.addLink("bbrb_rtr","te7/2","gozb_rtr","te2/1");
        n.addLink("bbrb_rtr","te7/2","cozb_rtr","te2/1");
        n.addLink("bbrb_rtr","te7/2","poza_rtr","te3/1");
        n.addLink("bbrb_rtr","te7/2","soza_rtr","te3/1");
        n.addLink("bbrb_rtr","te6/1","coza_rtr","te3/1");
        n.addLink("bbrb_rtr","te6/1","sozb_rtr","te2/1");
        n.addLink("boza_rtr","te2/3","bozb_rtr","te2/3");
        n.addLink("coza_rtr","te2/3","cozb_rtr","te2/3");
        n.addLink("goza_rtr","te2/3","gozb_rtr","te2/3");
        n.addLink("poza_rtr","te2/3","pozb_rtr","te2/3");
        n.addLink("roza_rtr","te2/3","rozb_rtr","te2/3");
        n.addLink("soza_rtr","te2/3","sozb_rtr","te2/3");
        n.addLink("yoza_rtr","te1/1","yozb_rtr","te1/3");
        n.addLink("yoza_rtr","te1/2","yozb_rtr","te1/2");

        n.getAllDevices().forEach(device -> device.uid = Device.cnt++);
        return n;
    }

}