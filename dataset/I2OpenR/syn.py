import networkx as nx

g = nx.Graph()
g.add_nodes_from(['rwash', 'ratla', 'rhous',
                  'rlosa', 'rnew', 'rchic',
                  'rkans', 'rsalt', 'rseat'])
g.add_edges_from([('rwash', 'ratla'), ('rwash', 'rchic'), ('rwash', 'rnew'), ('rnew', 'rchic'),
                  ('ratla', 'rchic'), ('ratla', 'rhous'), ('rchic',
                                                           'rkans'), ('rhous', 'rkans'),
                  ('rhous', 'rlosa'), ('rkans', 'rsalt'), ('rlosa',
                                                           'rsalt'), ('rlosa', 'rseat'),
                  ('rsalt', 'rseat')
                  ])
print(g)

for n in g.nodes:
    for m in g.nodes:
        if n != m:
            sp = nx.shortest_path(g, n, m)
            print(sp)
# rwash, ratla, rhous, rlosa, rnew, rchic, rkans, rsalt, rseat = self.addRouters('rwash', 'ratla', 'rhous',
#                                                                                        'rlosa', 'rnew', 'rchic',
#                                                                                        'rkans', 'rsalt', 'rseat',
#                                                                                        config=OpenrConfig,
#                                                                                        privateDirs=['/tmp', '/var/log'])
#         self.addLink(rwash, ratla, loss=loss, delay=delay)
#         self.addLink(rwash, rchic, loss=loss, delay=delay)
#         link = self.addLink(rwash, rnew, loss=0, delay=delay)
#         self.addLink(rnew, rchic, loss=0, delay=delay)
#         self.addLink(ratla, rchic, loss=loss, delay=delay)
#         self.addLink(ratla, rhous, loss=loss, delay=delay)
#         self.addLink(rchic, rkans, loss=loss, delay=delay)
#         self.addLink(rhous, rkans, loss=loss, delay=delay)
#         self.addLink(rhous, rlosa, loss=loss, delay=delay)
#         self.addLink(rkans, rsalt, loss=loss, delay=delay)
#         self.addLink(rlosa, rsalt, loss=loss, delay=delay)
#         self.addLink(rlosa, rseat, loss=loss, delay=delay)
#         self.addLink(rsalt, rseat, loss=loss, delay=delay)
