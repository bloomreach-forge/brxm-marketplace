/*
 * Copyright 2025 Bloomreach
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.bloomreach.forge.marketplace.repository.service;

import org.bloomreach.forge.marketplace.common.model.Addon;
import org.bloomreach.forge.marketplace.common.model.AddonVersion;
import org.bloomreach.forge.marketplace.common.model.Category;
import org.bloomreach.forge.marketplace.common.model.Compatibility;
import org.bloomreach.forge.marketplace.common.model.PluginTier;
import org.bloomreach.forge.marketplace.common.model.Publisher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

public class AddonRegistryTest {

    private AddonRegistry registry;

    @BeforeEach
    void setUp() {
        registry = new AddonRegistry();
    }

    @Test
    void registerAddon_storesAddonById() {
        Addon addon = createAddon("ip-filter", Category.SECURITY);

        registry.register(addon);

        Optional<Addon> result = registry.findById("ip-filter");
        assertTrue(result.isPresent());
        assertEquals("ip-filter", result.get().getId());
    }

    @Test
    void registerAddon_withNullId_throwsException() {
        Addon addon = new Addon();

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> registry.register(addon));
        assertEquals("Addon id is required", exception.getMessage());
    }

    @Test
    void registerAddon_overwrites_existingAddon() {
        Addon first = createAddon("ip-filter", Category.SECURITY);
        first.setVersion("1.0.0");
        Addon second = createAddon("ip-filter", Category.SECURITY);
        second.setVersion("2.0.0");

        registry.register(first);
        registry.register(second);

        Optional<Addon> result = registry.findById("ip-filter");
        assertEquals("2.0.0", result.get().getVersion());
    }

    @Test
    void findById_returnsEmpty_whenNotFound() {
        Optional<Addon> result = registry.findById("nonexistent");

        assertFalse(result.isPresent());
    }

    @Test
    void findById_withNull_returnsEmpty() {
        Optional<Addon> result = registry.findById(null);

        assertFalse(result.isPresent());
    }

    @Test
    void findAll_returnsAllRegisteredAddons() {
        registry.register(createAddon("addon-1", Category.SECURITY));
        registry.register(createAddon("addon-2", Category.INTEGRATION));
        registry.register(createAddon("addon-3", Category.SEARCH));

        List<Addon> all = registry.findAll();

        assertEquals(3, all.size());
    }

    @Test
    void findAll_returnsEmptyList_whenNoAddons() {
        List<Addon> all = registry.findAll();

        assertTrue(all.isEmpty());
    }

    @Test
    void filter_byCategory_returnsMatchingAddons() {
        registry.register(createAddon("security-1", Category.SECURITY));
        registry.register(createAddon("security-2", Category.SECURITY));
        registry.register(createAddon("integration-1", Category.INTEGRATION));

        List<Addon> filtered = registry.filter(Category.SECURITY, null, null, null);

        assertEquals(2, filtered.size());
        assertTrue(filtered.stream().allMatch(a -> a.getCategory() == Category.SECURITY));
    }

    @Test
    void filter_byPublisherType_returnsMatchingAddons() {
        Addon bloomreachAddon = createAddon("br-addon", Category.SECURITY);
        bloomreachAddon.getPublisher().setType(Publisher.PublisherType.bloomreach);

        Addon communityAddon = createAddon("comm-addon", Category.SECURITY);
        communityAddon.getPublisher().setType(Publisher.PublisherType.community);

        registry.register(bloomreachAddon);
        registry.register(communityAddon);

        List<Addon> filtered = registry.filter(null, Publisher.PublisherType.bloomreach, null, null);

        assertEquals(1, filtered.size());
        assertEquals("br-addon", filtered.get(0).getId());
    }

    @Test
    void filter_byPluginTier_returnsMatchingAddons() {
        Addon forgeAddon = createAddon("forge-addon", Category.SECURITY);
        forgeAddon.setPluginTier(PluginTier.FORGE_ADDON);

        Addon servicePlugin = createAddon("service-plugin", Category.SECURITY);
        servicePlugin.setPluginTier(PluginTier.SERVICE_PLUGIN);

        registry.register(forgeAddon);
        registry.register(servicePlugin);

        List<Addon> filtered = registry.filter(null, null, PluginTier.FORGE_ADDON, null);

        assertEquals(1, filtered.size());
        assertEquals("forge-addon", filtered.get(0).getId());
    }

    @Test
    void filter_byBrxmVersion_returnsCompatibleAddons() {
        Addon compatible = createAddon("compatible", Category.SECURITY);
        Compatibility compat = new Compatibility();
        Compatibility.VersionRange brxm = new Compatibility.VersionRange();
        brxm.setMin("15.0.0");
        brxm.setMax("16.6.5");
        compat.setBrxm(brxm);
        compatible.setCompatibility(compat);

        Addon incompatible = createAddon("incompatible", Category.SECURITY);
        Compatibility incompat = new Compatibility();
        Compatibility.VersionRange brxm2 = new Compatibility.VersionRange();
        brxm2.setMin("14.0.0");
        brxm2.setMax("14.9.0");
        incompat.setBrxm(brxm2);
        incompatible.setCompatibility(incompat);

        registry.register(compatible);
        registry.register(incompatible);

        List<Addon> filtered = registry.filter(null, null, null, "15.5.0");

        assertEquals(1, filtered.size());
        assertEquals("compatible", filtered.get(0).getId());
    }

    @Test
    void filter_withMultipleCriteria_appliesAll() {
        Addon match = createAddon("match", Category.SECURITY);
        match.getPublisher().setType(Publisher.PublisherType.bloomreach);
        match.setPluginTier(PluginTier.FORGE_ADDON);

        Addon wrongCategory = createAddon("wrong-cat", Category.INTEGRATION);
        wrongCategory.getPublisher().setType(Publisher.PublisherType.bloomreach);
        wrongCategory.setPluginTier(PluginTier.FORGE_ADDON);

        Addon wrongPublisher = createAddon("wrong-pub", Category.SECURITY);
        wrongPublisher.getPublisher().setType(Publisher.PublisherType.community);
        wrongPublisher.setPluginTier(PluginTier.FORGE_ADDON);

        registry.register(match);
        registry.register(wrongCategory);
        registry.register(wrongPublisher);

        List<Addon> filtered = registry.filter(
                Category.SECURITY,
                Publisher.PublisherType.bloomreach,
                PluginTier.FORGE_ADDON,
                null
        );

        assertEquals(1, filtered.size());
        assertEquals("match", filtered.get(0).getId());
    }

    @Test
    void filter_withNoFilters_returnsAll() {
        registry.register(createAddon("addon-1", Category.SECURITY));
        registry.register(createAddon("addon-2", Category.INTEGRATION));

        List<Addon> filtered = registry.filter(null, null, null, null);

        assertEquals(2, filtered.size());
    }

    @Test
    void clear_removesAllAddons() {
        registry.register(createAddon("addon-1", Category.SECURITY));
        registry.register(createAddon("addon-2", Category.INTEGRATION));

        registry.clear();

        assertTrue(registry.findAll().isEmpty());
    }

    @Test
    void size_returnsAddonCount() {
        assertEquals(0, registry.size());

        registry.register(createAddon("addon-1", Category.SECURITY));
        assertEquals(1, registry.size());

        registry.register(createAddon("addon-2", Category.INTEGRATION));
        assertEquals(2, registry.size());
    }

    @Test
    void filter_bySource_returnsMatchingAddons() {
        Addon forgeAddon = createAddon("forge-addon", Category.SECURITY);
        forgeAddon.setSource("bloomreach-forge");

        Addon partnerAddon = createAddon("partner-addon", Category.SECURITY);
        partnerAddon.setSource("partner-acme");

        registry.register(forgeAddon);
        registry.register(partnerAddon);

        List<Addon> filtered = registry.filter(null, null, null, null, "bloomreach-forge");

        assertEquals(1, filtered.size());
        assertEquals("forge-addon", filtered.get(0).getId());
    }

    @Test
    void listSources_returnsDistinctSources() {
        Addon addon1 = createAddon("addon-1", Category.SECURITY);
        addon1.setSource("source-a");
        Addon addon2 = createAddon("addon-2", Category.SECURITY);
        addon2.setSource("source-b");
        Addon addon3 = createAddon("addon-3", Category.SECURITY);
        addon3.setSource("source-a");

        registry.register(addon1);
        registry.register(addon2);
        registry.register(addon3);

        List<String> sources = registry.listSources();

        assertEquals(2, sources.size());
        assertTrue(sources.contains("source-a"));
        assertTrue(sources.contains("source-b"));
    }

    @Test
    void clearBySource_removesOnlyMatchingAddons() {
        Addon forgeAddon = createAddon("forge-addon", Category.SECURITY);
        forgeAddon.setSource("bloomreach-forge");

        Addon partnerAddon = createAddon("partner-addon", Category.SECURITY);
        partnerAddon.setSource("partner-acme");

        registry.register(forgeAddon);
        registry.register(partnerAddon);

        registry.clearBySource("bloomreach-forge");

        assertFalse(registry.findById("forge-addon").isPresent());
        assertTrue(registry.findById("partner-addon").isPresent());
    }

    @Test
    void clearBySource_withNull_doesNothing() {
        registry.register(createAddon("addon-1", Category.SECURITY));

        registry.clearBySource(null);

        assertEquals(1, registry.size());
    }

    @Test
    void findById_withQualifiedId_returnsMatchingAddon() {
        Addon forgeAddon = createAddon("ip-filter", Category.SECURITY);
        forgeAddon.setSource("forge");
        Addon partnerAddon = createAddon("ip-filter", Category.SECURITY);
        partnerAddon.setSource("partner");
        partnerAddon.setId("ip-filter"); // same id, different source

        // Register both with qualified IDs to allow qualified lookups
        registry.registerWithQualifiedId(forgeAddon);
        registry.registerWithQualifiedId(partnerAddon);

        Optional<Addon> forgeResult = registry.findById("forge:ip-filter");
        Optional<Addon> partnerResult = registry.findById("partner:ip-filter");

        assertTrue(forgeResult.isPresent());
        assertEquals("forge", forgeResult.get().getSource());

        assertTrue(partnerResult.isPresent());
        assertEquals("partner", partnerResult.get().getSource());
    }

    @Test
    void findById_withUnqualifiedId_prefersForgeSource() {
        Addon forgeAddon = createAddon("ip-filter", Category.SECURITY);
        forgeAddon.setSource("forge");
        Addon partnerAddon = createAddon("ip-filter", Category.SECURITY);
        partnerAddon.setSource("partner");

        registry.registerWithQualifiedId(partnerAddon);
        registry.register(forgeAddon); // forge overwrites unqualified

        Optional<Addon> result = registry.findById("ip-filter");

        assertTrue(result.isPresent());
        assertEquals("forge", result.get().getSource());
    }

    @Test
    void registerWithQualifiedId_storesWithSourcePrefix() {
        Addon addon = createAddon("my-addon", Category.INTEGRATION);
        addon.setSource("partner");

        registry.registerWithQualifiedId(addon);

        assertTrue(registry.findById("partner:my-addon").isPresent());
        assertFalse(registry.findById("my-addon").isPresent());
    }

    // --- Epoch (versions[]) filtering tests ---

    @Test
    void filter_withEpochs_matchesOlderBrxmViaExplicitMax() {
        // epoch 4: min=15, max=16.6.5 (explicit); epoch 5: min=17
        Addon addon = createAddon("brut", Category.DEVELOPER_TOOLS);
        addon.setVersions(List.of(
                epoch("4.0.2", "15.0.0", "16.6.5", null),
                epoch("5.0.1", "17.0.0", null, null)
        ));

        registry.register(addon);

        List<Addon> result = registry.filter(null, null, null, "16.6.5");

        assertEquals(1, result.size(), "addon should match via epoch 4 explicit max");
    }

    @Test
    void filter_withEpochs_brxmAtEpochBoundaryExcludedByInferredMax() {
        // epoch 4: min=15, no explicit max, inferredMax=17.0.0; epoch 5: min=17
        // brxmVersion=17.0.0 must NOT match epoch 4 (17.0 >= inferredMax 17.0 is exclusive)
        // and MUST match epoch 5
        Addon addon = createAddon("brut", Category.DEVELOPER_TOOLS);
        AddonVersion v4 = epoch("4.0.2", "15.0.0", null, null);
        v4.setInferredMax("17.0.0");
        addon.setVersions(List.of(v4, epoch("5.0.1", "17.0.0", null, null)));

        registry.register(addon);

        List<Addon> result = registry.filter(null, null, null, "17.0.0");

        assertEquals(1, result.size(), "addon should match via epoch 5 when brxm=17.0.0");
    }

    @Test
    void filter_withEpochs_brxmBetweenEpochsBelowInferredMax() {
        // brxmVersion=16.7.0 < inferredMax=17.0.0 → epoch 4 matches
        Addon addon = createAddon("brut", Category.DEVELOPER_TOOLS);
        AddonVersion v4 = epoch("4.0.2", "15.0.0", null, null);
        v4.setInferredMax("17.0.0");
        addon.setVersions(List.of(v4, epoch("5.0.1", "17.0.0", null, null)));

        registry.register(addon);

        List<Addon> result = registry.filter(null, null, null, "16.7.0");

        assertEquals(1, result.size(), "16.7.0 is below inferredMax so epoch 4 matches");
    }

    @Test
    void filter_withEpochs_newerBrxmMatchesLatestEpoch() {
        // brxmVersion=20.0.0: epoch 4 excluded by inferredMax, epoch 5 (no max) matches
        Addon addon = createAddon("brut", Category.DEVELOPER_TOOLS);
        AddonVersion v4 = epoch("4.0.2", "15.0.0", null, null);
        v4.setInferredMax("17.0.0");
        addon.setVersions(List.of(v4, epoch("5.0.1", "17.0.0", null, null)));

        registry.register(addon);

        List<Addon> result = registry.filter(null, null, null, "20.0.0");

        assertEquals(1, result.size(), "20.0.0 should match epoch 5 which has no ceiling");
    }

    @Test
    void filter_withEpochs_veryOldBrxmMatchesNoEpoch() {
        // brxmVersion=10.0.0 is below both epochs' min → excluded
        Addon addon = createAddon("brut", Category.DEVELOPER_TOOLS);
        AddonVersion v4 = epoch("4.0.2", "15.0.0", null, null);
        v4.setInferredMax("17.0.0");
        addon.setVersions(List.of(v4, epoch("5.0.1", "17.0.0", null, null)));

        registry.register(addon);

        List<Addon> result = registry.filter(null, null, null, "10.0.0");

        assertTrue(result.isEmpty(), "10.0.0 is below all epochs' min");
    }

    @Test
    void filter_withEpochs_nullBrxmVersionReturnsAddon() {
        Addon addon = createAddon("brut", Category.DEVELOPER_TOOLS);
        addon.setVersions(List.of(epoch("5.0.1", "17.0.0", null, null)));
        registry.register(addon);

        List<Addon> result = registry.filter(null, null, null, null);

        assertEquals(1, result.size());
    }

    private AddonVersion epoch(String version, String brxmMin, String brxmMax, String inferredMax) {
        Compatibility.VersionRange range = new Compatibility.VersionRange();
        range.setMin(brxmMin);
        range.setMax(brxmMax);
        Compatibility compat = new Compatibility();
        compat.setBrxm(range);

        AddonVersion av = new AddonVersion();
        av.setVersion(version);
        av.setCompatibility(compat);
        av.setInferredMax(inferredMax);
        return av;
    }

    private Addon createAddon(String id, Category category) {
        Addon addon = new Addon();
        addon.setId(id);
        addon.setName(id);
        addon.setCategory(category);

        Publisher publisher = new Publisher();
        publisher.setName("Test Publisher");
        publisher.setType(Publisher.PublisherType.community);
        addon.setPublisher(publisher);

        return addon;
    }
}
