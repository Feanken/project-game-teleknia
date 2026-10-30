package com.rpgchatgame.service;
import com.rpgchatgame.model.*;
import java.util.*;

/** All decisions use persisted, one-time flags; reading text never awards resources. */
public final class LivingService {
    public record Conversation(String id,String npc,String label,List<String> requires,List<String> absent,boolean ending,String text){}
    public record FieldEvent(String id,String location,String gate,String title,String text){}
    public static final List<Conversation> DIALOGUES=List.of(WorldContent.read("context-dialogues",Conversation[].class));
    public static final List<FieldEvent> EVENTS=List.of(WorldContent.read("field-events",FieldEvent[].class));
    static {
        Set<String> known=new HashSet<>(WorldContent.EVENTS.keySet());known.addAll(WorldContent.QUESTS.keySet());known.addAll(WorldContent.MONSTERS.keySet());WorldContent.ACTIONS.values().forEach(a->known.addAll(a.flags()));for(String id:List.of("mira","sera","orin"))known.add("bond_"+id);
        Set<String> ids=new HashSet<>();for(var d:DIALOGUES)if(!ids.add(d.id())||!WorldContent.NPCS.containsKey(d.npc())||d.text().isBlank()||d.requires()==null||d.absent()==null||!known.containsAll(d.requires())||!known.containsAll(d.absent()))throw new IllegalStateException("Invalid contextual dialogue");
        ids.clear();for(var e:EVENTS)if(!ids.add(e.id())||!WorldContent.LOCATIONS.containsKey(e.location())||!WorldContent.EVENTS.containsKey(e.gate())&&!WorldContent.QUESTS.containsKey(e.gate())&&!WorldContent.MONSTERS.containsKey(e.gate()))throw new IllegalStateException("Invalid field event");
    }
    public static String objective(String id){return switch(id){case "mira"->"Burned Village";case "sera"->"Frostveil Pass";default->"Emberwood Shrine";};}
    public static String next(GameState s,BondService.Companion c){var w=s.getCampaign();return w.has("journey_end_"+c.id())?"Journey complete":!w.has("recruit_"+c.id())?"Recruit at camp after "+c.gate():!w.has("journey_start_"+c.id())?"Talk at camp to begin":!w.has("journey_field_"+c.id())?"Activate "+c.name()+" and investigate at "+objective(c.id()):BondService.affinity(s,c)<2?"Complete two milestones: "+String.join(", ",c.bonds()):"Return to camp and choose your promise in Story";}
    public static List<Choice> choices(GameState s){
        var w=s.getCampaign();List<Choice> result=new ArrayList<>();if(w.getEnemy()!=null||w.epilogueClosed())return result;
        if(!w.getDialogueNode().isBlank()){
            String npc=WorldContent.DIALOGUES.get(w.getDialogueNode()).npc();
            for(var d:DIALOGUES)if(d.npc().equals(npc)&&w.getFlags().containsAll(d.requires())&&d.absent().stream().noneMatch(w::has)&&(!d.ending()||!w.getEnding().isBlank())&&(!d.id().equals("mira_trust")||WorldService.reputation(s,"camp")>=3))result.add(new Choice(d.label(),"context_"+d.id()));return result;
        }
        String location=WorldContent.location(s.getCurrentLocation()).id();
        for(var e:EVENTS)if(e.location().equals(location)&&w.has(e.gate())&&!w.has("event16_"+e.id())){
            result.add(new Choice("Help / "+e.title(),"event_help_"+e.id()));result.add(new Choice("Remember / "+e.title(),"event_note_"+e.id()));
        }
        var ally=BondService.companion(w.getCompanion());
        if(ally!=null&&w.has("journey_start_"+ally.id())&&!w.has("journey_field_"+ally.id())&&s.getCurrentLocation().equals(objective(ally.id())))result.add(new Choice("Investigate with "+ally.name(),"journey_"+ally.id()));
        if(BondService.camp(s))for(var c:BondService.COMPANIONS)if(w.has("recruit_"+c.id())&&w.has("journey_field_"+c.id())&&!w.has("journey_end_"+c.id())&&BondService.affinity(s,c)>=2){result.add(new Choice(c.name()+": promise a home","promise_home_"+c.id()));result.add(new Choice(c.name()+": promise a watch","promise_watch_"+c.id()));}
        return result;
    }
    public static boolean handle(GameState s,String id,StoryResponse r){
        if(choices(s).stream().noneMatch(c->c.getAction().equals(id)))return false;var w=s.getCampaign();
        if(id.startsWith("context_")){var d=DIALOGUES.stream().filter(x->id.equals("context_"+x.id())).findFirst().orElseThrow();r.setNarration(WorldContent.NPCS.get(d.npc()).name()+":\n\n"+d.text());return true;}
        for(var e:EVENTS)if(id.equals("event_help_"+e.id())||id.equals("event_note_"+e.id())){
            if(!w.mark("event16_"+e.id(),e.title()+": "+(id.startsWith("event_help_")?"Helped a traveler.":"Recorded their story.")))return false;
            boolean help=id.startsWith("event_help_");w.mark("event16_"+(help?"help_":"note_")+e.id(),"");r.setGoldChange(help?4:0);r.setNarration(help?e.text()+"\n\nThe travelers offer four gold in thanks. This moment will not repeat.":"You sit beside the traveler and listen.\n\n"+e.text()+" Their story joins the pages of your journal.");return true;
        }
        if(id.startsWith("journey_")){String ally=id.substring(8);w.mark("journey_field_"+ally,"Investigated "+objective(ally)+" with "+ally+".");r.setNarration(BondService.companion(ally).name()+": "+switch(ally){
                case "mira"->"Here, beneath the soot: the children marked their heights on this doorframe. We cannot rebuild their old lives, but we can give them somewhere to grow.\n\nBring this memory back to camp. I want to decide whether our first promise should be a home or a watch against another fire.";
                case "sera"->"See these tracks? The drivers followed the beacon until this bend, then lost sight of it. A light is not enough without a road people can trust.\n\nLet us return to camp and decide: build shelters along the pass, or organize a watch to guide travelers through it.";
                default->"The stone beneath the altar says shelter, not obedience. Someone wore that word smooth by touching it before a journey.\n\nAt camp, let us choose how to live this oath: a refuge open to everyone, or a watch sworn to protect the road.";
            });return true;}
        if(id.startsWith("promise_")){String ally=id.substring(id.lastIndexOf('_')+1);w.mark("journey_end_"+ally,"Companion journey concluded: "+id);w.mark(id,"");if(!w.has("bond_"+ally))BondService.manage(s,"bond",ally);r.setNarration(BondService.companion(ally).name()+": "+(id.startsWith("promise_home_")?"Then let our work become a home, open to anyone who has lost theirs.":"Then let us keep watch, so nobody must face the dark alone.")+"\n\nOur promise lives in the choices we make tomorrow. Your companion's support remains strengthened; no duplicate rewards were granted.");return true;}
        return false;
    }
    public static void comment(GameState s,String action,StoryResponse r){
        var w=s.getCampaign();if(!action.startsWith("go_")||w.getCompanion().isBlank())return;
        var c=BondService.companion(w.getCompanion());String key="remark16_"+c.id()+"_"+WorldContent.location(s.getCurrentLocation()).id();
        if(s.getCurrentLocation().equals(objective(c.id()))&&w.mark(key,"A companion shared a memory."))r.setNarration(r.getNarration()+"\n\n"+c.name()+": "+c.greeting());
    }
    public static String questNext(GameState s,String id){
        var w=s.getCampaign();if(w.has(id))return "Complete";
        if(Set.of("guardian","q_signal","q_caravan","q_oath").contains(id)&&!w.has("wolf"))return "Locked: resolve the Ash Wolf at Northern Gate first.";
        if(Set.of("archive","locket_returned","warden","q_medicine","q_chart","q_captain").contains(id)&&!w.has("guardian"))return "Locked: resolve the guardian at Emberwood Shrine first.";
        return switch(id){
            case "survivors"->"Burned Village: rescue the people beneath the fallen beam.";
            case "wolf"->"Northern Gate: confront the Ash Wolf.";
            case "guardian"->"Emberwood Shrine: challenge the Hollow Guardian.";
            case "sigil"->!w.has("guardian")?"Locked: defeat the shrine guardian.":!w.has("runes")?"Read the shrine runes first.":"Restore the Sun Sigil at Emberwood Shrine.";
            case "archive"->"Read the keeper's archive in Sunken Ruins.";
            case "locket_returned"->ItemCatalog.owns(s,"silver_locket")?"Return the silver locket to Mira at camp.":"Search the flooded quarters in Sunken Ruins.";
            case "warden"->"Cinder Tower: confront the Warden, then choose your ending.";
            case "q_signal"->ItemCatalog.owns(s,"frost_lens")?"Bring the lens to Sera in Frostveil Pass.":"Glacier Sanctum: recover the signal lens.";
            case "q_caravan"->ItemCatalog.owns(s,"caravan_crate")?"Speak to Sera in Frostveil Pass about the crate.":"Search for the missing crate in Frostveil Pass.";
            case "q_oath"->!ItemCatalog.owns(s,"rune_tablet")&&!w.has("oath_read")?"Find the tablet in Frostveil Pass.":"Visit Orin in Glacier Sanctum; translate the oath, then choose a resolution.";
            case "q_medicine"->!w.has("survivors")?"Rescue Mira before delivering medicine.":ItemCatalog.owns(s,"tide_medicine")?"Deliver the medicine to Mira at Survivor Camp.":"Recover medicine on Drowned Coast.";
            case "q_chart"->ItemCatalog.owns(s,"star_chart")?"Bring the chart to Ilyra at Tidewatch Lighthouse.":"Find the star chart on Drowned Coast.";
            case "q_captain"->!w.has("q_chart")?"Locked: resolve Ilyra's chart quest first.":"Tidewatch Lighthouse: confront the captain, or seek Marin's ledger and the bell for diplomacy.";
            default->"Consult the quest description.";
        };
    }
    public static String campDescription(GameState s){var w=s.getCampaign();return (w.has("survivors")?"Children gather beside newly raised shelters. ":"A quiet fire waits for travelers. ")+(w.has("q_medicine")?"Mira's patients are recovering. ":"")+(w.has("q_signal")?"Scouts bring news from the restored beacon. ":"")+(w.has("q_chart")?"Boats deliver supplies and news from the coast. ":"");}
}
