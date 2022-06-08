
import sys
from I2LongTail import I2LongTail
from I2CE2D import I2CE2D
from I2EarlyDetection import I2EarlyDetection
from LNet1AllPair import LNet1AllPair

if __name__ == '__main__':
    prog = sys.argv[1]
    output = None
    if len(sys.argv) == 3:
        output = sys.argv[2]
    if prog == 'I2CE2D':
        I2CE2D(output)
    elif prog == 'I2EarlyDetection':
        I2EarlyDetection(output)
    elif prog == 'I2LongTail':
        I2LongTail(output)
    elif prog == 'LNet1AllPair':
        LNet1AllPair(output)