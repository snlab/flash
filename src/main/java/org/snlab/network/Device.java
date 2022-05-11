package org.snlab.network;


import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;

public class Device {
    public static int cnt;
    public int uid;
    
    private String name;
    private HashMap<String, Port> nameToPort;
    private ArrayList<Rule> initialRules;
    private int type = 0; // 0: rsw; 1: fsw; 2: ssw

    /**
     * Create a device and add a default port.
     * The default port name is "default", be careful there is a physical port called "default"!
     * @param name the name of the device
     */
    public Device(String name) {
        this.name = name;
        this.nameToPort = new HashMap<>();
        this.addPort("default");
        this.initialRules = new ArrayList<>();
    }

    public void addInitialRule(Rule rule) {
        this.initialRules.add(rule);
    }

    public ArrayList<Rule> getInitialRules() {
        return initialRules;
    }

    public String getName() {
        return name;
    }

    public Collection<Port> getPorts() {
        return this.nameToPort.values();
    }

    public Port addPort(String name) {
        Port port = new Port(name);
        port.setDevice(this);
        this.nameToPort.put(name, port);
        return port;
    }

    public Port getPort(String name) {
        nameToPort.putIfAbsent(name, new Port(name, this));
        return this.nameToPort.get(name);
    }

    @Override
    public String toString() {
        return this.name;
    }

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }
}
