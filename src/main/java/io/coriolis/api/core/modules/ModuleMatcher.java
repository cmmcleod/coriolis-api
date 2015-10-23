package io.coriolis.api.core.modules;

import cern.colt.bitvector.BitVector;

/**
 * Created by cmmcleod on 10/16/15.
 */
public class ModuleMatcher {

    private BitVector original;
    private BitVector buffer;
    private int setSize;
    private int count;

    public ModuleMatcher(ModuleSet set) {
        original = set.getSet();
        count = original.cardinality();
        setSize = original.size();
        buffer = original.copy();
    }

    public int count() {
        return count;
    }

    public int match(ModuleSet ms) {
        if (ms == null || ms.getSet() == null) {
            return 0;
        }
        buffer.and(ms.getSet());
        int matchCount = buffer.cardinality();
        buffer.replaceFromToWith(0, setSize - 1, original, 0);
        return matchCount;
    }

}
