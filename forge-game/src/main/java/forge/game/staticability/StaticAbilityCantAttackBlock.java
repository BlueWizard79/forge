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
package forge.game.staticability;

import com.google.common.collect.Iterables;

import forge.game.GameEntity;
import forge.game.card.Card;
import forge.game.card.CardCollectionView;
import forge.game.card.CardFactoryUtil;
import forge.game.card.CardPredicates;
import forge.game.cost.Cost;
import forge.game.keyword.KeywordInterface;
import forge.game.player.Player;
import forge.game.zone.ZoneType;

/**
 * The Class StaticAbility_CantBeCast.
 */
public class StaticAbilityCantAttackBlock {

    /**
     * TODO Write javadoc for this method.
     * 
     * @param stAb
     *            a StaticAbility
     * @param card
     *            the card
     * @return a Cost
     */
    public static boolean applyCantAttackAbility(final StaticAbility stAb, final Card card, final GameEntity target) {
        final Card hostCard = stAb.getHostCard();

        if (!stAb.matchesValidParam("ValidCard", card)) {
            return false;
        }

        if (!stAb.matchesValidParam("Target", target)) {
            return false;
        }

        final Player defender = target instanceof Card ? ((Card) target).getController() : (Player) target;

        if (stAb.hasParam("UnlessDefenderControls")) {
            String type = stAb.getParam("UnlessDefenderControls");
            CardCollectionView list = defender.getCardsIn(ZoneType.Battlefield);
            if (Iterables.any(list, CardPredicates.restriction(type.split(","), hostCard.getController(), hostCard, stAb))) {
                return false;
            }
        }
        if (stAb.hasParam("IfDefenderControls")) {
            String type = stAb.getParam("IfDefenderControls");
            CardCollectionView list = defender.getCardsIn(ZoneType.Battlefield);
            if (!Iterables.any(list, CardPredicates.restriction(type.split(","), hostCard.getController(), hostCard, stAb))) {
                return false;
            }
        }
        if (stAb.hasParam("DefenderNotNearestToYouInChosenDirection")
                && hostCard.getChosenDirection() != null
                && defender.equals(hostCard.getGame().getNextPlayerAfter(card.getController(), hostCard.getChosenDirection()))) {
            return false;
        }
        if (stAb.hasParam("UnlessDefender")) {
            final String type = stAb.getParam("UnlessDefender");
            if (defender.hasProperty(type, hostCard.getController(), hostCard, stAb)) {
                return false;
            }
        }

        return true;
    }

    /**
     * returns true if attacker can be blocked by blocker
     * @param stAb
     * @param attacker
     * @param blocker
     * @return boolean
     */
    public static boolean applyCantBlockByAbility(final StaticAbility stAb, final Card attacker, final Card blocker) {
        final Card host = stAb.getHostCard();
        if (!stAb.matchesValidParam("ValidAttacker", attacker)) {
            return false;
        }
        if (stAb.hasParam("ValidBlocker")) {
            for (final String v : stAb.getParam("ValidBlocker").split(",")) {
                if (blocker.isValid(v, host.getController(), host, stAb)) {
                    boolean stillblock = false;
                    //Dragon Hunter check
                    if (v.contains("withoutReach") && blocker.hasStartOfKeyword("IfReach")) {
                        for (KeywordInterface inst : blocker.getKeywords()) {
                            String k = inst.getOriginal();
                            if (k.startsWith("IfReach")) {
                                String[] n = k.split(":");
                                if (attacker.getType().hasCreatureType(n[1])) {
                                    stillblock = true;
                                    break;
                                }
                            }
                        }
                    }
                    if (!stillblock) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    /**
     * TODO Write javadoc for this method.
     * 
     * @param stAb
     *            a StaticAbility
     * @param card
     *            the card
     * @return a Cost
     */
    public static Cost getAttackCost(final StaticAbility stAb, final Card card, final GameEntity target) {
        final Card hostCard = stAb.getHostCard();

        if (!stAb.matchesValidParam("ValidCard", card)) {
            return null;
        }

        if (!stAb.matchesValidParam("Target", target)) {
            return null;
        }
        String costString = stAb.getParam("Cost");
        if (stAb.hasSVar(costString)) {
            costString = Integer.toString(CardFactoryUtil.xCount(hostCard, stAb.getSVar(costString)));
        }

        return new Cost(costString, true);
    }

    /**
     * TODO Write javadoc for this method.
     * 
     * @param stAb
     *            a StaticAbility
     * @param blocker
     *            the card
     * @return a Cost
     */
    public static Cost getBlockCost(final StaticAbility stAb, final Card blocker, final GameEntity attacker) {
        final Card hostCard = stAb.getHostCard();

        if (!stAb.matchesValidParam("ValidCard", blocker)) {
            return null;
        }

        if (!stAb.matchesValidParam("Attacker", attacker)) {
            return null;
        }
        String costString = stAb.getParam("Cost");
        if (stAb.hasSVar(costString)) {
            costString = Integer.toString(CardFactoryUtil.xCount(hostCard, stAb.getSVar(costString)));
        }

        return new Cost(costString, true);
    }

    public static boolean applyCanAttackHasteAbility(final StaticAbility stAb, final Card card, final GameEntity target) {
        if (!stAb.matchesValidParam("ValidCard", card)) {
            return false;
        }

        if (!stAb.matchesValidParam("ValidTarget", target)) {
            return false;
        }

        final Player defender = target instanceof Card ? ((Card) target).getController() : (Player) target;
        if (!stAb.matchesValidParam("ValidDefender", defender)) {
            return false;
        }
        return true;
    }
}
