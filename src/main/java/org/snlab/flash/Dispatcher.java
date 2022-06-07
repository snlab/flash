package org.snlab.flash;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import org.snlab.evaluation.Main;
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

/**
 * Dispatch updates to (epoch, subspace) model
 */

// Concerns: (1) with epoch v.s. without epoch; (2) with CE2D v.s. without CE2D
public class Dispatcher {
    private Network network;
    private BlockingQueue<Update> updateQueue = new LinkedBlockingQueue<>();
    private Map<String, EpochInstance> epochToInstance = new HashMap<>();
    private int batchSize = 1;

    public Dispatcher(Network network) {
        this(network, 1);
    }

    public Dispatcher(Network network, int batchSize) {
        this.network = network;
        this.batchSize = batchSize;
    }

    public void dispatch(Update update) {
        dispatch(Arrays.asList(update));
    }

    public void dispatch(List<Update> updates) {
        updateQueue.addAll(updates);
        Set<EpochInstance> activeInstances = new HashSet<>();
        for (Update update : updates) {
            if (!epochToInstance.containsKey(update.getEpoch())) {
                EpochInstance instance = new EpochInstance(update.getEpoch(), network, batchSize);
                if (update.getEpoch().equals("1")) {
                    instance.model = epochToInstance.get("0").model;
                }
                epochToInstance.put(update.getEpoch(), instance);
            }

            epochToInstance.get(update.getEpoch()).addUpdate(update);
            activeInstances.add(epochToInstance.get(update.getEpoch()));
        }

        for (EpochInstance instance : activeInstances) {
            instance.active();
        }
    }

    private class EpochInstance {
        private String epoch;
        private BlockingQueue<Update> updateQueue = new LinkedBlockingQueue<>();
        public InverseModel model;
        private int batchSize;
        private EarlyDetector earlyDetector = new EarlyDetector();

        public EpochInstance(String epoch, Network network, int batchSize) {
            this.epoch = epoch;
            this.model = new InverseModel(network);
            this.batchSize = batchSize;
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
                Changes cgs = model.miniBatch(insertions, deletions);
                model.update(cgs);
                if (Main.evalOptions.mode.equals("PUV") || Main.evalOptions.mode.equals("BUV")) {
                    PropertyChecker propertyChcker = new PropertyChecker();
                    propertyChcker.checkLoop(network, model.getPortToPredicate());
                    if (propertyChcker.hasLoop) {
                        System.out.println(updates.get(updates.size() - 1).getDevice().getName());
                        System.out.println(updates.get(updates.size() - 1).getRule().getMatch());
                        break;
                    }
                } else {
                    Setting setting = new Setting(0, 0, System.nanoTime());
                    Set<Device> newClosed = new HashSet<>();
                    for (Update update : updates) {
                        if (update.isIsLast()) {
                            newClosed.add(update.getDevice());
                        }
                    }
                    earlyDetector.detectLoop(setting, network, newClosed, model.getPortToPredicate());
                }
            }
        }
    }
}
