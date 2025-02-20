package com.petrolpark.destroy.chemistry.legacy;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.Predicate;
import java.util.function.Supplier;

import javax.annotation.Nullable;

import com.petrolpark.destroy.Destroy;
import com.petrolpark.destroy.chemistry.api.util.Constants;
import com.petrolpark.destroy.chemistry.legacy.index.DestroyMolecules;
import com.petrolpark.destroy.util.DestroyLang;
import com.simibubi.create.foundation.utility.NBTHelper;

import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;

/**
 * A {@link com.petrolpark.destroy.chemistry.legacy.LegacyMixture Mixture} which cannot react.
 * Instantiating - or adding a {@link LegacySpecies} to - a Read-Only Mixture skips out on the processing-intensive generation of {@link LegacyReaction Reactions},
 * making them useful for GUI displays.
 */
public class ReadOnlyMixture {

    /**
     * A Decimal Formatter used for displaying the contents of Mixtures.
     */
    private static DecimalFormat df = new DecimalFormat();
    static {
        df.setMinimumFractionDigits(1);
        df.setMinimumFractionDigits(1);
    };

    /**
     * The minimum value below which a {@link LegacySpecies} is considered an impurity and not a significant part of the Mixture.
     */
    public static final float IMPURITY_THRESHOLD = 0.1f;

    /**
     * The display name of this Mixture. This may be a custom name (if this Mixture comes from a Recipe), or {@link ReadOnlyMixture#getName generated}
     * if this Mixture has been {@link LegacyMixture#react reacted}.
     * <p>It is recommemded that Mixtures obtained from Recipes are given custom names using the tag {@code TranslationKey} to avoid having to calculate a name.</p>
     */
    protected Component name;

    /**
     * The full translation key of this Mixture if it has a custom name, for example {@code destroy.mixture.brine}.
     */
    protected String translationKey;

    /**
     * The RGBA color of this Mixture.
     */
    protected Integer color;
    
    /**
     * How hot (in kelvins) this Mixture is. Temperature affects the rate of {@link LegacyReaction Reactions}.
     */
    protected float temperature;

    /**
     * The {@link LegacySpecies Molecules} contained by this Mixture, mapped to their concentrations (in moles per Bucket).
     */
    protected Map<LegacySpecies, Float> contents;

    /**
     * The {@link LegacySpecies Molecules} in this Mixture, mapped to the proportion of which are gaseous. For example, {@code 0}
     * means this Molecule is entirely liquid or aqueous, {@code 0.5} means they are half liquid and half gaseous, and {@code 1}
     * means the Molecule is entirely gaseous in this Mixture.
     */
    protected Map<LegacySpecies, Float> states;

    /**
     * Whether any {@link LegacySpecies Molecules} are currently boiling or condensing (their {@link ReadOnlyMixture#states state} is not a whole number)
     */
    protected boolean boiling;

    public ReadOnlyMixture() {
        this(298f);
    };

    public ReadOnlyMixture(float temperature) {
        translationKey = "";
        contents = new HashMap<>();
        if (temperature < 0f) throw new IllegalStateException("Mixtures cannot be below 0K");
        this.temperature = temperature;
        states = new HashMap<>();
        boiling = false;
    };

    /**
     * Converts this Mixture into a storeable String that can be {@link ReadOnlyMixture#readNBT parsed back} into a Mixture.
     */
    public CompoundTag writeNBT() {
        CompoundTag compound = new CompoundTag();
        if (translationKey != null && !translationKey.isEmpty()) {
            compound.putString("TranslationKey", translationKey);
        };
        compound.putFloat("Temperature", temperature);
        compound.put("Contents", NBTHelper.writeCompoundList(contents.entrySet().stream().filter(e -> e.getValue() > 0f).toList(), (entry) -> {
            CompoundTag moleculeTag = new CompoundTag();
            moleculeTag.putString("Molecule", entry.getKey().getFullID());
            moleculeTag.putFloat("Concentration", entry.getValue());
            float gaseous = states.get(entry.getKey());
            if (gaseous != 1f && gaseous != 0f) moleculeTag.putFloat("Gaseous", states.get(entry.getKey())); // Only put the state if its not obvious from the temperature
            return moleculeTag;
        }));
        return compound;
    };

