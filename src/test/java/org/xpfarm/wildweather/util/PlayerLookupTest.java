/*
 * WildWeatherUpdate - dynamic weather events for Minecraft.
 * Copyright (C) 2026 Carmelo Santana
 *
 * This program is free software: you can redistribute it and/or modify it under the
 * terms of the GNU Affero General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later version.
 * See the LICENSE file at the project root for the full license text.
 */
package org.xpfarm.wildweather.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Exercises {@link PlayerLookup}'s pure functions -- the candidate-name list and the
 * failure message -- with no Bukkit types and no running server.
 *
 * <p>{@link PlayerLookup#resolve} and {@link PlayerLookup#resolveAllowingPartial} are
 * deliberately not covered: both call into {@code Bukkit}, whose static server handle
 * cannot be constructed headlessly. The decisions worth pinning were extracted into
 * static methods over strings precisely so they could be tested here, leaving only
 * untestable Bukkit glue behind.
 */
class PlayerLookupTest {

    /**
     * Resolving a typed target name.
     *
     * <p>A Bedrock account joins through Floodgate under a {@code .}-prefixed Java-side
     * username, so an operator typing the bare name gets "not found" for a player
     * standing in front of them. These cases pin the candidate list and the failure
     * message that replaced it.
     */
    @Nested
    @DisplayName("target resolution")
    class TargetResolution {

        @Test
        @DisplayName("a bare name also tries the Floodgate '.' prefix")
        void bareNameTriesFloodgatePrefix() {
            assertEquals(List.of("carm", ".carm"), PlayerLookup.targetNameCandidates("carm"));
        }

        @Test
        @DisplayName("an already-prefixed name is not prefixed twice")
        void prefixedNameIsNotDoubled() {
            assertEquals(List.of(".acarm"), PlayerLookup.targetNameCandidates(".acarm"));
        }

        @Test
        @DisplayName("surrounding whitespace is trimmed")
        void whitespaceIsTrimmed() {
            assertEquals(List.of("carm", ".carm"), PlayerLookup.targetNameCandidates("  carm  "));
        }

        @Test
        @DisplayName("null and blank yield no candidates")
        void nullAndBlankYieldNothing() {
            assertTrue(PlayerLookup.targetNameCandidates(null).isEmpty());
            assertTrue(PlayerLookup.targetNameCandidates("   ").isEmpty());
        }

        @Test
        @DisplayName("the failure message lists who is actually online")
        void failureMessageListsOnlinePlayers() {
            String message = PlayerLookup.noSuchPlayerMessage("carm", List.of(".acarm", "Steve"));
            assertTrue(message.contains("carm"), message);
            assertTrue(message.contains(".acarm"), message);
            assertTrue(message.contains("Steve"), message);
        }

        @Test
        @DisplayName("the failure message says so plainly when nobody is online")
        void failureMessageWhenNobodyOnline() {
            String message = PlayerLookup.noSuchPlayerMessage("carm", List.of());
            assertTrue(message.contains("carm"), message);
            assertTrue(message.toLowerCase(Locale.ROOT).contains("no players"), message);
        }
    }
}
