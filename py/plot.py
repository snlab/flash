
import sys
from I2LongTail import I2LongTail
from I2CE2D import I2CE2D
from I2EarlyDetection import I2EarlyDetection

if __name__ == '__main__':
    prog = sys.argv[1]
    if prog == 'I2CE2D':
        I2CE2D()
    elif prog == 'I2EarlyDetection':
        I2EarlyDetection()
    elif prog == 'I2LongTail':
        I2LongTail()