package org.snlab.evaluation;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;

import org.snlab.flash.Dispatcher;
import org.snlab.flash.CE2D.EarlyDetector;
import org.snlab.flash.CE2D.Setting;
import org.snlab.flash.ModelManager.InverseModel;
import org.snlab.flash.ModelManager.Changes;
import org.snlab.network.Device;
import org.snlab.network.Network;
import org.snlab.networkLoader.I2Network;

public class I2LongTail {
    public static void run() {
        Network network = I2Network.getNetwork();
        for (Device device : network.getAllDevices()) {
            device.getInitialRules().get(device.getInitialRules().size() - 1).setLast(true);
        }

        for (int i = 1; i < 8; i++) {
            for (int cnt = 0; cnt < 50; cnt++) {
                List<Device> shuffled = new ArrayList<>(network.getAllDevices());
                Collections.shuffle(shuffled);
                List<Device> remains = shuffled.subList(0, 9 - i);

                InverseModel verifier = new InverseModel(network);
                EarlyDetector earlyDetector = new EarlyDetector();
                long startAt = System.nanoTime();
                
                for (Device device : remains) {
                    Changes changes = verifier.insertMiniBatch(device.getInitialRules());
                    verifier.update(changes);
                    Setting setting = new Setting(i, cnt, startAt);
                    earlyDetector.detectLoop(setting, network, new HashSet<>(Arrays.asList(device)),
                            verifier.getPortToPredicate());
                }
            }
        }
        Dispatcher.logger.writeFile();
    }
}
