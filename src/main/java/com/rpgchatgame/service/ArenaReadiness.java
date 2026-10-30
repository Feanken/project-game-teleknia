package com.rpgchatgame.service;
import com.rpgchatgame.model.GameState;
import java.util.*;

/** Read-only presentation of the exact existing arena entry gates. */
public final class ArenaReadiness {
    public record Requirement(String label,boolean met){}
    public static List<Requirement> requirements(GameState s){var w=s.getCampaign();return List.of(
        new Requirement("Complete the main journey",!w.getEnding().isBlank()),
        new Requirement("Reopen the borderlands",!w.getEnding().isBlank()&&!w.epilogueClosed()),
        new Requirement("Return to Survivor Camp",s.getCurrentLocation().equals("Survivor Camp")),
        new Requirement("Finish the current battle",w.getEnemy()==null),
        new Requirement("Finish the current conversation",w.getDialogueNode().isBlank()),
        new Requirement("Have at least 10 gold",s.getCharacter().getGold()>=10));}
    public static boolean ready(GameState s){return requirements(s).stream().allMatch(Requirement::met);}
    public static String blocked(GameState s){return requirements(s).stream().filter(r->!r.met()).map(Requirement::label).findFirst().orElse("");}
    public static String material(String id){return switch(id){case "ash","crucible"->"ember_shard";case "ice","mirror"->"frost_thread";case "tide","gale"->"tide_pearl";default->throw new IllegalArgumentException("Unknown arena");};}
}
