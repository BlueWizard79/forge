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
package forge.game.card;

import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.common.collect.Table;

import forge.ImageKeys;
import forge.card.CardStateName;
import forge.card.CardType;
import forge.card.ColorSet;
import forge.card.MagicColor;
import forge.game.Game;
import forge.game.GameEntity;
import forge.game.GameObject;
import forge.game.ability.AbilityKey;
import forge.game.ability.AbilityUtils;
import forge.game.ability.ApiType;
import forge.game.player.Player;
import forge.game.spellability.OptionalCost;
import forge.game.spellability.SpellAbility;
import forge.game.spellability.TargetRestrictions;
import forge.game.zone.ZoneType;
import forge.util.TextUtil;
import forge.util.collect.FCollection;
import io.sentry.Sentry;
import io.sentry.event.BreadcrumbBuilder;

public final class CardUtil {
    // disable instantiation
    private CardUtil() { }

    public static final List<String> NON_STACKING_LIST = Lists.newArrayList();

    /** List of all keywords that could be modified by text changes.
     *  Mostly this is caused by them having a variable, like a cost.
     */
    public static final ImmutableList<String> modifiableKeywords = ImmutableList.<String>builder().add(
            "Enchant", "Protection", "Cumulative upkeep", "Equip", "Buyback",
            "Cycling", "Echo", "Kicker", "Flashback", "Madness", "Morph",
            "Affinity", "Entwine", "Splice", "Ninjutsu", "Presence",
            "Transmute", "Replicate", "Recover", "Suspend", "Aura swap",
            "Fortify", "Transfigure", "Champion", "Evoke", "Prowl", "IfReach",
            "Reinforce", "Unearth", "Level up", "Miracle", "Overload",
            "Scavenge", "Encore", "Bestow", "Outlast", "Dash", "Surge", "Emerge", "Hexproof:",
            "etbCounter", "Reflect").build();
    /** List of keyword endings of keywords that could be modified by text changes. */
    public static final ImmutableList<String> modifiableKeywordEndings = ImmutableList.<String>builder().add(
            "walk", "cycling", "offering").build();

    public static final boolean isKeywordModifiable(final String kw) {
        for (final String modKw : modifiableKeywords) {
            if (kw.startsWith(modKw)) {
                return true;
            }
        }
        for (final String end : modifiableKeywordEndings) {
            if (kw.endsWith(end)) {
                return true;
            }
        }
        return false;
    }

    public static ColorSet getColors(final Card c) {
        return c.determineColor();
    }

    public static boolean isStackingKeyword(final String keyword) {
        String kw = keyword;
        if (kw.startsWith("HIDDEN")) {
            kw = kw.substring(7);
        }

        return !kw.startsWith("Protection") && !kw.startsWith("CantBeBlockedBy")
                && !NON_STACKING_LIST.contains(kw);
    }

    public static String getShortColorsString(final Iterable<String> colors) {
        StringBuilder colorDesc = new StringBuilder();
        for (final String col : colors) {
            colorDesc.append(MagicColor.toShortString(col)).append(" ");
        }
        return colorDesc.toString();
    }

    /**
     * getThisTurnEntered.
     *
     * @param to    zone going to
     * @param from  zone coming from
     * @param valid a isValid expression
     * @param src   a Card object
     * @return a CardCollection that matches the given criteria
     */
    public static List<Card> getThisTurnEntered(final ZoneType to, final ZoneType from, final String valid, final Card src) {
        List<Card> res = Lists.newArrayList();
        final Game game = src.getGame();
        if (to != ZoneType.Stack) {
            for (Player p : game.getPlayers()) {
                res.addAll(p.getZone(to).getCardsAddedThisTurn(from));
            }
        }
        else {
            res.addAll(game.getStackZone().getCardsAddedThisTurn(from));
        }
        res = CardLists.getValidCardsAsList(res, valid, src.getController(), src, null);
        return res;
    }

