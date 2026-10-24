package io.github.oridiu.magicstorage.system;

import com.hypixel.hytale.component.Archetype;
import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.EntityEventSystem;
import com.hypixel.hytale.server.core.universe.world.events.ecs.ChunkSaveEvent;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
import io.github.oridiu.magicstorage.MagicStoragePlugin;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Saves changed Magic Chests whenever the world saves its chunks, so they are
 * written to disk together with everything else.
 */
public class ChestSaveSystem extends EntityEventSystem<ChunkStore, ChunkSaveEvent> {

    public ChestSaveSystem() {
        super(ChunkSaveEvent.class);
    }

    @Override
    public void handle(
            int index,
            @Nonnull ArchetypeChunk<ChunkStore> archetypeChunk,
            @Nonnull Store<ChunkStore> store,
            @Nonnull CommandBuffer<ChunkStore> commandBuffer,
            @Nonnull ChunkSaveEvent event
    ) {
        MagicStoragePlugin.getInstance().getStorage().saveDirty();
    }

    @Nullable
    @Override
    public Query<ChunkStore> getQuery() {
        return Archetype.empty();
    }
}
