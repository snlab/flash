package org.snlab.network;

public class Update {
    public enum Type {
        INSERT, DELETE
    }

    private Type mode;
    private Device device;
    private Rule rule;
    private boolean isLast = false;
    private String epoch = "0"; // epoch is "0" if not set

    public Update(Type mode, Device device, Rule rule) {
        this.mode = mode;
        this.device = device;
        this.rule = rule;
    }

    /**
     * @return String return the mode
     */
    public Type getMode() {
        return mode;
    }

    /**
     * @param mode the mode to set
     */
    public void setMode(Type mode) {
        this.mode = mode;
    }

    /**
     * @param device the device to set
     */
    public void setDevice(Device device) {
        this.device = device;
    }

    /**
     * @param rule the rule to set
     */
    public void setRule(Rule rule) {
        this.rule = rule;
    }

    /**
     * @return boolean return the isLast
     */
    public boolean isIsLast() {
        return isLast;
    }

    /**
     * @param isLast the isLast to set
     */
    public void setIsLast(boolean isLast) {
        this.isLast = isLast;
    }

    /**
     * @return Device return the device
     */
    public Device getDevice() {
        return device;
    }

    /**
     * @return Rule return the rule
     */
    public Rule getRule() {
        return rule;
    }


    /**
     * @return String return the epoch
     */
    public String getEpoch() {
        return epoch;
    }

    /**
     * @param epoch the epoch to set
     */
    public void setEpoch(String epoch) {
        this.epoch = epoch;
    }

}