    /**
     * getLastTurnEntered.
     *
     * @param to    zone going to
     * @param from  zone coming from
     * @param valid a isValid expression
     * @param src   a Card object
     * @return a CardCollection that matches the given criteria
     */
    public static List<Card> getLastTurnEntered(final ZoneType to, final ZoneType from, final String valid, final Card src) {
        List<Card> res = Lists.newArrayList();
        final Game game = src.getGame();
        if (to != ZoneType.Stack) {
            for (Player p : game.getPlayers()) {
                res.addAll(p.getZone(to).getCardsAddedLastTurn(from));
            }
        }
        else {
            res.addAll(game.getStackZone().getCardsAddedLastTurn(from));
        }
        res = CardLists.getValidCardsAsList(res, valid, src.getController(), src, null);
        return res;
    }

    public static List<Card> getThisTurnCast(final String valid, final Card src) {
        return CardLists.getValidCardsAsList(src.getGame().getStack().getSpellsCastThisTurn(), valid, src.getController(), src, null);
    }

    public static List<Card> getLastTurnCast(final String valid, final Card src) {
        return CardLists.getValidCardsAsList(src.getGame().getStack().getSpellsCastLastTurn(), valid, src.getController(), src, null);

    }

    public static List<Card> getLKICopyList(final Iterable<Card> in, Map<Integer, Card> cachedMap) {
        if (in == null) {
            return null;
        }
        List<Card> result = Lists.newArrayList();
        for (final Card c : in) {
            result.add(getLKICopy(c, cachedMap));
        }
        return result;
    }

    public static GameEntity getLKICopy(final GameEntity in, Map<Integer, Card> cachedMap) {
        if (in instanceof Card) {
            return getLKICopy((Card)in, cachedMap);
        }
        return in;
    }
    /**
     * @param in  a Card to copy.
     * @return a copy of C with LastKnownInfo stuff retained.
     */
    public static Card getLKICopy(final Card in) {
        if (in == null) {
            return null;
        }
        return getLKICopy(in, Maps.newHashMap());
    }
    public static Card getLKICopy(final Card in, Map<Integer, Card> cachedMap) {
        if (in == null) {
            return null;
        }
        Card cachedCard = cachedMap.get(in.getId());
        if (cachedCard != null) {
            return cachedCard;
        }
        String msg = "CardUtil:getLKICopy copy object";
        Sentry.getContext().recordBreadcrumb(
                new BreadcrumbBuilder().setMessage(msg)
                .withData("Card", in.getName())
                .withData("CardState", in.getCurrentStateName().toString())
                .withData("Player", in.getController().getName())
                .build()
        );

        final Card newCopy = new Card(in.getId(), in.getPaperCard(), in.getGame(), null);
        cachedMap.put(in.getId(), newCopy);
        newCopy.setSetCode(in.getSetCode());
        newCopy.setOwner(in.getOwner());
        newCopy.setController(in.getController(), 0);
        newCopy.setCommander(in.isCommander());

        // needed to ensure that the LKI object has correct CMC info no matter what state the original card was in
        // (e.g. Scrap Trawler + transformed Harvest Hand)
        newCopy.setLKICMC(in.getCMC());
        // used for the purpose of cards that care about the zone the card was known to be in last
        newCopy.setLastKnownZone(in.getLastKnownZone());

        newCopy.getCurrentState().copyFrom(in.getState(in.getFaceupCardStateName()), true);
        if (in.isFaceDown()) {
            newCopy.turnFaceDownNoUpdate();
        }

        if (in.isAdventureCard() && in.getFaceupCardStateName().equals(CardStateName.Original)) {
            newCopy.addAlternateState(CardStateName.Adventure, false);
            newCopy.getState(CardStateName.Adventure).copyFrom(in.getState(CardStateName.Adventure), true);
        }

        /*
        if (in.isCloned()) {
            newCopy.addAlternateState(CardStateName.Cloner, false);
            newCopy.getState(CardStateName.Cloner).copyFrom(in.getState(CardStateName.Cloner), true);
        }
        //*/

        newCopy.setType(new CardType(in.getType()));
        newCopy.setToken(in.isToken());
        newCopy.setCopiedSpell(in.isCopiedSpell());
        newCopy.setImmutable(in.isImmutable());

        // lock in the current P/T
        newCopy.setBasePower(in.getCurrentPower());
        newCopy.setBaseToughness(in.getCurrentToughness());

        // extra copy PT boost
        newCopy.setPTBoost(in.getPTBoostTable());

        newCopy.setCounters(Maps.newHashMap(in.getCounters()));

        newCopy.setColor(in.determineColor().getColor());
        newCopy.setReceivedDamageFromThisTurn(in.getReceivedDamageFromThisTurn());
        newCopy.setReceivedDamageFromPlayerThisTurn(in.getReceivedDamageFromPlayerThisTurn());
        newCopy.getDamageHistory().setCreatureGotBlockedThisTurn(in.getDamageHistory().getCreatureGotBlockedThisTurn());

        newCopy.setAttachedCards(getLKICopyList(in.getAttachedCards(), cachedMap));
        newCopy.setEntityAttachedTo(getLKICopy(in.getEntityAttachedTo(), cachedMap));

        newCopy.setHaunting(in.getHaunting());
        newCopy.setCopiedPermanent(in.getCopiedPermanent());
        for (final Card haunter : in.getHauntedBy()) {
            newCopy.addHauntedBy(haunter, false);
        }
        for (final Object o : in.getRemembered()) {
            newCopy.addRemembered(o);
        }
        for (final Card o : in.getImprintedCards()) {
            newCopy.addImprintedCard(o);
        }

        for(Table.Cell<Player, CounterType, Integer> cl : in.getEtbCounters()) {
            newCopy.addEtbCounter(cl.getColumnKey(), cl.getValue(), cl.getRowKey());
        }

        newCopy.setUnearthed(in.isUnearthed());

        newCopy.setChangedCardColors(in.getChangedCardColors());
        newCopy.setChangedCardKeywords(in.getChangedCardKeywords());
        newCopy.setChangedCardTypes(in.getChangedCardTypesMap());
        newCopy.setChangedCardNames(in.getChangedCardNames());
        newCopy.setChangedCardTraits(in.getChangedCardTraits());

        newCopy.copyChangedTextFrom(in);

        newCopy.setBestowTimestamp(in.getBestowTimestamp());

        newCopy.setForetold(in.isForetold());
        newCopy.setForetoldThisTurn(in.isForetoldThisTurn());
        newCopy.setForetoldByEffect(in.isForetoldByEffect());

        newCopy.setMeldedWith(getLKICopy(in.getMeldedWith(), cachedMap));

        newCopy.setTimestamp(in.getTimestamp());

        // update keyword cache on all states
        for (CardStateName s : newCopy.getStates()) {
            newCopy.updateKeywordsCache(newCopy.getState(s));
        }

        newCopy.setKickerMagnitude(in.getKickerMagnitude());

        for (OptionalCost ocost : in.getOptionalCostsPaid()) {
            newCopy.addOptionalCostPaid(ocost);
        }

        if (in.getCastSA() != null) {
            SpellAbility castSA = in.getCastSA().copy(newCopy, true);
            castSA.setLastStateBattlefield(CardCollection.EMPTY);
            castSA.setLastStateGraveyard(CardCollection.EMPTY);
            newCopy.setCastSA(castSA);
        }
        newCopy.setCastFrom(in.getCastFrom());

        newCopy.setExiledWith(getLKICopy(in.getExiledWith(), cachedMap));

        return newCopy;
    }

