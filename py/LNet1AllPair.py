import os
import matplotlib.pyplot as plt
import seaborn as sns

class LNet1AllPair:
    def __init__(self, output=None) -> None:
        os.system('mvn exec:exec -Dexec.args="-e LNet1AllPair"')
        x = []
        for line in open('tmp/log.txt'):
            if 'time' in line:
                data = int(line.split(': ')[1])/1000000
                # if data > 100:
                #     continue
                x.append(data)
        
        plt.figure(figsize=(8, 4))
        plt.xlim(0, 30)
        sns.ecdfplot(x)
        plt.grid()
        if output:
            plt.savefig(output)
        else:
            plt.show()