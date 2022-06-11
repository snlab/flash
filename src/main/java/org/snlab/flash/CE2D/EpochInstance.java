package org.snlab.flash.CE2D;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import org.snlab.evaluation.Main;
import org.snlab.flash.Dispatcher;
import org.snlab.flash.ModelManager.ConflictFreeChanges;
import org.snlab.flash.ModelManager.InverseModel;
import org.snlab.network.Device;
import org.snlab.network.Network;
import org.snlab.network.Rule;
import org.snlab.network.Update;
import org.snlab.network.Update.Type;

public class EpochInstance {
    private String epoch;
    private Network network;
    private BlockingQueue<Update> updateQueue = new LinkedBlockingQueue<>();
    public InverseModel model;
    private int batchSize;
    private EarlyDetector earlyDetector = new EarlyDetector();
    private PropertyChecker propertyChecker = new PropertyChecker();

    public EpochInstance(String epoch, Network network, int batchSize) {
        this.epoch = epoch;
        this.network = network;
        this.model = new InverseModel(network);
        this.batchSize = batchSize;
        earlyDetector.useSingleThread = true;
    }

    public void addUpdate(Update update) {
        this.updateQueue.add(update);
    }

    public void active() {
        while (updateQueue.size() >= batchSize) {
            List<Update> updates = new ArrayList<>();
            updateQueue.drainTo(updates, batchSize);

            List<Rule> insertions = new ArrayList<>();
            List<Rule> deletions = new ArrayList<>();
            for (Update update : updates) {
                if (update.getMode() == Type.INSERT) {
                    insertions.add(update.getRule());
                } else {
                    Rule rule = update.getDevice().getRule(update.getRule().getMatch(),
                            update.getRule().getPrefix());
                    deletions.add(rule);
                }
            }
            ConflictFreeChanges cgs = model.miniBatch(insertions, deletions);
            Set<Integer> transfered = model.update(cgs);

            if (Main.evalOptions.mode.equals("PUV") || Main.evalOptions.mode.equals("BUV")) {

                if (this.epoch.equals(Main.evalOptions.checkEpoch) && transfered.size() > 0) {
                    propertyChecker.checkLoop(network, model.getPortToPredicate(), transfered);
                    if (propertyChecker.hasLoop) {
                        Dispatcher.logger.logPrintln("Found loop using " + (batchSize > 1 ? "BUV" : "PUV") + " at time: "
                                + (System.nanoTime() - Dispatcher.logger.startAt));
                        // System.out.println(updates.get(updates.size() - 1).getDevice().getName());
                        // System.out.println(updates.get(updates.size() - 1).getRule().getMatch());
                        // break;
                    }
                }
            } else {
                Setting setting = new Setting(0, 0, 0);
                Set<Device> newClosed = new HashSet<>();
                for (Update update : updates) {
                    if (update.isIsLast()) {
                        newClosed.add(update.getDevice());
                    }
                }
                earlyDetector.detectLoop(setting, network, newClosed, model.getPortToPredicate(), null);
            }
        }
    }
}
