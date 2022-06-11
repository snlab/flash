package org.snlab.networkLoader;

import org.snlab.network.Device;
import org.snlab.network.Network;
import org.snlab.network.Rule;

public class LNetNetwork {
    private static final int npod = 112;

    private static Network buildTopology() {
        Network n = new Network("FBNetwork");
        //// create devices
        for (int iPod = 0; iPod < npod; iPod++) {
            for (int j = 0; j < 48; j++) {
                Device device = n.addDevice("rsw-" + iPod + "-" + j);
                // add rsw -> host port
                device.addPort(String.format("rsw-%d-%d>h-%d-%d", iPod, j, iPod, j));
            }

            for (int j = 0; j < 4; j++) {
                n.addDevice("fsw-" + iPod + "-" + j);
            }
        }
        for (int iSpine = 0; iSpine < 4; iSpine++) {
            for (int j = 0; j < 48; j++) {
                n.addDevice("ssw-" + iSpine + "-" + j);
            }
        }
        //// \create devices

        //// create links
        for (int iPod = 0; iPod < npod; iPod++) {
            for (int iFsw = 0; iFsw < 4; iFsw++) {
                String fsw = "fsw-" + iPod + "-" + iFsw;
                // down links
                for (int iRsw = 0; iRsw < 48; iRsw++) {
                    String rsw = "rsw-" + iPod + "-" + iRsw;
                    n.addLink(fsw, fsw + ">" + rsw, rsw, rsw + ">" + fsw);
                }
                // up links
                for (int iSsw = 0; iSsw < 48; iSsw++) {
                    String ssw = "ssw-" + iFsw + "-" + iSsw;
                    n.addLink(fsw, fsw + ">" + ssw, ssw, ssw + ">" + fsw);
                }
            }
        }
        //// \create links
        for (Device device : n.getAllDevices()) device.uid = Device.cnt++;
        return n;
    }

    /**
     * A data center network with synthesized shortest path rules
     * @return
     */
    public static Network getLNET() {
        Network n = buildTopology();

        //// create rules
        // first 8 bits is pod id, next 8 bit is index of rsw in pod
        for (int iPod = 0; iPod < npod; iPod++) {
            for (int iRsw = 0; iRsw < 48; iRsw++) {
                String rsw = "rsw-" + iPod + "-" + iRsw;
                Device device = n.getDevice(rsw);
                for (int jPod = 0; jPod < npod; jPod++) {
                    for (int jRsw = 0; jRsw < 48; jRsw++) {
                        String dstrsw = "rsw-" + jPod + "-" + jRsw;
                        long dstip = (jPod << 24) + ((jRsw + 1) << 16);
                        Rule rule;
                        if (dstrsw.equals(rsw)) {
                            rule = new Rule(device, dstip, 16, device.getPort(rsw + ">h-" + jPod + "-" + jRsw));
                        } else {
                            String dstfsw = "fsw-" + iPod + "-" + (48 * jPod + jRsw) % 4;
                            rule = new Rule(device, dstip, 16, device.getPort(rsw + ">" + dstfsw));
                        }
                        n.addInitialRule(rule);
                        device.addInitialRule(rule);
                    }
                }
            }

            for (int iSpine = 0; iSpine < 4; iSpine++) { // spine id
                String fsw = "fsw-" + iPod + "-" + iSpine;
                Device device = n.getDevice(fsw);
                for (int jPod = 0; jPod < npod; jPod++) {
                    for (int jRsw = 0; jRsw < 48; jRsw++) {
                        String dstrsw = "rsw-" + jPod + "-" + jRsw;
                        long dstip = (jPod << 24) + ((jRsw + 1) << 16);
                        Rule rule;
                        if (jPod == iPod) { // intra pod
                            rule = new Rule(device, dstip, 16, device.getPort(fsw + ">" + dstrsw));
                        } else {
                            String dstssw = "ssw-" + iSpine + "-" + (48 * jPod + jRsw) % 48;
                            rule = new Rule(device, dstip, 16, device.getPort(fsw + ">" + dstssw));
                        }
                        n.addInitialRule(rule);
                        device.addInitialRule(rule);
                    }
                }
            }
        }
        for (int iSpine = 0; iSpine < 4; iSpine++) { // spine id
            for (int iSsw = 0; iSsw < 48; iSsw++) {
                String ssw = "ssw-" + iSpine + "-" + iSsw;
                Device device = n.getDevice(ssw);
                for (int k = 0; k < npod; k++) {
                    for (int l = 0; l < 48; l++) {
                        long dstip = (k << 24) + ((l + 1) << 16);
                        String dstfsw = "fsw-" + k + "-" + iSpine;
                        Rule rule = new Rule(device, dstip, 16, device.getPort(ssw + ">" + dstfsw));
                        n.addInitialRule(rule);
                        device.addInitialRule(rule);
                    }
                }
            }
        }
        //// \create rules

        return n;
    }

