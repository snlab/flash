import matplotlib.pyplot as plt
import seaborn as sns

class I2LongTail:
    def __init__(self) -> None:
        data = {}
        for i in range(1, 8):
            data[i] = {}
        for line in open('tmp/log.txt'):
            nDampping = int(line.split(',')[0].split('=')[1])
            nCase = int(line.split('}')[0].split('=')[2])
            if nCase in data[nDampping].keys():
                continue

            time = int(line.split(' ')[5])/1000000
            data[nDampping][nCase] = time

        for (_, d) in data.items():
            for i in range(50):
                if i not in d.keys():
                    d[i] = 60000
            print(len(d))
        
        plt.figure(figsize=(8, 4))
        plt.xlim(0, 1000)
        pdata = {}
        for (k, d) in data.items():
            if k in range(1, 3):
                pdata[k] = d.values()
        sns.ecdfplot(pdata)
        
        plt.grid()
        plt.show()
        plt.close()

        plt.figure(figsize=(8, 4))
        plt.xlim(0, 1000)
        pdata = {}
        for (k, d) in data.items():
            if k in range(3, 8):
                pdata[k] = d.values()
        sns.ecdfplot(pdata)
        plt.grid()
        plt.show()

        
