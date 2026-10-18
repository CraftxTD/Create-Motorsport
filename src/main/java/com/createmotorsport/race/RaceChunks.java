package com.createmotorsport.race;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.Set;

public final class RaceChunks {

    private final Set<Long> forced = new LinkedHashSet<>();

    public boolean isEmpty() {
        return this.forced.isEmpty();
    }

    public void clear() {
        this.forced.clear();
    }

    public void force(ServerLevel level, BlockPos pos) {
        int cx = pos.getX() >> 4;
        int cz = pos.getZ() >> 4;
        level.setChunkForced(cx, cz, true);
        this.forced.add(ChunkPos.asLong(cx, cz));
    }

    public void release(ServerLevel level) {
        for (long packed : new ArrayList<>(this.forced)) {
            level.setChunkForced(ChunkPos.getX(packed), ChunkPos.getZ(packed), false);
        }
        this.forced.clear();
    }

    public void write(CompoundTag tag) {
        long[] out = new long[this.forced.size()];
        int i = 0;
        for (long packed : this.forced) {
            out[i++] = packed;
        }
        tag.putLongArray("ForcedChunks", out);
    }

    public void read(CompoundTag tag) {
        this.forced.clear();
        for (long packed : tag.getLongArray("ForcedChunks")) {
            this.forced.add(packed);
        }
    }
}