    /**
     * Generates a Read-Only Mixture from the given Compound Tag.
     * @param compound
     * @return A new Read-Only Mixture instance
     */
    public static <T extends ReadOnlyMixture> T readNBT(Supplier<T> newMixture, CompoundTag compound) {
        T mixture = newMixture.get();
        if (compound == null) {
            Destroy.LOGGER.warn("Null Mixture read");
            return mixture;  
        };
        mixture.translationKey = compound.getString("TranslationKey"); // Set to "" if the key is not present
        if (compound.contains("Temperature")) mixture.temperature = compound.getFloat("Temperature");
        ListTag contents = compound.getList("Contents", 10);
        contents.forEach(tag -> {
            CompoundTag moleculeTag = (CompoundTag) tag;
            LegacySpecies molecule = LegacySpecies.getMolecule(moleculeTag.getString("Molecule"));
            mixture.addMolecule(molecule, moleculeTag.getFloat("Concentration"));
            float state = moleculeTag.getFloat("Gaseous");
            if (state != 0f && state != 1f) mixture.boiling = true;
            mixture.states.put(molecule, state);
        });
        mixture.updateName();
        mixture.updateColor();
        return mixture;
    };

    /**
     * The display name of this Mixture. This may be a custom name (if this Mixture comes from a Recipe), or {@link ReadOnlyMixture#getName generated}
     * if this Mixture has been {@link LegacyMixture#react reacted}.
     * <p>It is recommemded that Mixtures obtained from Recipes are given custom names using the tag {@code TranslationKey} to avoid having to calculate a name.</p>
     */
    public Component getName() {
        if (name == null) updateName();
        return name;
    };

    /**
     * The {@link ReadOnlyMixture#color color} of this Mixture.
     */
    public int getColor() {
        if (color == null) updateColor();
        return color;
    };

    /**
     * Sets the display name of this Mixture to avoid having to calculate what the name should be based on the {@link ReadOnlyMixture#contents contents}. 
     * @param translationKey The full translation key of this Mixture, for example {@code destroy.mixture.brine}.
     */
    public void setTranslationKey(String translationKey) {
        this.translationKey = translationKey;
    };

    /**
     * How hot (in kelvins) this Mixture is. Temperature affects the rate of {@link LegacyReaction Reactions}.
     */
    public float getTemperature() {
        return temperature;
    };

    /**
     * Whether any {@link LegacySpecies Molecules} are currently boiling or condensing (their {@link ReadOnlyMixture#states state} is not a whole number)
     */
    public boolean isBoiling() {
        return boiling;
    };

    /**
     * Whether this Mixture has no {@link LegacySpecies Molecules} in it.
     */
    public boolean isEmpty() {
        return contents.isEmpty();
    };

    /**
     * The concentration of the given {@link LegacySpecies} in this Mixture.
     * @param molecule
     * @return 0 if the Mixture does not contain the given Molecule
     */
    public float getConcentrationOf(LegacySpecies molecule) {
        if (contents.containsKey(molecule)) {
            return contents.get(molecule);
        } else {
            return 0f;
        }
    };

    /**
     * Get the combined concentration of every Molecule in this Mixture.
     * @return in moles per bucket
     */
    public float getTotalConcentration() {
        float total = 0f;
        for (Float concentration : contents.values()) {
            total += concentration;
        };
        return total;
    };

    /**
     * Checks that this Mixture contains a suitable concentration of the given {@link LegacySpecies}, and that all other substances present are solvents or low-concentration impurities.
     * This is used in Recipes.
     * @param molecule Only known (non-novel) Molecules (i.e. those with a name space) will be detected
     * @param concentration
     * @param ignoreableMolecules Molecules other than solvents and low-concentration impurities that should be ignored should return {@code true}. The predicate can be {@code null} if there are no other Molecules that can be ignored
     */
    public boolean hasUsableMolecule(LegacySpecies molecule, float minConcentration, float maxConcentration, @Nullable Predicate<LegacySpecies> ignore) {
        return hasUsableMolecules(molecule::equals, minConcentration, maxConcentration, ignore);
    };

    /**
     * Checks that this Mixture contains a suitable concentration of {@link LegacySpecies Molecules} that pass a given predicate, and that all other substances present are solvents
     * or low-concentration impurities. This is used in Recipes.
     * @param molecules The check for Molecules that should contribute to the total concentration
     * @param concentration
     * @param ignore Molecules other than solvents and low-concentration impurities that should be ignored should return {@code true}. The predicate can be {@code null} if there are no other Molecules that can be ignored
     */
    public boolean hasUsableMolecules(Predicate<LegacySpecies> molecules, float minConcentration, float maxConcentration, @Nullable Predicate<LegacySpecies> ignore) {
        if (ignore == null) ignore = (m) -> false;
        float combinedConcentration = 0f;
        for (Entry<LegacySpecies, Float> entry : contents.entrySet()) {
            if (ignore.test(entry.getKey())) continue; // If this Molecule is specified as ignoreable, ignore it.
            if (molecules.test(entry.getKey())) {
                combinedConcentration += entry.getValue(); // If this passes the test, add it to the total
                continue; // Then move on
            };
            if (entry.getKey().hasTag(DestroyMolecules.Tags.SOLVENT)) continue; // If this is a solvent, ignore it
            if (entry.getValue() > IMPURITY_THRESHOLD) return false; // If this illegal impurity is in high-enough concentration, this Mixture is unsuitable
        };
        return (combinedConcentration < maxConcentration + 0.05f && combinedConcentration > minConcentration - 0.05f); // The  0.05 accounts for rounding errors
    };

