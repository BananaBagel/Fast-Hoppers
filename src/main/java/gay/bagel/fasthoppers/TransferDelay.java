package gay.bagel.fasthoppers;

/**
 * Maps vanilla hopper cooldown values onto the delay configured by gamerule.
 *
 * <p>
 * Contains no Minecraft types by design, so its behaviour is unit testable
 * without a running game. This is where the only non-trivial arithmetic in the
 * mod lives.
 */
public final class TransferDelay {

    /**
     * The cooldown vanilla applies to a hopper after a normal transfer.
     * Offsets from this value carry meaning — see {@link #apply(int, int)}.
     */
    public static final int VANILLA_HOPPER_DELAY = 8;

	// Disable the mixin when gamerule is 8?
    public static final boolean DISABLED = Boolean.getBoolean("fasthoppers.disabled");

    private TransferDelay() {
        throw new AssertionError("No instances.");
    }

	// it do be applying
    public static int apply(int vanillaValue, int gameruleValue) {
        if (vanillaValue <= 0) {
            return vanillaValue;
        }
        int offset = vanillaValue - VANILLA_HOPPER_DELAY;
        return Math.max(0, gameruleValue + offset);
    }


}