    /**
     * A synthesized data set using 8-16 bits as src id for ECMP to Pods 0-5
     */
    public static Network getLNET1() {
        Network n = buildTopology();

        //// create rules
        // first 8 bits is pod id, next 8 bit is index of rsw in pod
        for (int iPod = 0; iPod < npod; iPod++) {
            for (int iRsw = 0; iRsw < 48; iRsw++) {
                String rsw = "rsw-" + iPod + "-" + iRsw;
                Device device = n.getDevice(rsw);
                for (int jPod = 0; jPod < npod; jPod++) {
                    for (int jRsw = 0; jRsw < 48; jRsw++) {
                        String dstrsw = "rsw-" + jPod + "-" + jRsw;
                        long dstip = (jPod << 24) + ((jRsw + 1) << 16);
                        Rule rule;
                        if (dstrsw.equals(rsw)) {
                            rule = new Rule(device, dstip, 16, device.getPort(rsw + ">h-" + jPod + "-" + jRsw));
                        } else {
                            String dstfsw = "fsw-" + iPod + "-" + (48 * jPod + jRsw) % 4;
                            rule = new Rule(device, dstip, 16, device.getPort(rsw + ">" + dstfsw));
                        }
                        n.addInitialRule(rule);
                        device.addInitialRule(rule);
                    }
                }
            }

            for (int iSpine = 0; iSpine < 4; iSpine++) { // spine id
                String fsw = "fsw-" + iPod + "-" + iSpine;
                Device device = n.getDevice(fsw);
                for (int jPod = 0; jPod < npod; jPod++) {
                    for (int jRsw = 0; jRsw < 48; jRsw++) {
                        String dstrsw = "rsw-" + jPod + "-" + jRsw;
                        long dstip = (jPod << 24) + ((jRsw + 1) << 16);
                        Rule rule;
                        if (jPod == iPod) { // intra pod
                            rule = new Rule(device, dstip, 16, device.getPort(fsw + ">" + dstrsw));
                            n.addInitialRule(rule);
                            device.addInitialRule(rule);
                        } else {
                            if (iPod == 0 && (jPod < 5)) {
                                for (int i = 0; i < 48; i++) {
                                    long srcdstip = dstip + (i << 8);
                                    String dstssw = "ssw-" + iSpine + "-" + (i);
                                    rule = new Rule(device, srcdstip, 24, device.getPort(fsw + ">" + dstssw));
                                    n.addInitialRule(rule);
                                    device.addInitialRule(rule);
                                }
                            } else {
                                String dstssw = "ssw-" + iSpine + "-" + (48 * jPod + jRsw) % 48;
                                rule = new Rule(device, dstip, 16, device.getPort(fsw + ">" + dstssw));
                                n.addInitialRule(rule);
                                device.addInitialRule(rule);
                            }
                        }
                    }
                }
            }
        }
        for (int iSpine = 0; iSpine < 4; iSpine++) { // spine id
            for (int iSsw = 0; iSsw < 48; iSsw++) {
                String ssw = "ssw-" + iSpine + "-" + iSsw;
                Device device = n.getDevice(ssw);
                for (int k = 0; k < npod; k++) {
                    for (int l = 0; l < 48; l++) {
                        long dstip = (k << 24) + ((l + 1) << 16);
                        String dstfsw = "fsw-" + k + "-" + iSpine;
                        Rule rule = new Rule(device, dstip, 16, device.getPort(ssw + ">" + dstfsw));
                        n.addInitialRule(rule);
                        device.addInitialRule(rule);
                    }
                }
            }
        }
        //// \create rules

        return n;
    }

