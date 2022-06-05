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


import org.snlab.flash.ModelManager.InverseModel;
import org.snlab.network.Network;
import org.snlab.network.Update;

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
                epochToInstance.put(update.getEpoch(), instance);

                instance.addUpdate(update);
            }
            activeInstances.add(epochToInstance.get(update.getEpoch()));
        }

        for (EpochInstance instance : activeInstances) {
            instance.active();
        }
    }

    private class EpochInstance {
        private String epoch;
        private BlockingQueue<Update> updateQueue = new LinkedBlockingQueue<>();
        private InverseModel model;
        private int batchSize;

        public EpochInstance(String epoch, Network network, int batchSize) {
            this.epoch = epoch;
            this.model = new InverseModel(network);
            this.batchSize = batchSize;
        }

        public void addUpdate(Update update) {
            this.updateQueue.add(update);
        }

        public void active() {

        }
    }
}