    public static CardCollection getRadiance(final SpellAbility sa) {
        if (!sa.usesTargeting() || !sa.hasParam("Radiance")) {
            return new CardCollection();
        }

        final Card source = sa.getHostCard();
        final Game game = source.getGame();
        final CardCollection res = new CardCollection();
        final String[] valid = sa.getParam("ValidTgts").split(",");
        final CardCollectionView tgts = sa.getTargets().getTargetCards();

        byte combinedColor = 0;
        for (Card tgt : tgts) {
            ColorSet cs = CardUtil.getColors(tgt);
            for (byte color : MagicColor.WUBRG) {
                if(!cs.hasAnyColor(color))
                    continue;
                combinedColor |= color;
            }
        }
        for (byte color : MagicColor.WUBRG) {
            if ((combinedColor & color) == 0) {
                continue;
            }
            for(final Card c : game.getColoredCardsInPlay(MagicColor.toLongString(color))) {
                if (!res.contains(c) && !tgts.contains(c) && c.isValid(valid, source.getController(), source, sa)) {
                    res.add(c);
                }
            }
        }

        return res;
    }

    public static ColorSet getColorsYouCtrl(final Player p) {
        byte b = 0;
        for (Card c : p.getCardsIn(ZoneType.Battlefield)) {
            b |= c.determineColor().getColor();
        }
        return ColorSet.fromMask(b);
    }

