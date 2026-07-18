package com.Huziyang520.alexscavessaccuratemobspawn;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.config.ModConfigEvent;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.HashMap;
import java.util.Map;

@Mod.EventBusSubscriber(modid = alexscavessaccuratemobspawn.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class Config {

    private static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();

    // ==================== Magnetic Caves ====================
    public static final ForgeConfigSpec.DoubleValue MAGNETRON;
    public static final ForgeConfigSpec.DoubleValue TELETOR;
    public static final ForgeConfigSpec.DoubleValue BOUNDROID;
    public static final ForgeConfigSpec.DoubleValue FERROUSLIME;
    public static final ForgeConfigSpec.DoubleValue NOTOR;

    // ==================== Toxic Caves ====================
    public static final ForgeConfigSpec.DoubleValue GAMMAROACH;
    public static final ForgeConfigSpec.DoubleValue BRAINIAC;
    public static final ForgeConfigSpec.DoubleValue NUCLEEPER;
    public static final ForgeConfigSpec.DoubleValue RAYCAT;
    public static final ForgeConfigSpec.DoubleValue RADGILL;

    // ==================== Primordial Caves ====================
    public static final ForgeConfigSpec.DoubleValue SUBTERRANODON;
    public static final ForgeConfigSpec.DoubleValue VALLUMRAPTOR;
    public static final ForgeConfigSpec.DoubleValue GROTTOCERATOPS;
    public static final ForgeConfigSpec.DoubleValue TREMORSAURUS;
    public static final ForgeConfigSpec.DoubleValue RELICHEIRUS;
    public static final ForgeConfigSpec.DoubleValue ATLATITAN;
    public static final ForgeConfigSpec.DoubleValue TRILOCARIS;

    // ==================== Abyssal Chasm ====================
    public static final ForgeConfigSpec.DoubleValue DEEP_ONE;
    public static final ForgeConfigSpec.DoubleValue DEEP_ONE_KNIGHT;
    public static final ForgeConfigSpec.DoubleValue DEEP_ONE_MAGE;
    public static final ForgeConfigSpec.DoubleValue LANTERNFISH;
    public static final ForgeConfigSpec.DoubleValue HULLBREAKER;
    public static final ForgeConfigSpec.DoubleValue TRIPODFISH;
    public static final ForgeConfigSpec.DoubleValue SEA_PIG;
    public static final ForgeConfigSpec.DoubleValue GOSSAMER_WORM;
    public static final ForgeConfigSpec.DoubleValue MINE_GUARDIAN;

    // ==================== Forlorn Hollows ====================
    public static final ForgeConfigSpec.DoubleValue GLOOMOTH;
    public static final ForgeConfigSpec.DoubleValue UNDERZEALOT;
    public static final ForgeConfigSpec.DoubleValue CORRODENT;
    public static final ForgeConfigSpec.DoubleValue VESPER;
    public static final ForgeConfigSpec.DoubleValue WATCHER;
    public static final ForgeConfigSpec.DoubleValue FORSAKEN;

    // ==================== Candy Cavity ====================
    public static final ForgeConfigSpec.DoubleValue CANDICORN;
    public static final ForgeConfigSpec.DoubleValue GUMMY_BEAR;
    public static final ForgeConfigSpec.DoubleValue CANIAC;
    public static final ForgeConfigSpec.DoubleValue GUMBEEPER;
    public static final ForgeConfigSpec.DoubleValue GUM_WORM;
    public static final ForgeConfigSpec.DoubleValue CARAMEL_CUBE;
    public static final ForgeConfigSpec.DoubleValue LICOWITCH;
    public static final ForgeConfigSpec.DoubleValue GINGERBREAD_MAN;
    public static final ForgeConfigSpec.DoubleValue SWEETISH_FISH;

    // ==================== Special Mobs ====================
    public static final ForgeConfigSpec.DoubleValue LUXTRUCTOSAURUS;
    public static final ForgeConfigSpec.DoubleValue TREMORZILLA;

    public static final ForgeConfigSpec SPEC;

    // Lookup map: entity registry name -> config value
    private static final Map<String, ForgeConfigSpec.DoubleValue> MULTIPLIER_MAP = new HashMap<>();

    static {
        BUILDER.push("spawn_multipliers");

        BUILDER.push("magnetic_caves");
        MAGNETRON = BUILDER.comment("Spawn rate multiplier for Magnetron (magnetic caves). Default: 1.0").defineInRange("magnetron", 1.0D, 0.0D, 10.0D);
        TELETOR = BUILDER.comment("Spawn rate multiplier for Teletor (magnetic caves). Default: 1.0").defineInRange("teletor", 1.0D, 0.0D, 10.0D);
        BOUNDROID = BUILDER.comment("Spawn rate multiplier for Boundroid (magnetic caves). Default: 1.0").defineInRange("boundroid", 1.0D, 0.0D, 10.0D);
        FERROUSLIME = BUILDER.comment("Spawn rate multiplier for Ferrouslime (magnetic caves). Default: 1.0").defineInRange("ferrouslime", 1.0D, 0.0D, 10.0D);
        NOTOR = BUILDER.comment("Spawn rate multiplier for Notor (magnetic caves). Default: 1.0").defineInRange("notor", 1.0D, 0.0D, 10.0D);
        BUILDER.pop();

        BUILDER.push("toxic_caves");
        GAMMAROACH = BUILDER.comment("Spawn rate multiplier for Gammaroach (toxic caves). Default: 1.0").defineInRange("gammaroach", 1.0D, 0.0D, 10.0D);
        BRAINIAC = BUILDER.comment("Spawn rate multiplier for Brainiac (toxic caves). Default: 1.0").defineInRange("brainiac", 1.0D, 0.0D, 10.0D);
        NUCLEEPER = BUILDER.comment("Spawn rate multiplier for Nucleeper (toxic caves). Default: 1.0").defineInRange("nucleeper", 1.0D, 0.0D, 10.0D);
        RAYCAT = BUILDER.comment("Spawn rate multiplier for Raycat (toxic caves). Default: 1.0").defineInRange("raycat", 1.0D, 0.0D, 10.0D);
        RADGILL = BUILDER.comment("Spawn rate multiplier for Radgill (toxic caves). Default: 1.0").defineInRange("radgill", 1.0D, 0.0D, 10.0D);
        BUILDER.pop();

        BUILDER.push("primordial_caves");
        SUBTERRANODON = BUILDER.comment("Spawn rate multiplier for Subterranodon (primordial caves). Default: 1.0").defineInRange("subterranodon", 1.0D, 0.0D, 10.0D);
        VALLUMRAPTOR = BUILDER.comment("Spawn rate multiplier for Vallumraptor (primordial caves). Default: 1.0").defineInRange("vallumraptor", 1.0D, 0.0D, 10.0D);
        GROTTOCERATOPS = BUILDER.comment("Spawn rate multiplier for Grottoceratops (primordial caves). Default: 1.0").defineInRange("grottoceratops", 1.0D, 0.0D, 10.0D);
        TREMORSAURUS = BUILDER.comment("Spawn rate multiplier for Tremorsaurus (primordial caves). Default: 1.0").defineInRange("tremorsaurus", 1.0D, 0.0D, 10.0D);
        RELICHEIRUS = BUILDER.comment("Spawn rate multiplier for Relicheirus (primordial caves). Default: 1.0").defineInRange("relicheirus", 1.0D, 0.0D, 10.0D);
        ATLATITAN = BUILDER.comment("Spawn rate multiplier for Atlatitan (primordial caves). Default: 1.0").defineInRange("atlatitan", 1.0D, 0.0D, 10.0D);
        TRILOCARIS = BUILDER.comment("Spawn rate multiplier for Trilocaris (primordial caves). Default: 1.0").defineInRange("trilocaris", 1.0D, 0.0D, 10.0D);
        BUILDER.pop();

        BUILDER.push("abyssal_chasm");
        DEEP_ONE = BUILDER.comment("Spawn rate multiplier for Deep One (abyssal chasm). Default: 1.0").defineInRange("deep_one", 1.0D, 0.0D, 10.0D);
        DEEP_ONE_KNIGHT = BUILDER.comment("Spawn rate multiplier for Deep One Knight (abyssal chasm). Default: 1.0").defineInRange("deep_one_knight", 1.0D, 0.0D, 10.0D);
        DEEP_ONE_MAGE = BUILDER.comment("Spawn rate multiplier for Deep One Mage (abyssal chasm). Default: 1.0").defineInRange("deep_one_mage", 1.0D, 0.0D, 10.0D);
        LANTERNFISH = BUILDER.comment("Spawn rate multiplier for Lanternfish (abyssal chasm). Default: 1.0").defineInRange("lanternfish", 1.0D, 0.0D, 10.0D);
        HULLBREAKER = BUILDER.comment("Spawn rate multiplier for Hullbreaker (abyssal chasm). Default: 1.0").defineInRange("hullbreaker", 1.0D, 0.0D, 10.0D);
        TRIPODFISH = BUILDER.comment("Spawn rate multiplier for Tripodfish (abyssal chasm). Default: 1.0").defineInRange("tripodfish", 1.0D, 0.0D, 10.0D);
        SEA_PIG = BUILDER.comment("Spawn rate multiplier for Sea Pig (abyssal chasm). Default: 1.0").defineInRange("sea_pig", 1.0D, 0.0D, 10.0D);
        GOSSAMER_WORM = BUILDER.comment("Spawn rate multiplier for Gossamer Worm (abyssal chasm). Default: 1.0").defineInRange("gossamer_worm", 1.0D, 0.0D, 10.0D);
        MINE_GUARDIAN = BUILDER.comment("Spawn rate multiplier for Mine Guardian (abyssal chasm). Default: 1.0").defineInRange("mine_guardian", 1.0D, 0.0D, 10.0D);
        BUILDER.pop();

        BUILDER.push("forlorn_hollows");
        GLOOMOTH = BUILDER.comment("Spawn rate multiplier for Gloomoth (forlorn hollows). Default: 1.0").defineInRange("gloomoth", 1.0D, 0.0D, 10.0D);
        UNDERZEALOT = BUILDER.comment("Spawn rate multiplier for Underzealot (forlorn hollows). Default: 1.0").defineInRange("underzealot", 1.0D, 0.0D, 10.0D);
        CORRODENT = BUILDER.comment("Spawn rate multiplier for Corrodent (forlorn hollows). Default: 1.0").defineInRange("corrodent", 1.0D, 0.0D, 10.0D);
        VESPER = BUILDER.comment("Spawn rate multiplier for Vesper (forlorn hollows). Default: 1.0").defineInRange("vesper", 1.0D, 0.0D, 10.0D);
        WATCHER = BUILDER.comment("Spawn rate multiplier for Watcher (forlorn hollows). Default: 1.0").defineInRange("watcher", 1.0D, 0.0D, 10.0D);
        FORSAKEN = BUILDER.comment("Spawn rate multiplier for Forsaken (forlorn hollows). Default: 1.0").defineInRange("forsaken", 1.0D, 0.0D, 10.0D);
        BUILDER.pop();

        BUILDER.push("candy_cavity");
        CANDICORN = BUILDER.comment("Spawn rate multiplier for Candicorn (candy cavity). Default: 1.0").defineInRange("candicorn", 1.0D, 0.0D, 10.0D);
        GUMMY_BEAR = BUILDER.comment("Spawn rate multiplier for Gummy Bear (candy cavity). Default: 1.0").defineInRange("gummy_bear", 1.0D, 0.0D, 10.0D);
        CANIAC = BUILDER.comment("Spawn rate multiplier for Caniac (candy cavity). Default: 1.0").defineInRange("caniac", 1.0D, 0.0D, 10.0D);
        GUMBEEPER = BUILDER.comment("Spawn rate multiplier for Gumbeeper (candy cavity). Default: 1.0").defineInRange("gumbeeper", 1.0D, 0.0D, 10.0D);
        GUM_WORM = BUILDER.comment("Spawn rate multiplier for Gum Worm (candy cavity). Default: 1.0").defineInRange("gum_worm", 1.0D, 0.0D, 10.0D);
        CARAMEL_CUBE = BUILDER.comment("Spawn rate multiplier for Caramel Cube (candy cavity). Default: 1.0").defineInRange("caramel_cube", 1.0D, 0.0D, 10.0D);
        LICOWITCH = BUILDER.comment("Spawn rate multiplier for Licowitch (candy cavity). Default: 1.0").defineInRange("licowitch", 1.0D, 0.0D, 10.0D);
        GINGERBREAD_MAN = BUILDER.comment("Spawn rate multiplier for Gingerbread Man (candy cavity). Default: 1.0").defineInRange("gingerbread_man", 1.0D, 0.0D, 10.0D);
        SWEETISH_FISH = BUILDER.comment("Spawn rate multiplier for Sweetish Fish (candy cavity). Default: 1.0").defineInRange("sweetish_fish", 1.0D, 0.0D, 10.0D);
        BUILDER.pop();

        BUILDER.push("special");
        LUXTRUCTOSAURUS = BUILDER.comment("Spawn rate multiplier for Luxtructosaurus (surface boss). Default: 1.0").defineInRange("luxtructosaurus", 1.0D, 0.0D, 10.0D);
        TREMORZILLA = BUILDER.comment("Spawn rate multiplier for Tremorzilla (primordial boss). Default: 1.0").defineInRange("tremorzilla", 1.0D, 0.0D, 10.0D);
        BUILDER.pop();

        BUILDER.pop(); // spawn_multipliers

        SPEC = BUILDER.build();

        MULTIPLIER_MAP.put("alexscaves:magnetron", MAGNETRON);
        MULTIPLIER_MAP.put("alexscaves:teletor", TELETOR);
        MULTIPLIER_MAP.put("alexscaves:boundroid", BOUNDROID);
        MULTIPLIER_MAP.put("alexscaves:ferrouslime", FERROUSLIME);
        MULTIPLIER_MAP.put("alexscaves:notor", NOTOR);

        MULTIPLIER_MAP.put("alexscaves:gammaroach", GAMMAROACH);
        MULTIPLIER_MAP.put("alexscaves:brainiac", BRAINIAC);
        MULTIPLIER_MAP.put("alexscaves:nucleeper", NUCLEEPER);
        MULTIPLIER_MAP.put("alexscaves:raycat", RAYCAT);
        MULTIPLIER_MAP.put("alexscaves:radgill", RADGILL);

        MULTIPLIER_MAP.put("alexscaves:subterranodon", SUBTERRANODON);
        MULTIPLIER_MAP.put("alexscaves:vallumraptor", VALLUMRAPTOR);
        MULTIPLIER_MAP.put("alexscaves:grottoceratops", GROTTOCERATOPS);
        MULTIPLIER_MAP.put("alexscaves:tremorsaurus", TREMORSAURUS);
        MULTIPLIER_MAP.put("alexscaves:relicheirus", RELICHEIRUS);
        MULTIPLIER_MAP.put("alexscaves:atlatitan", ATLATITAN);
        MULTIPLIER_MAP.put("alexscaves:trilocaris", TRILOCARIS);

        MULTIPLIER_MAP.put("alexscaves:deep_one", DEEP_ONE);
        MULTIPLIER_MAP.put("alexscaves:deep_one_knight", DEEP_ONE_KNIGHT);
        MULTIPLIER_MAP.put("alexscaves:deep_one_mage", DEEP_ONE_MAGE);
        MULTIPLIER_MAP.put("alexscaves:lanternfish", LANTERNFISH);
        MULTIPLIER_MAP.put("alexscaves:hullbreaker", HULLBREAKER);
        MULTIPLIER_MAP.put("alexscaves:tripodfish", TRIPODFISH);
        MULTIPLIER_MAP.put("alexscaves:sea_pig", SEA_PIG);
        MULTIPLIER_MAP.put("alexscaves:gossamer_worm", GOSSAMER_WORM);
        MULTIPLIER_MAP.put("alexscaves:mine_guardian", MINE_GUARDIAN);

        MULTIPLIER_MAP.put("alexscaves:gloomoth", GLOOMOTH);
        MULTIPLIER_MAP.put("alexscaves:underzealot", UNDERZEALOT);
        MULTIPLIER_MAP.put("alexscaves:corrodent", CORRODENT);
        MULTIPLIER_MAP.put("alexscaves:vesper", VESPER);
        MULTIPLIER_MAP.put("alexscaves:watcher", WATCHER);
        MULTIPLIER_MAP.put("alexscaves:forsaken", FORSAKEN);

        MULTIPLIER_MAP.put("alexscaves:candicorn", CANDICORN);
        MULTIPLIER_MAP.put("alexscaves:gummy_bear", GUMMY_BEAR);
        MULTIPLIER_MAP.put("alexscaves:caniac", CANIAC);
        MULTIPLIER_MAP.put("alexscaves:gumbeeper", GUMBEEPER);
        MULTIPLIER_MAP.put("alexscaves:gum_worm", GUM_WORM);
        MULTIPLIER_MAP.put("alexscaves:caramel_cube", CARAMEL_CUBE);
        MULTIPLIER_MAP.put("alexscaves:licowitch", LICOWITCH);
        MULTIPLIER_MAP.put("alexscaves:gingerbread_man", GINGERBREAD_MAN);
        MULTIPLIER_MAP.put("alexscaves:sweetish_fish", SWEETISH_FISH);

        MULTIPLIER_MAP.put("alexscaves:luxtructosaurus", LUXTRUCTOSAURUS);
        MULTIPLIER_MAP.put("alexscaves:tremorzilla", TREMORZILLA);
    }

    public static double getMultiplier(EntityType<?> entityType) {
        ResourceLocation id = ForgeRegistries.ENTITY_TYPES.getKey(entityType);
        if (id != null) {
            ForgeConfigSpec.DoubleValue config = MULTIPLIER_MAP.get(id.toString());
            if (config != null) {
                return config.get();
            }
        }
        return 1.0D;
    }

    @SubscribeEvent
    public static void onLoad(final ModConfigEvent event) {
        if (event.getConfig().getModId().equals(alexscavessaccuratemobspawn.MOD_ID)) {
            if (event instanceof ModConfigEvent.Reloading) {
                alexscavessaccuratemobspawn.LOGGER.info("Spawn multipliers config reloaded. Changes take effect for next spawns.");
            }
        }
    }
}