package org.snlab.network;

public class Update {
    private String mode;
    private Device device;
    private Rule rule;
    private boolean isLast = false;

    public Update(String mode, Device device, Rule rule) {
        this.mode = mode;
        this.device = device;
        this.rule = rule;
    }

    /**
     * @return String return the mode
     */
    public String getMode() {
        return mode;
    }

    /**
     * @param mode the mode to set
     */
    public void setMode(String mode) {
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

}
