import os
import matplotlib.pyplot as plt
import seaborn as sns


class I2CE2D:
    def __init__(self, output=None) -> None:
        plt.figure(figsize=(8, 4))
        os.system('mvn exec:java -Dexec.args="-e I2CE2D"')
        self.draw_epoch('dataset/I2OpenR/trace.txt')
        self.draw_epoch('dataset/I2OpenR/update.txt')
        self.draw_loop_check('tmp/log.txt')

        plt.grid()
        plt.xlim(-5)
        if output:
            plt.savefig(output)
        else:
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
        with open(file) as f:
            lines = f.readlines()
            for i in range(len(lines)):
                line = lines[i]
                if 'Start' in line:
                    if i != len(lines) - 1:
                        data[line.split(' ')[4]] = int(lines[i + 1].split(': ')[1]) / 1000000
                    else:
                        data[line.split(' ')[4]] = -100 # CE2D has no loop
                        
        # for line in open(file):
        #     tokens = line.split(' ')
        #     if tokens[3] in data.keys():
        #         continue
        #     data[tokens[3]] = int(line.split(': ')[1]) / 1000000
        plt.scatter(data.values(), data.keys())
