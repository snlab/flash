package org.snlab.flash.CE2D;

import java.math.BigInteger;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Set;

import org.snlab.network.Device;
import org.snlab.network.Network;
import org.snlab.network.Port;
import org.snlab.network.Rule;

public class Epoch {
    private long epochId;
    private Network network;
    private Set<Device> closedDevices;

    public Epoch(long epochId, Network network) {
        this.epochId = epochId;
        this.network = network;
        this.closedDevices = new HashSet<>();
    }

    private static <T> T random(Collection<T> coll) {
        int num = (int) (Math.random() * coll.size());
        for (T t : coll)
            if (--num < 0)
                return t;
        throw new AssertionError();
    }

    public void addFib(RouteDatabase routeDatabase) {
        System.out.println("add fib from " + routeDatabase.thisNodeName);
        for (UnicastRoute route : routeDatabase.unicastRoutes) {
            BigInteger ip = new BigInteger(route.dest.prefixAddress.addr);
            String devname = routeDatabase.thisNodeName;
            String portname = route.nextHops.get(0).address.ifName;
            Port p = network.getDevices().get(devname).getPortByName(portname);
            if (p == null) {
                p = network.getDevices().get(devname).addPortByName(portname);
            }
            p = random(network.getDevices().get(devname).getPorts().values());
            Rule rule = new Rule(ip, route.dest.prefixLength, p);
            LinkedList<Device> result = network.addRuleAndVerifyInvariants(network.getDevices().get(devname), rule);
            if (result != null) {
                int r = early(result);
                if (r != 0) {
                    System.out.println("found an unfixable bug! " + System.nanoTime());
                }
            }
        }
        System.out.println(this.closedDevices);
        this.closedDevices.add(network.getDevices().get(routeDatabase.thisNodeName));
        System.out.println("epoch: " + routeDatabase.epochId);
        System.out.println("total #ecs: " + this.network.model.predToPorts.size());
        System.out.println("finish add fib. " + System.nanoTime());
    }

    /**
     *
     * @param history
     * @return 0: cannot judge bug; 1: bug cannot be fixed
     */
    private int early(LinkedList<Device> history) {
        for (Device d : history) {
            if (!this.closedDevices.contains(d)) {
                return 0;
            }
        }
        return 1;
    }
}
