package org.snlab.network;

import org.jgrapht.alg.util.Pair;

import java.util.*;

public class Network {
    //    private Graph<Device, Link> graph;
    private final LinkedHashMap<String, Device> nameToDevice;
    private ArrayList<Rule> initialRules;
    private final HashSet<Device> devices; // for quick check rules related to a partition
    public int nLinks = 0;

    public ArrayList<Pair<Boolean, Rule>> updateSequence;

    private final ArrayList<ACLUse> initialACLs;
    private String name;

    public Network(String name) {
        this.nameToDevice = new LinkedHashMap<>();
        this.initialRules = new ArrayList<>();
        this.initialACLs = new ArrayList<>();
        this.devices = new HashSet<>();
        this.name = name;
    }

    public Network() {
        this("default");
    }

    public Map<String, Device> getDevices() {
        return this.nameToDevice;
    }

    public Network setName(String name) {
        this.name = name;
        return this;
    }

    public String getName() {
        return this.name;
    }

    public void addACL(ACLUse acl) {
        this.initialACLs.add(acl);
    }

    public ArrayList<Rule> getInitialRules() {
        return initialRules;
    }

    public void addInitialRule(Rule rule) {
        this.initialRules.add(rule);
    }

    public Device addDevice(String name) {
        Device device = new Device(name);
        this.nameToDevice.put(name, device);
        this.devices.add(device);
        return device;
    }

    public void addDevice(Device device) {
        this.nameToDevice.put(device.getName(), device);
        this.devices.add(device);
    }

    public Device getDevice(String name) {
        if (!this.nameToDevice.containsKey(name)) {
            Device device = new Device(name);
            this.nameToDevice.put(name, device);
            this.devices.add(device);
            return device;
        }
        return this.nameToDevice.get(name);
    }

    public HashSet<Device> getAllDevices() {
        return this.devices;
    }

    public void addLink(String device1, String device1Port, String device2, String device2Port) {
        nLinks++;
        Device dev1 = this.nameToDevice.get(device1);
        Port port1 = dev1.getPort(device1Port);
        if (port1 == null) {
            port1 = dev1.addPort(device1Port);
        }
        Device dev2 = this.nameToDevice.get(device2);
        Port port2 = dev2.getPort(device2Port);
        if (port2 == null) {
            port2 = dev2.addPort(device2Port);
        }
        port1.setPeer(port2);
        port2.setPeer(port1);
    }

    public Network getSubNetwork(Collection<Device> devices) {
        Network n = new Network();
        for (Device device : devices) {
            n.addDevice(device);
        }

//        for (Rule rule : this.initialRules) {
//            if (devices.contains(rule.getDevice())) {
//                n.addInitialRule(rule);
//            }
//        }
        return n;
    }

    public void filterIntoSubsapce(long subnet, long mask) {
        ArrayList<Rule> filtered = new ArrayList<>();
        for (Rule rule : initialRules) {
            if ((rule.getMatch().longValue() & mask) == subnet) filtered.add(rule);
        }
        this.initialRules = filtered;
    }
}
