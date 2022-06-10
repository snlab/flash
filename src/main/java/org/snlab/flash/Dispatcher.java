package org.snlab.flash;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import org.snlab.flash.CE2D.EpochInstance;
import org.snlab.flash.CE2D.Logger;
import org.snlab.network.Network;
import org.snlab.network.Update;

/**
 * Dispatch updates to (epoch, subspace) model
 */

// Concerns: (1) with epoch v.s. without epoch; (2) with CE2D v.s. without CE2D
public class Dispatcher {
    public static Config config;
    public static Logger logger = new Logger(config.output);
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
                    /**
                     * only 2 epochs, the second epoch model cloned from the first, thus we can
                     * directly apply updates to the first model
                     */
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
}
