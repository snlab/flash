package org.snlab.network;

public class Update {
    private Device device;
    private Rule rule;
    private boolean isLast = false;

    public Update(Device device, Rule rule) {
        this.device = device;
        this.rule = rule;
    }

    public Device getDevice() {
        return device;
    }

    public boolean isLast() {
        return isLast;
    }

    public void setLast(boolean last) {
        isLast = last;
    }

    public Rule getRule() {
        return rule;
    }
}
