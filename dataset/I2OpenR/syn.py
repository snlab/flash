import networkx as nx
import matplotlib.pyplot as plt
import ipaddress

class Network:
    g = nx.MultiGraph()
    gprefix = 0
    rtr_list = ['wash', 'atla', 'hous',
                'losa', 'newy32aoa', 'chic',
                'kans', 'salt', 'seat']

    def __init__(self) -> None:
        self.add_routers()
        self.add_edge("chic", "xe-0/1/0", "newy32aoa", "xe-0/1/3")
        self.add_edge("chic", "xe-1/0/1", "kans", "xe-0/1/0")
        self.add_edge("chic", "xe-1/1/3", "wash", "xe-6/3/0")
        self.add_edge("hous", "xe-3/1/0", "losa", "ge-6/0/0")
        self.add_edge("kans", "ge-6/0/0", "salt", "ge-6/1/0")
        self.add_edge("chic", "xe-1/1/2", "atla", "xe-0/1/3")
        self.add_edge("seat", "xe-0/0/0", "salt", "xe-0/1/1")
        self.add_edge("chic", "xe-1/0/2", "kans", "xe-0/0/3")
        self.add_edge("hous", "xe-1/1/0", "kans", "xe-1/0/0")
        self.add_edge("seat", "xe-0/1/0", "losa", "xe-0/0/0")
        self.add_edge("salt", "xe-0/0/1", "losa", "xe-0/1/3")
        self.add_edge("seat", "xe-1/0/0", "salt", "xe-0/1/3")
        self.add_edge("newy32aoa", "et-3/0/0-0", "wash", "et-3/0/0-0")
        self.add_edge("newy32aoa", "et-3/0/0-1", "wash", "et-3/0/0-1")
        self.add_edge("chic", "xe-1/1/1", "atla", "xe-0/0/0")
        self.add_edge("losa", "xe-0/1/0", "seat", "xe-2/1/0")
        self.add_edge("hous", "xe-0/1/0", "losa", "ge-6/1/0")
        self.add_edge("atla", "xe-0/0/3", "wash", "xe-1/1/3")
        self.add_edge("hous", "xe-3/1/0", "kans", "ge-6/2/0")
        self.add_edge("atla", "ge-6/0/0", "hous", "xe-0/0/0")
        self.add_edge("chic", "xe-1/0/3", "kans", "xe-1/0/3")
        self.add_edge("losa", "xe-0/0/3", "salt", "xe-0/1/0")
        self.add_edge("atla", "ge-6/1/0", "hous", "xe-1/0/0")
        self.add_edge("atla", "xe-1/0/3", "wash", "xe-0/0/0")
        self.add_edge("chic", "xe-2/1/3", "wash", "xe-0/1/3")
        self.add_edge("atla", "xe-1/0/1", "wash", "xe-0/0/3")
        self.add_edge("kans", "xe-0/1/1", "salt", "ge-6/0/0")
        self.add_edge("chic", "xe-1/1/0", "newy32aoa", "xe-0/0/0")

    def add_routers(self):
        for rtr in self.rtr_list:
            self.g.add_node(rtr, type='router')

    def gen_prefix(self):
        prefix = '10.0.%d.0' % self.gprefix
        self.gprefix += 1
        return prefix

    def add_edge(self, src, srcp, dst, dstp):
        prefix = self.gen_prefix()
        g = self.g
        link_name = src + srcp + dst + dstp
        g.add_node(link_name, type='link', prefix=prefix,
                   attr={src: srcp, dst: dstp})
        g.add_edge(src, link_name)
        g.add_edge(dst, link_name)

    def gen_rules(self, file):
        g = self.g
        rules = []
        for n in self.g.nodes:
            if self.g.nodes[n]['type'] == 'router':
                print(n)
                for l in self.g.nodes:
                    if self.g.nodes[l]['type'] == 'link':
                        sp = nx.shortest_path(g, n, l)
                        intip = self.ip_to_integer(self.g.nodes[l]['prefix'])
                        rule = (n, str(intip),
                                '24', self.g.nodes[sp[1]]['attr'][n])
                        rules.append(rule)

        self.write_rules(file, rules)

    def write_rules(self, file, rules):
        with open(file, 'w') as f:
            for rule in rules:
                print(rule)
                f.write(' '.join(rule))
                f.write('\n')
    def ip_to_integer(self, string_ip):
        try:
            if type(ipaddress.ip_address(string_ip)) is ipaddress.IPv4Address:
                return int(ipaddress.IPv4Address(string_ip))
            if type(ipaddress.ip_address(string_ip)) is ipaddress.IPv6Address:
                return int(ipaddress.IPv6Address(string_ip))
        except Exception as e:
            return -1

    def draw(self):
        g = self.g
        print(g)
        nx.draw(g, with_labels=True, font_weight='bold')
        plt.show()


n = Network()
n.gen_rules('dataset/I2OpenR/trace.txt')
