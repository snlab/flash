package org.snlab.network;

public class Port implements Comparable<Port>{
    private String name;
    private Device device;
    private Port peer = null;
    private int hash;
    private static int cnt = 0;

    public Port(String name) {
        this.name = name;
        this.hash = cnt++;
    }


    public Port(String name, Device device) {
        this.name = name;
        this.hash = cnt++;
        this.device = device;
    }

    public Device getPeerDevice() {
        if (this.peer == null) {
            return null;
        }
        return this.peer.device;
    }

    public Device getDevice() {
        return device;
    }

    public void setDevice(Device device) {
        this.device = device;
    }

    public Port getPeer() {
        return peer;
    }

    public void setPeer(Port peer) {
        this.peer = peer;
    }

    @Override
    public int compareTo(Port o) {
        return this.hash - o.hash;
//        return this.name.hashCode() - o.name.hashCode() + this.device.getName().hashCode() - o.device.getName().hashCode();
    }

    public String getName() {
        return name;
    }

    @Override
    public String toString() {
        return name;
    }
}
