
#!/bin/python
import argparse
from py.I2LongTail import I2LongTail
from py.I2CE2D import I2CE2D
from py.I2EarlyDetection import I2EarlyDetection
from py.LNet1AllPair import LNet1AllPair

if __name__ == '__main__':
    parser = argparse.ArgumentParser()
    parser.add_argument('-e', dest='evaluation', help='The EVALUATION to be run', required=True)
    parser.add_argument('-o', dest='output', help='The OUTPUT log file, default: tmp/log.txt')
                        
    args = parser.parse_args()
    eval = args.evaluation
    output = args.output

    if eval == 'I2CE2D':
        I2CE2D(output)
    elif eval == 'I2EarlyDetection':
        I2EarlyDetection(output)
    elif eval == 'I2LongTail':
        I2LongTail(output)
    elif eval == 'LNet1AllPair':
        LNet1AllPair(output)