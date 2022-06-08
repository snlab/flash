package org.snlab.evaluation;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Scanner;
import java.util.Set;
import java.util.concurrent.DelayQueue;

import org.snlab.flash.Dispatcher;
import org.snlab.flash.CE2D.EarlyDetector;
import org.snlab.flash.CE2D.Setting;
import org.snlab.flash.ModelManager.Changes;
import org.snlab.flash.ModelManager.InverseModel;
import org.snlab.network.Device;
import org.snlab.network.Network;
import org.snlab.network.Rule;
import org.snlab.network.Update.Type;
import org.snlab.networkLoader.I2Network;

public class I2EarlyDetection {
    public static void run() {
        Network network = I2Network.getTopo();
        List<Rule> rules = new ArrayList<>();
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
                rules.add(rule);
                network.addInitialRule(rule);
                device.addInitialRule(rule);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        for (int i = 0; i < 100; i++) {
            /**
             * Must reload rules each run, else buggy rules always exist
             */
            network = I2Network.getTopo();
            List<UpdateTrace> updates = loadUpdates(network);
            
            DelayQueue<UpdateTrace> dq = new DelayQueue<>();
            dq.addAll(updates);
    
            Main.evalOptions.mode = "CE2D";
            Dispatcher dispatcher = new Dispatcher(network, 10);
            Dispatcher.logger.startAt = System.nanoTime();
    
            Device buggyDevice = network.getAllDevices().stream()
                    .skip((int) (network.getAllDevices().size() * Math.random())).findFirst().get();
            Device longTailDevice = network.getAllDevices().stream()
                    .skip((int) (network.getAllDevices().size() * Math.random())).findFirst().get();
            Dispatcher.logger.logPrintln("=== buggy device: " + buggyDevice.getName() + "; long tail device: " + longTailDevice.getName() + "===");

            while (!dq.isEmpty()) {
                try {
                    UpdateTrace update = dq.take();
                    
                    if (update.getDevice() == longTailDevice) {
                        // continue;
                    }
                    if (update.getDevice() == buggyDevice) {
                        update.getRule().setRandomOutPort();
                    }
                    dispatcher.dispatch(update);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
        Dispatcher.logger.writeFile();
    }

    private static List<UpdateTrace> loadUpdates(Network network) {
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
                if (tokens[7].equals("1")) { // the last update
                    ut.setIsLast(true);
                }
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
}
