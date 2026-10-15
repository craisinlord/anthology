package com.craisinlord.anthology.compat.irons_spellbooks;

import com.craisinlord.anthology.time.TimeDilationApi;
import com.craisinlord.anthology.time.TimeDilationHooks;
import com.craisinlord.anthology.time.TimeDilationMath;
import io.redspace.ironsspellbooks.entity.mobs.IMagicSummon;
import net.minecraft.world.entity.Entity;

final class IronsSpellbooksSummonRates {
    private IronsSpellbooksSummonRates() {
    }

    static void register() {
        TimeDilationHooks.addRateSource(IronsSpellbooksSummonRates::summonerRate);
        TimeDilationHooks.addTrackedPredicate(entity -> Math.abs(summonerRate(entity) - TimeDilationMath.NORMAL_RATE) > 0.001D);
    }

    private static double summonerRate(Entity entity) {
        if (entity instanceof IMagicSummon summon && summon.getSummoner() instanceof Entity owner && owner != entity) {
            return TimeDilationApi.getRate(owner);
        }
        return TimeDilationMath.NORMAL_RATE;
    }
}
