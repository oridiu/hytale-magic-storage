package io.github.oridiu.magicstorage.system;

import io.github.oridiu.magicstorage.storage.ChestKey;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

class StorageRulesTest {

    @Test
    void fileNameDistinguishesArenaSpaceFromUnderscore() {
        ChestKey spaced = new ChestKey("Arena 1", 1, 2, 3);
        ChestKey underscored = new ChestKey("Arena_1", 1, 2, 3);
        assertNotEquals(spaced.fileName(), underscored.fileName());
        assertEquals(spaced.fileName(), new ChestKey("arena 1", 1, 2, 3).fileName());
        assertEquals(new ChestKey("Arena", 1, 2, 3).fileName(), new ChestKey("arena", 1, 2, 3).fileName());
    }

    @Test
    void keepsLinkedBagOverEarlierUnlinkedOne() {
        assertEquals(1, OneBagSystems.keptBag(new boolean[] {false, true}));
    }
}