    /**
     * Adds the given {@link LegacySpecies} to this Read-Only Mixture with the given concentration.
     * If the Read-Only Mixture already contained the Molecule its concentration will be replaced,
     * thought this should not happen - to modify the contents of existing Mixtures, use a {@link LegacyMixture} and not a Read-Only Mixture.
     * @param molecule Will not be added if hypothetical (for example an {@link LegacyFunctionalGroup#getExampleMolecule example Molecule} of a {@link LegacyFunctionalGroup functional Group})
     * @param concentration
     * @return This Mixture
     */
    public ReadOnlyMixture addMolecule(LegacySpecies molecule, float concentration) {

        if (molecule == null || concentration == 0f) {
            return this;
        };
        if (molecule.isHypothetical()) {
            Destroy.LOGGER.warn("Could not add hypothetical Molecule '"+molecule.getFullID()+"'' to a real Mixture.");
            return this;
        };

        contents.put(molecule, concentration);
        states.put(molecule, molecule.getBoilingPoint() < temperature ? 1f : 0f);

        return this;
    };

    /**
     * Get all the {@link LegacySpecies Molecules} present in this Mixture.
     * @param excludeNovel Whether to exclude novel Molecules
     */
    public List<LegacySpecies> getContents(boolean excludeNovel) {
        return contents.keySet().stream().filter(molecule -> !molecule.isNovel() || !excludeNovel).toList();
    };

    /**
     * Get the list of {@link LegacySpecies Molecules} in this Mixture as a String.
     * (Used for debugging).
     */
    public String getContentsString() {
        String string = "";
        if (contents.isEmpty()) return string;
        for (Entry<LegacySpecies, Float> entry : contents.entrySet()) {
            string += entry.getKey().getFullID() + " (" + entry.getValue() + "M), ";
        };
        return string.substring(0, string.length() - 2);
    };

    /**
     * The tooltip listing the {@link ReadOnlyMixture#contents contents} of this Mixture.
     * @param iupac Whether to use IUPAC names instead of common names
     * @param monospace Whether to add extra whitespace so all Molecule names line up
     * @param useMoles Whether to use moles instead of molar for the units
     * @param amount The size of the Fluid Stack. If {@code useMoles} is {@code false}, this is ignored
     * @param concentrationFormatter The formatter that determines the number of decimals places for concentration to display
     */
    public List<Component> getContentsTooltip(boolean iupac, boolean monospace, boolean useMoles, int amount, DecimalFormat concentrationFormatter) {
        int i = 0;
        List<Component> tooltip = new ArrayList<>();
        List<LegacySpecies> molecules = new ArrayList<>(contents.keySet());
        Collections.sort(molecules, (m1, m2) -> contents.get(m2).compareTo(contents.get(m1)));
        
        int quantityLabelLength = DestroyLang.quantity(0f, useMoles, concentrationFormatter).string().length() + 2;
        for (LegacySpecies molecule : molecules) {
            float quantity = contents.get(molecule) * (useMoles ? amount / Constants.MILLIBUCKETS_PER_LITER: 1);
            tooltip.add(i, DestroyLang.builder()
                .space().space()
                .add(Component.literal(monospace ? String.format("%1$"+quantityLabelLength+"s", DestroyLang.quantity(quantity, useMoles, concentrationFormatter).string()) : DestroyLang.quantity(quantity, useMoles, concentrationFormatter).string())) // Show concentration
                .space()
                .add(molecule.getName(iupac).plainCopy())
                .add(Component.literal(
                    (molecule.getCharge() == 0 ? "" : " [" + molecule.getSerializedCharge(false) + "]") // Show charge, if there is one
                ))
                .style(ChatFormatting.GRAY)
                .component()
            );
            i++;
        };
        return tooltip;
    };

    protected void updateColor() {
        color = 0x20FFFFFF;
    };

    /**
     * Update the {@link ReadOnlyMixture#name name} of this Mixture to reflect what's in it.
     */
    protected void updateName() {
        name = DestroyLang.translate("mixture.mixture").component();
    };
};
