#!/usr/bin/env python3

import sys, os
import matplotlib as mpl
import matplotlib.pyplot as plt
import numpy as np

def main(argv):
    argv[1]
    names = ["LNet0", "LNet1", "LNet*"] #, "Airtel1", "Stanford", "Internet2"]
    
    for name in names:
        f = open(argv[1] + "/" + name + "bPuUs.txt", "r")
        
        line = f.readline()
        totalSize = int(line)

        x, y = [], []
        while True:
            line = f.readline()
            if not line:
                break

            f_list = [float(i) for i in line.split(" ") if i.strip()]
            x.append(f_list[0] / totalSize)
            y.append(f_list[1])

        plt.plot(x, y, label=name)

    plt.xlabel('Batch size / total #updates')
    plt.ylabel('Model update time (s)')
    plt.yscale('log')
    plt.title("Model update time for different block size")
    os.makedirs('output', exist_ok=True)
    plt.savefig('output/batchSize.png')
    plt.show()

if __name__ == "__main__":
    main(sys.argv)