    /**
     * A synthesized data set with suffix match
     * @return
     */
    public static Network getLNETStar() {
        Network n = buildTopology();

        //// create rules
        // first 8 bits is pod id, next 8 bit is index of rsw in pod
        for (int iPod = 0; iPod < npod; iPod++) {
            for (int iRsw = 0; iRsw < 48; iRsw++) {
                String rsw = "rsw-" + iPod + "-" + iRsw;
                Device device = n.getDevice(rsw);
                for (int jPod = 0; jPod < npod; jPod++) {
                    for (int jRsw = 0; jRsw < 48; jRsw++) {
//                        double pro = 1 - n * n;
                        String dstrsw = "rsw-" + jPod + "-" + jRsw;
                        long dstip = (jPod << 24) + ((jRsw + 1) << 16);
                        Rule rule;
                        if (dstrsw.equals(rsw)) {
                            rule = new Rule(device, dstip, 24, device.getPort(rsw + ">h-" + jPod + "-" + jRsw));
                        } else {
                            String dstfsw = "fsw-" + iPod + "-" + (48 * jPod + jRsw) % 4;
                            rule = new Rule(device, dstip, 24, device.getPort(rsw + ">" + dstfsw));
                        }
                        n.addInitialRule(rule);
                        device.addInitialRule(rule);
                    }
                }
            }

            for (int iSpine = 0; iSpine < 4; iSpine++) { // spine id
                String fsw = "fsw-" + iPod + "-" + iSpine;
                Device device = n.getDevice(fsw);
                for (int jPod = 0; jPod < npod; jPod++) {
                    for (int jRsw = 0; jRsw < 48; jRsw++) {
                        String dstrsw = "rsw-" + jPod + "-" + jRsw;
                        long dstip = (jPod << 24) + ((jRsw + 1) << 16);
                        Rule rule;
                        if (jPod == iPod) { // intra pod
                            rule = new Rule(device, dstip, 24, device.getPort(fsw + ">" + dstrsw));
                            n.addInitialRule(rule);
                            device.addInitialRule(rule);
                        } else {
                            if ((jPod < 5)) {
                                for (int i = 0; i < npod; i++) {
                                    String dstssw = "ssw-" + iSpine + "-" + (48 * jPod + jRsw) % 48;
                                    rule = new Rule(device, i, 24, dstip, 24, device.getPort(fsw + ">" + dstssw));
                                    rule.setPriority(31);
                                    n.addInitialRule(rule);
                                    device.addInitialRule(rule);
                                }
                            }
                            String dstssw = "ssw-" + iSpine + "-" + (48 * jPod + jRsw) % 48;
                            rule = new Rule(device, dstip, 24, device.getPort(fsw + ">" + dstssw));
                            n.addInitialRule(rule);
                            device.addInitialRule(rule);
                        }
                    }
                }
            }
        }
        for (int iSpine = 0; iSpine < 4; iSpine++) { // spine id
            for (int iSsw = 0; iSsw < 48; iSsw++) {
                String ssw = "ssw-" + iSpine + "-" + iSsw;
                Device device = n.getDevice(ssw);
                for (int k = 0; k < npod; k++) {
                    for (int l = 0; l < 48; l++) {
                        long dstip = (k << 24) + ((l + 1) << 16);
                        String dstfsw = "fsw-" + k + "-" + iSpine;
                        Rule rule = new Rule(device, dstip, 24, device.getPort(ssw + ">" + dstfsw));
                        n.addInitialRule(rule);
                        device.addInitialRule(rule);
                    }
                }
            }
        }

        return n;
    }
}
