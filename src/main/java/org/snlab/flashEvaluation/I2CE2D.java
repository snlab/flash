package org.snlab.flashEvaluation;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Scanner;

import org.snlab.flash.CE2D.EarlyDetector;
import org.snlab.flash.CE2D.Setting;
import org.snlab.flash.ModelManager.Changes;
import org.snlab.flash.ModelManager.InverseModel;
import org.snlab.network.Device;
import org.snlab.network.Network;
import org.snlab.network.Rule;
import org.snlab.network.Update;
import org.snlab.networkLoader.I2Network;

public class I2CE2D {
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
                Update update = new Update(tokens[0], device, rule);
                updates.add(update);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        for (Update update : updates) {
            Changes cgs;
            if (update.getMode().equals("+")) {
                cgs = model.miniBatch(Arrays.asList(update.getRule()), new ArrayList<>());
            } else {
                Rule rule = update.getDevice().getRule(update.getRule().getPrefix(), update.getRule().getPriority());
                if (rule == null) {
                    System.out.println("Rule not found");
                    continue;
                }
                cgs = model.miniBatch(new ArrayList<>(), Arrays.asList(rule));
            }
            model.update(cgs);
            Setting setting = new Setting(0, 0, System.nanoTime());
            
            EarlyDetector earlyDetector = new EarlyDetector();
            earlyDetector.detectLoop(setting, network, network.getAllDevices(),
                    model.getPortToPredicate());
        }
    }
}
