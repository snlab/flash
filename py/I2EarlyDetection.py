import matplotlib.pyplot as plt
import seaborn as sns

class I2EarlyDetection:
    def __init__(self) -> None:
        x = []
        with open('tmp/log.txt') as f:
            lines = f.readlines()
            for i in range(len(lines)):
                if 'buggy' in lines[i] and i != len(lines) - 1:
                    n = lines[i + 1]
                    if 'at:' in n:
                        x.append(int(n.split(' ')[5])/1000000)
                    else:
                        x.append(60000)
        # print(len(x))
        # for i in range(len(x), 100):
        #     x.append(60000)
        # print(len(x))
        
        plt.figure(figsize=(8, 4))
        plt.xlim(0, 160)
        sns.ecdfplot(x)
        plt.grid()
        plt.show()