package gay.bagel.fasthoppers.platform.neoforge;

//? neoforge {

/*import gay.bagel.fasthoppers.platform.Platform;
import net.neoforged.fml.ModList;
import net.neoforged.fml.loading.FMLLoader;
import net.neoforged.fml.loading.VersionInfo;

public class NeoforgePlatform implements Platform {

	@Override
	public boolean isModLoaded(String modId) {
		return ModList.get().isLoaded(modId);
	}

	@Override
	public ModLoader loader() {
		return ModLoader.NEOFORGE;
	}

	@Override
	public String mcVersion() {
		// Same split as isProduction below, and for the same reason: FML's loader became an instance
		// after 1.21.7. The static versionInfo() and the instance getVersionInfo() never coexist —
		// loader 9 and earlier have only the former, loader 10 and later only the latter.
		//? > 1.21.7 {
		return FMLLoader.getCurrent().getVersionInfo().mcVersion();
		//?} <= 1.21.7 {
		/^return FMLLoader.versionInfo().mcVersion();
		^///?}
	}

	@Override
	public boolean isDevelopmentEnvironment() {
		return !FMLLoader/^? if > 1.21.7 {^/.getCurrent()/^?}^/.isProduction();
	}
}
*///?}
