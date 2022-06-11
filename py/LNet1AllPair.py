import os
import matplotlib.pyplot as plt
import seaborn as sns

class LNet1AllPair:
    def __init__(self, output=None) -> None:
        os.system('java -jar flash.jar -e LNet1AllPair')
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
            os.makedirs('output', exist_ok=True)
            plt.savefig('output/%s.png' % output)
        else:
            plt.show()