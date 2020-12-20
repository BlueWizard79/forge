/*
 * Forge: Play Magic: the Gathering.
 * Copyright (C) 2011  Forge Team
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package forge.game.ability.effects;

import forge.GameCommand;
import forge.card.CardType;
import forge.card.mana.ManaCost;
import forge.card.mana.ManaCostParser;
import forge.game.Game;
import forge.game.ability.AbilityFactory;
import forge.game.ability.AbilityUtils;
import forge.game.ability.SpellAbilityEffect;
import forge.game.card.Card;
import forge.game.event.GameEventCardStatsChanged;
import forge.game.keyword.Keyword;
import forge.game.phase.PhaseType;
import forge.game.replacement.ReplacementEffect;
import forge.game.replacement.ReplacementHandler;
import forge.game.spellability.AbilityStatic;
import forge.game.spellability.SpellAbility;
import forge.game.staticability.StaticAbility;
import forge.game.trigger.Trigger;
import forge.game.trigger.TriggerHandler;

import java.util.List;

import com.google.common.collect.Lists;

public abstract class AnimateEffectBase extends SpellAbilityEffect {
    public static void doAnimate(final Card c, final SpellAbility sa, final Integer power, final Integer toughness,
            final CardType addType, final CardType removeType, final String colors,
            final List<String> keywords, final List<String> removeKeywords, final List<String> hiddenKeywords,
            List<String> abilities, final List<String> triggers, final List<String> replacements, final List<String> stAbs,
            final long timestamp) {
        final Card source = sa.getHostCard();
        final Game game = source.getGame();

        boolean removeSuperTypes = false;
        boolean removeCardTypes = false;
        boolean removeSubTypes = false;
        boolean removeLandTypes = false;
        boolean removeCreatureTypes = false;
        boolean removeArtifactTypes = false;
        boolean removeEnchantmentTypes = false;

        boolean removeAll = sa.hasParam("RemoveAllAbilities");

        if (sa.hasParam("OverwriteTypes")) {
            removeSuperTypes = true;
            removeCardTypes = true;
            removeSubTypes = true;
            removeLandTypes = true;
            removeCreatureTypes = true;
            removeArtifactTypes = true;
            removeEnchantmentTypes = true;
        }

        if (sa.hasParam("KeepSupertypes")) {
            removeSuperTypes = false;
        }

        if (sa.hasParam("KeepCardTypes")) {
            removeCardTypes = false;
        }

        if (sa.hasParam("KeepSubtypes")) {
            removeSubTypes = false;
            removeLandTypes = false;
            removeCreatureTypes = false;
            removeArtifactTypes = false;
            removeEnchantmentTypes = false;
        }

        if (sa.hasParam("RemoveSuperTypes")) {
            removeSuperTypes = true;
        }

        if (sa.hasParam("RemoveCardTypes")) {
            removeCardTypes = true;
        }

        if (sa.hasParam("RemoveSubTypes")) {
            removeSubTypes = true;
        }

        if (sa.hasParam("RemoveLandTypes")) {
            removeLandTypes = true;
        }
        if (sa.hasParam("RemoveCreatureTypes")) {
            removeCreatureTypes = true;
        }
        if (sa.hasParam("RemoveArtifactTypes")) {
            removeArtifactTypes = true;
        }
        if (sa.hasParam("RemoveEnchantmentTypes")) {
            removeEnchantmentTypes = true;
        }

        if ((power != null) || (toughness != null)) {
            c.addNewPT(power, toughness, timestamp);
        }

        if (!addType.isEmpty() || !removeType.isEmpty() || removeCreatureTypes) {
            c.addChangedCardTypes(addType, removeType, removeSuperTypes, removeCardTypes, removeSubTypes,
                    removeLandTypes, removeCreatureTypes, removeArtifactTypes, removeEnchantmentTypes, timestamp);
        }

        c.addChangedCardKeywords(keywords, removeKeywords, removeAll, removeLandTypes, timestamp);

        if (sa.hasParam("CantHaveKeyword")) {
            c.addCantHaveKeyword(timestamp, Keyword.setValueOf(sa.getParam("CantHaveKeyword")));
        }

        for (final String k : hiddenKeywords) {
            c.addHiddenExtrinsicKeyword(k);
        }

        c.addColor(colors, !sa.hasParam("OverwriteColors"), timestamp);

        if (sa.hasParam("LeaveBattlefield")) {
            addLeaveBattlefieldReplacement(c, sa, sa.getParam("LeaveBattlefield"));
        }

        // remove abilities
        final List<SpellAbility> removedAbilities = Lists.newArrayList();
        boolean clearSpells = sa.hasParam("OverwriteSpells");

        if (clearSpells) {
            removedAbilities.addAll(Lists.newArrayList(c.getSpells()));
        }

        if (sa.hasParam("RemoveThisAbility") && !removedAbilities.contains(sa)) {
            removedAbilities.add(sa);
        }

        // give abilities
        final List<SpellAbility> addedAbilities = Lists.newArrayList();
        for (final String s : abilities) {
            SpellAbility sSA = AbilityFactory.getAbility(c, s, sa);
            sSA.setOriginalHost(source);
            addedAbilities.add(sSA);
        }

        // Grant triggers
        final List<Trigger> addedTriggers = Lists.newArrayList();
        for (final String s : triggers) {
            final Trigger parsedTrigger = TriggerHandler.parseTrigger(AbilityUtils.getSVar(sa, s), c, false);
            parsedTrigger.setOverridingAbility(AbilityFactory.getAbility(c, parsedTrigger.getParam("Execute"), sa));
            parsedTrigger.setOriginalHost(source);
            addedTriggers.add(parsedTrigger);
        }

        // give replacement effects
        final List<ReplacementEffect> addedReplacements = Lists.newArrayList();
        for (final String s : replacements) {
            addedReplacements.add(ReplacementHandler.parseReplacement(AbilityUtils.getSVar(sa, s), c, false));
        }

        // give static abilities (should only be used by cards to give
        // itself a static ability)
        final List<StaticAbility> addedStaticAbilities = Lists.newArrayList();
        for (final String s : stAbs) {
            addedStaticAbilities.add(new StaticAbility(AbilityUtils.getSVar(sa, s), c));
        }

        final GameCommand unanimate = new GameCommand() {
            private static final long serialVersionUID = -5861759814760561373L;

            @Override
            public void run() {
                doUnanimate(c, sa, hiddenKeywords, timestamp);

                c.removeChangedName(timestamp);
                c.updateStateForView();

                game.fireEvent(new GameEventCardStatsChanged(c));
            }
        };


        if (sa.hasParam("RevertCost")) {
            final ManaCost cost = new ManaCost(new ManaCostParser(sa.getParam("RevertCost")));
            final String desc = sa.getStackDescription();
            final SpellAbility revertSA = new AbilityStatic(c, cost) {
                @Override
                public void resolve() {
                    unanimate.run();
                }
                @Override
                public String getDescription() {
                    return cost + ": End Effect: " + desc;
                }
            };
            addedAbilities.add(revertSA);
        }

        // after unanimate to add RevertCost
        if (removeAll || removeLandTypes
                || !addedAbilities.isEmpty() || !removedAbilities.isEmpty() || !addedTriggers.isEmpty()
                || !addedReplacements.isEmpty() || !addedStaticAbilities.isEmpty()) {
            c.addChangedCardTraits(addedAbilities, removedAbilities, addedTriggers, addedReplacements,
                    addedStaticAbilities, removeAll, false, removeLandTypes, timestamp);
        }

        if (!sa.hasParam("Permanent")) {
            if (sa.hasParam("UntilEndOfCombat")) {
                game.getEndOfCombat().addUntil(unanimate);
            } else if (sa.hasParam("UntilHostLeavesPlay")) {
                source.addLeavesPlayCommand(unanimate);
            } else if (sa.hasParam("UntilLoseControlOfHost")) {
                sa.getHostCard().addLeavesPlayCommand(unanimate);
                sa.getHostCard().addChangeControllerCommand(unanimate);
            } else if (sa.hasParam("UntilYourNextUpkeep")) {
                game.getUpkeep().addUntil(source.getController(), unanimate);
            } else if (sa.hasParam("UntilTheEndOfYourNextUpkeep")) {
                if (game.getPhaseHandler().is(PhaseType.UPKEEP)) {
                    game.getUpkeep().registerUntilEnd(source.getController(), unanimate);
                } else {
                    game.getUpkeep().addUntilEnd(source.getController(), unanimate);
                }
            } else if (sa.hasParam("UntilControllerNextUntap")) {
                game.getUntap().addUntil(c.getController(), unanimate);
            } else if (sa.hasParam("UntilAPlayerCastSpell")) {
                game.getStack().addCastCommand(sa.getParam("UntilAPlayerCastSpell"), unanimate);
            } else if (sa.hasParam("UntilYourNextTurn")) {
                game.getCleanup().addUntil(source.getController(), unanimate);
            } else {
                game.getEndOfTurn().addUntil(unanimate);
            }
        }
    }

    /**
     * <p>
     * doUnanimate.
     * </p>
     *
     * @param c
     *            a {@link forge.game.card.Card} object.
     *            a {@link java.util.ArrayList} object.
     * @param colorDesc
     *            a {@link java.lang.String} object.
     * @param addedAbilities
     *            a {@link java.util.ArrayList} object.
     * @param addedTriggers
     *            a {@link java.util.ArrayList} object.
     * @param timestamp
     *            a long.
     */
    static void doUnanimate(final Card c, SpellAbility sa,
            final List<String> hiddenKeywords, final long timestamp) {

        c.removeNewPT(timestamp);

        c.removeChangedCardKeywords(timestamp);

        c.removeChangedCardTypes(timestamp);
        c.removeColor(timestamp);

        c.removeChangedCardTraits(timestamp);

        c.removeCantHaveKeyword(timestamp);

        for (final String k : hiddenKeywords) {
            c.removeHiddenExtrinsicKeyword(k);
        }

        // any other unanimate cleanup
        if (!c.isCreature()) {
            c.unEquipAllCards();
        }
    }

}
