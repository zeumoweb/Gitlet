package gitlet;

import java.io.Serializable;

interface Dumpable extends Serializable {
    /** Print useful information about this object on System.out. */
    void dump();
}
