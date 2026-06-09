package com.kyluua.verity.dialogue;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.kyluua.verity.Verity;
import com.kyluua.verity.progression.CorruptionData;
import com.kyluua.verity.progression.CorruptionStage;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.profiling.ProfilerFiller;
import net.neoforged.neoforge.event.AddReloadListenerEvent;
import net.neoforged.bus.api.SubscribeEvent;

import javax.annotation.Nullable;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Loads and serves Verity's dialogue. Data-driven: every pool is a JSON file under
 * {@code data/verity/dialogue/<pool>.json}, so writers can add lines without code.
 *
 * <p>Selection is <b>weighted</b> and <b>anti-repeat</b>: each pool keeps a short
 * history of recently used lines and avoids them, so Verity never feels like it is
 * looping. Lines support tokens ({@code {player}}, {@code {encounters}},
 * {@code {structure}}) substituted from a per-call context, and each line carries a
 * stage-appropriate voice + emotion tag so the audio matches the mood.</p>
 *
 * <p>Pools are conventionally named after stages ({@code friendly}, {@code unsettling},
 * {@code psychological}, {@code hostile}, {@code final}) plus {@code tips}. The
 * {@link #pickForStage} helper maps the current corruption stage to its pool.</p>
 */
public class DialogueManager extends SimpleJsonResourceReloadListener {

    private static final DialogueManager INSTANCE = new DialogueManager();

    /** poolName -> entries. */
    private final Map<String, List<DialogueEntry>> pools = new HashMap<>();

    /** poolName -> recently spoken texts (ring buffer) to suppress repeats. */
    private final Map<String, Deque<String>> recent = new HashMap<>();

    private static final int RECENT_MEMORY = 5;

    private static final Gson GSON = new Gson();

    private DialogueManager() {
        super(GSON, "dialogue");
    }

    public static DialogueManager get() {
        return INSTANCE;
    }

    /** Registers this manager as a datapack reload listener (Forge game bus). */
    @SubscribeEvent
    public void onAddReloadListener(AddReloadListenerEvent event) {
        event.addListener(this);
    }

    // =========================================================================
    //  Loading
    // =========================================================================
    @Override
    protected void apply(Map<ResourceLocation, JsonElement> data, ResourceManager manager, ProfilerFiller profiler) {
        pools.clear();
        recent.clear();
        for (Map.Entry<ResourceLocation, JsonElement> e : data.entrySet()) {
            // Only load our own namespace.
            if (!e.getKey().getNamespace().equals(Verity.MOD_ID)) continue;
            String poolName = e.getKey().getPath();
            try {
                JsonObject root = e.getValue().getAsJsonObject();
                JsonArray entries = GsonHelper.getAsJsonArray(root, "entries");
                List<DialogueEntry> list = new ArrayList<>();
                for (JsonElement el : entries) {
                    list.add(DialogueEntry.fromJson(el.getAsJsonObject()));
                }
                pools.put(poolName, list);
            } catch (Exception ex) {
                Verity.LOGGER.error("[Verity] Failed to load dialogue pool '{}'", poolName, ex);
            }
        }
        Verity.LOGGER.info("[Verity] Loaded {} dialogue pools.", pools.size());
    }

    // =========================================================================
    //  Selection
    // =========================================================================

    /** Picks a line for the given corruption stage, addressed to a specific player. */
    @Nullable
    public DialogueEntry pickForStage(CorruptionStage stage, ServerPlayer player, CorruptionData data) {
        return pick(poolFor(stage), player, data);
    }

    /** Maps a stage to its dialogue pool name. */
    public static String poolFor(CorruptionStage stage) {
        return switch (stage) {
            case FRIENDLY -> "friendly";
            case UNSETTLING -> "unsettling";
            case PSYCHOLOGICAL -> "psychological";
            case HOSTILE -> "hostile";
            case FINAL -> "final";
        };
    }

    /**
     * Weighted, anti-repeat pick from a named pool, with tokens already substituted.
     *
     * @return a ready-to-speak entry, or null if the pool is empty/missing
     */
    @Nullable
    public DialogueEntry pick(String poolName, @Nullable ServerPlayer player, @Nullable CorruptionData data) {
        List<DialogueEntry> list = pools.get(poolName);
        if (list == null || list.isEmpty()) return null;

        Deque<String> history = recent.computeIfAbsent(poolName, k -> new ArrayDeque<>());

        // Candidates exclude anything we said recently, unless that would empty the pool.
        List<DialogueEntry> candidates = new ArrayList<>();
        for (DialogueEntry entry : list) {
            if (!history.contains(entry.text())) candidates.add(entry);
        }
        if (candidates.isEmpty()) candidates = list;

        DialogueEntry chosen = weightedRandom(candidates);
        if (chosen == null) return null;

        // Update the anti-repeat ring buffer.
        history.addLast(chosen.text());
        while (history.size() > RECENT_MEMORY) history.removeFirst();

        // Substitute tokens against the player's memory.
        String resolved = substitute(chosen.text(), player, data);
        return new DialogueEntry(resolved, chosen.weight(), chosen.voice(), chosen.emotion());
    }

    @Nullable
    private DialogueEntry weightedRandom(List<DialogueEntry> candidates) {
        int total = 0;
        for (DialogueEntry e : candidates) total += e.weight();
        if (total <= 0) return null;
        int roll = (int) (Math.random() * total);
        for (DialogueEntry e : candidates) {
            roll -= e.weight();
            if (roll < 0) return e;
        }
        return candidates.get(candidates.size() - 1);
    }

    /** Replaces {player}, {encounters}, {structure} tokens. Unknown tokens are left as-is. */
    private String substitute(String text, @Nullable ServerPlayer player, @Nullable CorruptionData data) {
        String out = text;
        if (player != null) {
            out = out.replace("{player}", player.getGameProfile().getName());
            if (data != null) {
                UUID id = player.getUUID();
                CorruptionData.PlayerMemory mem = data.getMemory(id);
                int encounters = mem == null ? 0 : mem.encounters;
                out = out.replace("{encounters}", Integer.toString(encounters));
            }
        }
        return out;
    }
}
