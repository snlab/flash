package org.snlab.flash.CE2D;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Scanner;

import com.google.common.collect.Collections2;

import org.snlab.flash.model.ModelManager;
import org.snlab.flash.model.PersistentPorts;
import org.snlab.network.Device;
import org.snlab.network.Network;
import org.snlab.network.Port;
import org.snlab.network.Rule;
import org.snlab.network.Update;

public class Main {
    public static void main(String[] args) throws IOException {
        String[] devicenames = { "atla", "chic", "hous", "kans", "losa", "newy32aoa", "salt", "seat", "wash" };
        Collection<List<String>> pmts = Collections2.permutations(new ArrayList<>(Arrays.asList(devicenames)));
        for (List<String> dns : pmts) {
            test(dns.stream().toArray(String[]::new));
        }
    }

    public static void test(String[] devicenames) throws IOException {
        System.out.println("Good day!");
        long start = System.nanoTime();

        // String[] devicenames = {"atla", "chic", "hous", "kans", "losa", "newy32aoa",
        // "salt", "seat", "wash"};

        Network n = new Network();

        n.addDevice("default");
        for (String name : devicenames) {
            n.addDevice(name);
            n.addLink(name, "default", "default", name + "-peer-default");
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

        ModelManager verifier = new ModelManager(n, new PersistentPorts());

        LinkedList<Update> updates = new LinkedList<>();
        for (String name : devicenames) {
            File inputFile = new File("i2/" + name + "apnotcomp");
            Scanner in = new Scanner(inputFile);
            int i = 0;
            while (in.hasNextLine()) {
                if (i == 400) {
                    break;
                }
                i++;
                String linestr = in.nextLine();
                String[] tokens = linestr.split(" ");
                // System.out.println(Arrays.toString(tokens));
                if (tokens[0].equals("fw")) {
                    String portname = tokens[3].split("\\.")[0];
                    Port p = n.getDevice(name).getPortByName(portname);
                    if (p == null) {
                        p = n.getDevice(name).addPortByName(portname);
                    }
                    long ip = Long.parseLong(tokens[1]);
                    
                    Rule rule = new Rule(n.getDevice(name), ip, Integer.parseInt(tokens[2]), p);
                    Update update = new Update(n.getDevice(name), rule);
                    updates.add(update);

                    // this function will update PPM and check invariants
                    // Automata a = new Automata("atla,.*,wash,.*");
                    // n.addRuleAndCheckReachability(n.getDevice(name), rule, a);
                }
            }
        }

        Collections.shuffle(updates);

        for (String name : devicenames) {
            for (int i = updates.size(); i-- > 0;) {
                if (updates.get(i).getDevice().getName().equals(name)) {
                    updates.get(i).setLast(true);
                    break;
                }
            }
        }

        long ip = 522018560;
        ByteBuffer buffer = ByteBuffer.allocate(Long.BYTES).order(ByteOrder.nativeOrder());
        buffer.putLong((int) ip);
        int hs = model.bddEngine.encodeIpv4(BitSet.valueOf(buffer.array()), 8);
        HashSet<String> devnames = new HashSet<>(
                Arrays.asList("atla", "chic", "hous", "kans", "losa", "newy32aoa", "salt", "seat", "wash", "default"));
        Automata a = Automata.fromString("atla,.*,wash,.*", devnames);
        Property property = new Property(hs, "atla", a);
        int i = 0;
        boolean ed = false;
        for (Update update : updates) {
            i++;
            int r = n.addRuleEarly(update.getDevice(), update.getRule(), property, update.isLast());
            if (r == 3 || r == 4) {
                System.out.println("update idx: " + i + " " + r);
                writeFile1(String.valueOf(i) + " " + r);
                ed = true;
                break;
            }
        }
        if (!ed) {
            writeFile1("3600 " + 0);
        }

        System.out.println(model.predToPorts.size());
        System.out.println((System.nanoTime() - start) / 1000000000);
    }

    public static void writeFile1(String str) throws IOException {
        try {
            String filename = "out-sf.txt";
            FileWriter fw = new FileWriter(filename, true); // the true will append the new data
            fw.write(str + "\n");// appends the string to the file
            fw.close();
        } catch (IOException ioe) {
            System.err.println("IOException: " + ioe.getMessage());
        }
    }
}
