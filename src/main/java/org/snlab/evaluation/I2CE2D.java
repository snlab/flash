package org.snlab.evaluation;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.DelayQueue;

import org.snlab.flash.Dispatcher;
import org.snlab.network.Device;
import org.snlab.network.Network;
import org.snlab.network.Rule;
import org.snlab.network.Update.Type;
import org.snlab.networkLoader.I2Network;

public class I2CE2D {
    static String mode = Main.evalOptions.mode; // PUV/BUV/CE2D
    public static long startAt;

    public static void run() {
        System.out.println("=== Start running using BUV ===");
        runBUV();
        System.out.println("=== Start running using PUV ===");
        runPUV();
        System.out.println("=== Start running using CE2D ===");
        runCE2D();
        Dispatcher.logger.writeFile();
    }

    private static List<UpdateTrace> loadTrace(Network network) {
        List<UpdateTrace> trace = new ArrayList<>();

        try {
            Scanner in = new Scanner(new File("dataset/I2OpenR/trace.txt"));
            while (in.hasNextLine()) {
                String line = in.nextLine();
                String[] tokens = line.split(" ");
                Device device = network.getDevice(tokens[3]);
                String pn = tokens[6].split("\\.")[0];
                if (device.getPort(pn) == null) {
                    device.addPort(pn);
                }
                long ip = Long.parseLong(tokens[4]);
                Rule rule = new Rule(device, ip, Integer.parseInt(tokens[5]), device.getPort(pn));
                device.addInitialRule(rule);
                network.addInitialRule(rule);
                UpdateTrace ut = new UpdateTrace(Type.INSERT, device, rule, Integer.valueOf(tokens[1]));
                ut.setEpoch(tokens[2]);
                trace.add(ut);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        try {
            Scanner in = new Scanner(new File("dataset/I2OpenR/update.txt"));
            while (in.hasNextLine()) {
                String line = in.nextLine();
                String[] tokens = line.split(" ");
                Device device = network.getDevice(tokens[3]);
                String pn = tokens[6].split("\\.")[0];
                if (device.getPort(pn) == null) {
                    device.addPort(pn);
                }
                long ip = Long.parseLong(tokens[4]);
                Rule rule = new Rule(device, ip, Integer.parseInt(tokens[5]), device.getPort(pn));
                Type type = tokens[0].equals("+") ? Type.INSERT : Type.DELETE;
                UpdateTrace update = new UpdateTrace(type, device, rule, Integer.valueOf(tokens[1]));
                update.setEpoch(tokens[2]);
                if (tokens[7].equals("1")) { // the last update
                    update.setIsLast(true);
                }
                trace.add(update);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return trace;
    }

    private static void runCE2D() {
        Network network = I2Network.getTopo();
        List<UpdateTrace> trace = loadTrace(network);
        DelayQueue<UpdateTrace> dq = new DelayQueue<>();
        dq.addAll(trace);

        Main.evalOptions.mode = "CE2D";
        Dispatcher dispatcher = new Dispatcher(network, 10);
        Dispatcher.logger.startAt = System.nanoTime();

        while (!dq.isEmpty()) {
            try {
                UpdateTrace update = dq.take();
                dispatcher.dispatch(update);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    private static void runBUV() {
        Network network = I2Network.getTopo();
        List<UpdateTrace> trace = loadTrace(network);
        DelayQueue<UpdateTrace> dq = new DelayQueue<>();
        dq.addAll(trace);

        Main.evalOptions.mode = "BUV";
        Dispatcher dispatcher = new Dispatcher(network, 10);
        Dispatcher.logger.startAt = System.nanoTime();

        while (!dq.isEmpty()) {
            try {
                UpdateTrace update = dq.take();
                dispatcher.dispatch(update);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    private static void runPUV() {
        Network network = I2Network.getTopo();
        List<UpdateTrace> trace = loadTrace(network);
        DelayQueue<UpdateTrace> dq = new DelayQueue<>();
        dq.addAll(trace);

        Main.evalOptions.mode = "PUV";
        Dispatcher dispatcher = new Dispatcher(network, 1);
        Dispatcher.logger.startAt = System.nanoTime();
        while (!dq.isEmpty()) {
            try {
                UpdateTrace update = dq.take();
                dispatcher.dispatch(update);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}
