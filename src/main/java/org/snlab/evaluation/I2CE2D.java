package org.snlab.evaluation;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Scanner;
import java.util.Set;

import com.google.common.collect.Lists;

import org.snlab.flash.CE2D.EarlyDetector;
import org.snlab.flash.CE2D.PropertyChecker;
import org.snlab.flash.CE2D.Setting;
import org.snlab.flash.ModelManager.Changes;
import org.snlab.flash.ModelManager.InverseModel;
import org.snlab.network.Device;
import org.snlab.network.Network;
import org.snlab.network.Rule;
import org.snlab.network.Update;
import org.snlab.network.Update.Type;
import org.snlab.networkLoader.I2Network;

public class I2CE2D {
    static String mode = Main.evalOptions.mode; // PUV/BUV/CE2D

    public static void run() {
        Network network = I2Network.getTopo();

        try {
            Scanner in = new Scanner(new File("dataset/I2OpenR/trace.txt"));
            while (in.hasNextLine()) {
                String line = in.nextLine();
                String[] tokens = line.split(" ");
                Device device = network.getDevice(tokens[1]);
                String pn = tokens[4].split("\\.")[0];
                if (device.getPort(pn) == null) {
                    device.addPort(pn);
                }
                long ip = Long.parseLong(tokens[2]);
                Rule rule = new Rule(device, ip, Integer.parseInt(tokens[3]), device.getPort(pn));
                device.addInitialRule(rule);
                network.addInitialRule(rule);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        InverseModel model = new InverseModel(network);
        for (Rule rule : network.getInitialRules()) {
            Changes cgs = model.insertMiniBatch(Arrays.asList(rule));
            model.update(cgs);
            Setting setting = new Setting(0, 0, System.nanoTime());

            EarlyDetector earlyDetector = new EarlyDetector();
            earlyDetector.detectLoop(setting, network, network.getAllDevices(),
                    model.getPortToPredicate());
        }

        System.out.println("=== apply updates ===");
        List<Update> updates = new ArrayList<>();
        try {
            Scanner in = new Scanner(new File("dataset/I2OpenR/update.txt"));
            while (in.hasNextLine()) {
                String line = in.nextLine();
                String[] tokens = line.split(" ");
                Device device = network.getDevice(tokens[1]);
                String pn = tokens[4].split("\\.")[0];
                if (device.getPort(pn) == null) {
                    device.addPort(pn);
                }
                long ip = Long.parseLong(tokens[2]);
                Rule rule = new Rule(device, ip, Integer.parseInt(tokens[3]), device.getPort(pn));
                Type type = tokens[0].equals("+") ? Type.INSERT : Type.DELETE;
                Update update = new Update(type, device, rule);
                if (tokens.length == 6) { // the last update
                    update.setIsLast(true);
                }
                updates.add(update);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (mode.equals("PUV")) {
            for (Update update : updates) {
                Changes cgs;
                if (update.getMode() == Type.INSERT) {
                    cgs = model.miniBatch(Arrays.asList(update.getRule()), new ArrayList<>());
                } else {
                    Rule rule = update.getDevice().getRule(update.getRule().getMatch(), update.getRule().getPrefix());
                    if (rule == null) {
                        System.out.println("Rule not found");
                        continue;
                    }
                    cgs = model.miniBatch(new ArrayList<>(), Arrays.asList(rule));
                }
                model.update(cgs);
                PropertyChecker propertyChcker = new PropertyChecker();
                propertyChcker.checkLoop(network, model.getPortToPredicate());
                if (propertyChcker.hasLoop) {
                    System.out.println(update.getRule().getDevice().getName());
                    System.out.println(update.getRule().getMatch());
                    break;
                }
            }
            
        } else if (mode.equals("BUV")) {
            /**
             * Batch size 10
             */
            List<List<Update>> partitions = Lists.partition(updates, 10);

            for (List<Update> subUpdates : partitions) {
                List<Rule> insertions = new ArrayList<>();
                List<Rule> deletions = new ArrayList<>();
                for (Update update : subUpdates) {
                    if (update.getMode() == Type.INSERT) {
                        insertions.add(update.getRule());
                    } else {
                        Rule rule = update.getDevice().getRule(update.getRule().getMatch(),
                                update.getRule().getPrefix());
                        deletions.add(rule);
                    }
                }
                Changes cgs = model.miniBatch(insertions, deletions);
                model.update(cgs);
                PropertyChecker propertyChcker = new PropertyChecker();
                propertyChcker.checkLoop(network, model.getPortToPredicate());
                if (propertyChcker.hasLoop) {
                    System.out.println(subUpdates.get(subUpdates.size() - 1).getDevice().getName());
                    break;
                }
            }
        } else if (mode.equals("CE2D")) {
            /**
             * only 2 epochs, the second epoch model cloned from the first, thus we can
             * directly apply updates to the first model
             */
            List<List<Update>> partitions = Lists.partition(updates, 10);

            EarlyDetector earlyDetector = new EarlyDetector();
            for (List<Update> subUpdates : partitions) {
                List<Rule> insertions = new ArrayList<>();
                List<Rule> deletions = new ArrayList<>();
                for (Update update : subUpdates) {
                    if (update.getMode() == Type.INSERT) {
                        insertions.add(update.getRule());
                    } else {
                        Rule rule = update.getDevice().getRule(update.getRule().getMatch(),
                                update.getRule().getPrefix());
                        deletions.add(rule);
                    }
                }
                Changes cgs = model.miniBatch(insertions, deletions);
                model.update(cgs);

                Setting setting = new Setting(0, 0, System.nanoTime());
                Set<Device> newClosed = new HashSet<>();
                for (Update update : subUpdates) {
                    if (update.isIsLast()) {
                        newClosed.add(update.getDevice());
                    }
                }
                earlyDetector.detectLoop(setting, network, newClosed, model.getPortToPredicate());
            }
        }
    }
}
