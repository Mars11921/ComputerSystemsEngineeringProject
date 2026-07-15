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
//   This class encapsulates the state-tracking and validation rules for the multi-wire cutting puzzle module.
public class CutTheWire {

    //   Collections storing the puzzle's constraints (which cuts are required vs. which cuts trigger a failure/explosion).
    private final Set<String> requiredWires;
    private final String forbiddenWire;
    private final Set<String> cutWires;

    private boolean complete;

    //   Constructor initializes rules depending on the game seed (1, 2, or 3) selected during setup.
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
    //   Evaluates cut events. Returns 'false' if a forbidden or irrelevant wire is cut (triggering a loss), and 'true' if the cut is correct.
    public boolean cut(String wireName) {
        if (complete) {
            return true;
        }

        //   Normalizes input strings (e.g., "brown" to "BROWN") to prevent string formatting mismatches from failing the check.
        String normalizedWire =
                wireName.trim().toUpperCase(Locale.ROOT);

        //   Instantly fails validation if the player cuts the wire explicitly labeled as forbidden for this seed.
        if (normalizedWire.equals(forbiddenWire)) {
            return false;
        }

        //   Instantly fails if the player cuts a wire that has no relevance to this seed's task.
        if (!requiredWires.contains(normalizedWire)) {
            return false;
        }

        //   Adds the valid cut to our collection of snipped wires.
        cutWires.add(normalizedWire);

        //   Performs a subset evaluation. If all required wires have been cut, the module is flagged as complete.
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

    //   Helper utility to format live puzzle progress (e.g., "1/2") for rendering or debug print logs.
    public String getProgressText() {
        return getProgress() + "/" + getRequiredCount();
    }
}
