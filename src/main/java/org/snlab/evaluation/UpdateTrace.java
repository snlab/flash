package org.snlab.evaluation;

import java.util.concurrent.Delayed;
import java.util.concurrent.TimeUnit;

import org.snlab.network.Device;
import org.snlab.network.Rule;
import org.snlab.network.Update;

public class UpdateTrace extends Update implements Delayed {
    private long delay;
    private long until;

    public UpdateTrace(Type mode, Device device, Rule rule) {
        super(mode, device, rule);
    }

    public UpdateTrace(Type mode, Device device, Rule rule, long delay) {
        super(mode, device, rule);
        this.until = System.currentTimeMillis() + delay;
    }

    @Override
    public int compareTo(Delayed o) {
        UpdateTrace other = (UpdateTrace) o;
        return until - other.until > 0 ? 1 : -1;
    }

    @Override
    public long getDelay(TimeUnit unit) {
        return until - System.currentTimeMillis();
    }
    
}