    public static CardState getFaceDownCharacteristic(Card c) {
        return getFaceDownCharacteristic(c, CardStateName.FaceDown);
    }

    public static CardState getFaceDownCharacteristic(Card c, CardStateName state) {
        final CardType type = new CardType(false);
        type.add("Creature");

        final CardState ret = new CardState(c, state);
        ret.setBasePower(2);
        ret.setBaseToughness(2);

        ret.setName("");
        ret.setType(type);

        //show hidden if exiled facedown
        if (state == CardStateName.FaceDown) {
            if (c.isInZone(ZoneType.Exile))
                ret.setImageKey(ImageKeys.getTokenKey(c.isForetold() ? ImageKeys.FORETELL_IMAGE : ImageKeys.HIDDEN_CARD));
            else
                ret.setImageKey(ImageKeys.getTokenKey(c.isManifested() ? ImageKeys.MANIFEST_IMAGE : ImageKeys.MORPH_IMAGE));
        } else {
            ret.setImageKey(c.getImageKey());
        }
        return ret;
    }

    // a nice entry point with minimum parameters
    public static Set<String> getReflectableManaColors(final SpellAbility sa) {
        return getReflectableManaColors(sa, sa, Sets.newHashSet(), new CardCollection());
    }

    private static Set<String> getReflectableManaColors(final SpellAbility abMana, final SpellAbility sa,
            Set<String> colors, final CardCollection parents) {
        // Here's the problem with reflectable Mana. If more than one is out,
        // they need to Reflect each other,
        // so we basically need to have a recursive list that send the parents
        // so we don't infinite recurse.
        final Card card = abMana.getHostCard();

        if (abMana.getApi() != ApiType.ManaReflected) {
            return colors;
        }

        final String colorOrType = sa.getParam("ColorOrType");
        // currently Color or Type, Type is colors + colorless
        final String reflectProperty = sa.getParam("ReflectProperty");
        // Produce (Reflecting Pool) or Is (Meteor Crater)

        int maxChoices = 5; // Color is the default colorOrType
        if (colorOrType.equals("Type")) {
            maxChoices++;
        }

        CardCollection cards;

        if (sa.hasParam("Valid")) {
            final String validCard = sa.getParam("Valid");
            // Reuse AF_Defined in a slightly different way
            if (validCard.startsWith("Defined.")) {
                cards = AbilityUtils.getDefinedCards(card, TextUtil.fastReplace(validCard, "Defined.", ""), abMana);
            } else {
                if (sa.getActivatingPlayer() == null) {
                    sa.setActivatingPlayer(sa.getHostCard().getController());
                }
                final Game game = sa.getActivatingPlayer().getGame();
                cards = CardLists.getValidCards(game.getCardsIn(ZoneType.Battlefield), validCard, abMana.getActivatingPlayer(), card, sa);
            }

            // remove anything cards that is already in parents
            cards.removeAll(parents);

            if (cards.isEmpty()) {
                return colors;
            }
        } else {
            cards = new CardCollection();
        }
        if (reflectProperty.equals("Is")) { // Meteor Crater
            for (final Card card1 : cards) {
                // For each card, go through all the colors and if the card is that color, add
                for (final String col : MagicColor.Constant.ONLY_COLORS) {
                    if (card1.isOfColor(col)) {
                        colors.add(col);
                        if (colors.size() == maxChoices) {
                            break;
                        }
                    }
                }
            }
        } else if (reflectProperty.equals("Produced")) {
            // Why is this name so similar to the one below?
            final String producedColors = (String) abMana.getRootAbility().getTriggeringObject(AbilityKey.Produced);
            for (final String col : MagicColor.Constant.ONLY_COLORS) {
                final String s = MagicColor.toShortString(col);
                if (producedColors.contains(s)) {
                    colors.add(col);
                }
            }
            // TODO Sol Remove production of "1" Generic Mana
            if (maxChoices == 6 && (producedColors.contains("1") || producedColors.contains("C"))) {
                colors.add(MagicColor.Constant.COLORLESS);
            }
        } else if (reflectProperty.equals("Produce")) {
            final FCollection<SpellAbility> abilities = new FCollection<>();
            for (final Card c : cards) {
                abilities.addAll(c.getManaAbilities());
            }

            final List<SpellAbility> reflectAbilities = Lists.newArrayList();

            for (final SpellAbility ab : abilities) {
                if (maxChoices == colors.size()) {
                    break;
                }

                if (ab.getApi() == ApiType.ManaReflected) {
                    if (!parents.contains(ab.getHostCard())) {
                        // Recursion! Set Activator to controller for appropriate valid comparison
                        ab.setActivatingPlayer(ab.getHostCard().getController());
                        reflectAbilities.add(ab);
                        parents.add(ab.getHostCard());
                    }
                    continue;
                }
                colors = canProduce(maxChoices, ab, colors);
                if (!parents.contains(ab.getHostCard())) {
                    parents.add(ab.getHostCard());
                }
            }

            for (final SpellAbility ab : reflectAbilities) {
                if (maxChoices == colors.size()) {
                    break;
                }

                colors = CardUtil.getReflectableManaColors(ab, sa, colors, parents);
            }
        }
        return colors;
    }

