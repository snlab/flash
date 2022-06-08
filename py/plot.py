import matplotlib.pyplot as plt
import seaborn as sns

plt.figure(figsize=(8, 4))

class I2EarlyDetection:
    def __init__(self) -> None:
        x = []
        with open('tmp/log.txt') as f:
            for line in f:
                if 'buggy' in line:
                    n = next(f)
                    if 'at:' in n:
                        x.append(int(n.split(' ')[5])/1000000)
        for i in range(len(x), 100):
            x.append(60000)
        print(len(x))
        plt.xlim(0, 160)
        sns.ecdfplot(x)
        plt.show()

class I2CE2D:
    def __init__(self) -> None:
        self.draw_epoch('dataset/I2OpenR/trace.txt')
        self.draw_epoch('dataset/I2OpenR/update.txt')
        self.draw_loop_check('tmp/log.txt')
        plt.grid()
        plt.show()
    def draw_epoch(self, file):
        data = {}

        for line in open(file):
            tokens = line.split()
            if tokens[3] in data.keys():
                continue
            data[tokens[3]] = int(tokens[1])

        plt.scatter(data.values(), data.keys(), marker='x')

    def draw_loop_check(self, file):
        data = {}
        for line in open(file):
            tokens = line.split(' ')
            if tokens[3] in data.keys():
                continue
            data[tokens[3]] = int(line.split(': ')[1]) / 1000000
        plt.scatter(data.values(), data.keys())

I2EarlyDetection()