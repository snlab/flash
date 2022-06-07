import matplotlib.pyplot as plt

plt.figure(figsize=(8, 4))

def draw_epoch(file):
    data = {}

    for line in open(file):
        tokens = line.split()
        if tokens[3] in data.keys():
            continue
        data[tokens[3]] = int(tokens[1])

    plt.scatter(data.values(), data.keys(), marker='x')

def draw_loop_check(file):
    data = {}
    for line in open(file):
        tokens = line.split(' ')
        if tokens[3] in data.keys():
            continue
        data[tokens[3]] = int(line.split(': ')[1]) / 1000000
    plt.scatter(data.values(), data.keys())

draw_epoch('dataset/I2OpenR/trace.txt')
draw_epoch('dataset/I2OpenR/update.txt')
draw_loop_check('tmp/log.txt')
plt.grid()
plt.show()