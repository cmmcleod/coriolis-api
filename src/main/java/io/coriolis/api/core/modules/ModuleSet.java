package io.coriolis.api.core.modules;

import cern.colt.bitvector.BitVector;

public class ModuleSet {

    private BitVector modules;

    protected ModuleSet(int bvSize) {
        modules = new BitVector(bvSize);
    }

    public BitVector getSet() {
        return modules;
    }

    public void add(int index) {
        modules.putQuick(index, true);
    }

    public boolean has(int index) {
        return modules.getQuick(index);
    }

}
