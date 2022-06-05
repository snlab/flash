package org.snlab.evaluation;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Scanner;
import java.util.Set;

import org.snlab.flash.CE2D.EarlyDetector;
import org.snlab.flash.CE2D.Setting;
import org.snlab.flash.ModelManager.Changes;
import org.snlab.flash.ModelManager.InverseModel;
import org.snlab.network.Device;
import org.snlab.network.Network;
import org.snlab.network.Rule;
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
                Device device = network.getDevice(tokens[1]);
                String pn = tokens[4].split("\\.")[0];
                if (device.getPort(pn) == null) {
                    device.addPort(pn);
                }
                long ip = Long.parseLong(tokens[2]);
                Rule rule = new Rule(device, ip, Integer.parseInt(tokens[3]), device.getPort(pn));
                rules.add(rule);
                network.addInitialRule(rule);
                device.addInitialRule(rule);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        for (int i = 0; i < 4; i++) {
            /**
             * Must reload rules each run, else buggy rules always exist
             */
            network = I2Network.getTopo();

            for (Rule rule : rules) {
                Device device = network.getDevice(rule.getDevice().getName());
                Rule r = new Rule(device, rule.getMatch().longValue(), rule.getPrefix(), device.getPort(rule.getOutPort().getName()));
                network.addInitialRule(r);
                device.addInitialRule(r);
            }
            
            InverseModel model = new InverseModel(network);
            EarlyDetector earlyDetector = new EarlyDetector();
            earlyDetector.useSingleThread = true;
            long startAt = System.nanoTime();
            Device buggyDevice = network.getAllDevices().stream()
                    .skip((int) (network.getAllDevices().size() * Math.random())).findFirst().get();
            System.out.println("buggy device: " + buggyDevice.getName());
            
            for (Device device : network.getAllDevices()) {
                if (device == buggyDevice) {
                    // Rule buggyRule =
                    // device.getInitialRules().stream().skip((int)(device.getInitialRules().size()
                    // * Math.random())).findFirst().get();
                    // buggyRule.setRandomOutPort();
                    for (Rule rule : device.getInitialRules()) {
                        rule.setRandomOutPort();
                    }
                }
                Changes changes = model.insertMiniBatch(device.getInitialRules());
                model.update(changes);
                Setting setting = new Setting(0, 0, startAt);
                Set<Device> newClosed = new HashSet<>();
                newClosed.add(device);
                earlyDetector.detectLoop(setting, network, newClosed, model.getPortToPredicate());
                if (earlyDetector.hasLoop()) {
                    break;
                }
            }
        }
    }
}
