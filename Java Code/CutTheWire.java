package bombgame;

import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

/**
 * Contains the complete wire-module logic.
 *
 * Seed 1: cut RED and GREEN; BROWN is forbidden.
 * Seed 2: cut BROWN and GREEN; RED is forbidden.
 * Seed 3: cut RED and BROWN; GREEN is forbidden.
 */
public class CutTheWire {

    private final Set<String> requiredWires;
    private final String forbiddenWire;
    private final Set<String> cutWires;

    private boolean complete;

    public CutTheWire(int seed) {
        this.cutWires = new HashSet<>();
        this.complete = false;

        switch (seed) {
            case 1 -> {
                requiredWires = Set.of("RED", "GREEN");
                forbiddenWire = "BROWN";
            }
            case 2 -> {
                requiredWires = Set.of("BROWN", "GREEN");
                forbiddenWire = "RED";
            }
            case 3 -> {
                requiredWires = Set.of("RED", "BROWN");
                forbiddenWire = "GREEN";
            }
            default -> throw new IllegalArgumentException(
                    "Wire seed must be 1, 2, or 3."
            );
        }
    }

    /**
     * Processes one physical wire-cut event.
     *
     * @return true for a valid cut; false for a forbidden or unknown cut
     */
    public boolean cut(String wireName) {
        if (complete) {
            return true;
        }

        String normalizedWire =
                wireName.trim().toUpperCase(Locale.ROOT);

        if (normalizedWire.equals(forbiddenWire)) {
            return false;
        }

        if (!requiredWires.contains(normalizedWire)) {
            return false;
        }

        cutWires.add(normalizedWire);

        if (cutWires.containsAll(requiredWires)) {
            complete = true;
        }

        return true;
    }

    public boolean isComplete() {
        return complete;
    }

    public int getProgress() {
        return cutWires.size();
    }

    public int getRequiredCount() {
        return requiredWires.size();
    }

    public String getProgressText() {
        return getProgress() + "/" + getRequiredCount();
    }
}
