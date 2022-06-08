import matplotlib.pyplot as plt
import seaborn as sns


class I2CE2D:
    def __init__(self, output=None) -> None:
        self.draw_epoch('dataset/I2OpenR/trace.txt')
        self.draw_epoch('dataset/I2OpenR/update.txt')
        self.draw_loop_check('tmp/log.txt')

        plt.figure(figsize=(8, 4))
        plt.grid()
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
        for line in open(file):
            tokens = line.split(' ')
            if tokens[3] in data.keys():
                continue
            data[tokens[3]] = int(line.split(': ')[1]) / 1000000
        plt.scatter(data.values(), data.keys())