    public static Set<String> canProduce(final int maxChoices, final SpellAbility sa,
            final Set<String> colors) {
        if (sa == null) {
            return colors;
        }
        for (final String col : MagicColor.Constant.ONLY_COLORS) {
            if (sa.canProduce(MagicColor.toShortString(col))) {
                colors.add(col);
            }
        }

        if (maxChoices == 6 && sa.canProduce("C")) {
            colors.add(MagicColor.Constant.COLORLESS);
        }

        return colors;
    }

    // these have been copied over from CardFactoryUtil as they need two extra
    // parameters for target selection.
    // however, due to the changes necessary for SA_Requirements this is much
    // different than the original
    public static List<Card> getValidCardsToTarget(TargetRestrictions tgt, SpellAbility ability) {
        final Game game = ability.getActivatingPlayer().getGame();
        final List<ZoneType> zone = tgt.getZone();

        final boolean canTgtStack = zone.contains(ZoneType.Stack);
        List<Card> validCards = CardLists.getValidCards(game.getCardsIn(zone), tgt.getValidTgts(), ability.getActivatingPlayer(), ability.getHostCard(), ability);
        List<Card> choices = CardLists.getTargetableCards(validCards, ability);
        if (canTgtStack) {
            // Since getTargetableCards doesn't have additional checks if one of the Zones is stack
            // Remove the activating card from targeting itself if its on the Stack
            Card activatingCard = ability.getHostCard();
            if (activatingCard.isInZone(ZoneType.Stack)) {
                choices.remove(ability.getHostCard());
            }
        }
        List<GameObject> targetedObjects = ability.getUniqueTargets();

        // Remove cards already targeted
        final List<Card> targeted = Lists.newArrayList(ability.getTargets().getTargetCards());
        for (final Card c : targeted) {
            choices.remove(c);
        }

        // Remove cards exceeding total CMC
        if (ability.hasParam("MaxTotalTargetCMC")) {
            int totalCMCTargeted = 0;
            for (final Card c : targeted) {
                totalCMCTargeted += c.getCMC();
            }

            final List<Card> choicesCopy = Lists.newArrayList(choices);
            for (final Card c : choicesCopy) {
                if (c.getCMC() > tgt.getMaxTotalCMC(c, ability) - totalCMCTargeted) {
                    choices.remove(c);
                }
            }
        }

        // Remove cards exceeding total power
        if (ability.hasParam("MaxTotalTargetPower")) {
            int totalPowerTargeted = 0;
            for (final Card c : targeted) {
                totalPowerTargeted += c.getNetPower();
            }

            final List<Card> choicesCopy = Lists.newArrayList(choices);
            for (final Card c : choicesCopy) {
                if (c.getNetPower() > tgt.getMaxTotalPower(c, ability) - totalPowerTargeted) {
                    choices.remove(c);
                }
            }
        }

        // If all cards (including subability targets) must have the same controller
        if (tgt.isSameController() && !targetedObjects.isEmpty()) {
            final List<Card> list = Lists.newArrayList();
            for (final Object o : targetedObjects) {
                if (o instanceof Card) {
                    list.add((Card) o);
                }
            }
            if (!list.isEmpty()) {
                final Card card = list.get(0);
                choices = CardLists.filter(choices, CardPredicates.sharesControllerWith(card));
            }
        }

        return choices;
    }
}
