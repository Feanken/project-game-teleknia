package com.rpgchatgame;
import com.rpgchatgame.service.*;
import com.rpgchatgame.repository.*;
import com.rpgchatgame.model.*;
import com.rpgchatgame.util.JsonUtil;

/** Engagement presentation observes real commits and exact arena rules, never alters progression. */
public final class PresentationChecks {
    static int checks;
    static void check(boolean value,String label){checks++;if(!value)throw new AssertionError(label);}
    static void readiness(GameState s){String before=JsonUtil.toJson(s);boolean engine=BondService.camp(s)&&!s.getCampaign().getEnding().isBlank()&&s.getCharacter().getGold()>=10;check(ArenaReadiness.ready(s)==engine,"Readiness matches authoritative arena gates");check(ArenaReadiness.requirements(s).size()==6,"Six explicit requirements");check(JsonUtil.toJson(s).equals(before),"Preparation preview is read-only");}
    public static void main(String[] args)throws Exception{
        var db=WorldChecks.database();var g=new GameService(db);g.startNewStory();check(g.saveRevision()==1,"New journey commits once");readiness(g.getCurrentState());check(ArenaReadiness.blocked(g.getCurrentState()).equals("Complete the main journey"),"Specific first blocked requirement");
        var s=g.getCurrentState();s.setCurrentLocation("Survivor Camp");readiness(s);s.getCampaign().setEnding("Dawnkeeper");readiness(s);check(ArenaReadiness.blocked(s).equals("Reopen the borderlands"),"Ending alone does not unlock arena");
        s.getCampaign().mark("borderlands_reopened","");readiness(s);check(ArenaReadiness.ready(s),"Ready with ending, reopening, camp and gold");
        s.getCharacter().setGold(9);readiness(s);check(ArenaReadiness.blocked(s).equals("Have at least 10 gold"),"Exact fee required");s.getCharacter().setGold(20);
        s.getCampaign().setEnemy(new Enemy("wolf","Wolf",100,10,2));readiness(s);s.getCampaign().setEnemy(null);s.getCampaign().setDialogueNode("mira");readiness(s);s.getCampaign().setDialogueNode("");s.setCurrentLocation("Northern Gate");readiness(s);
        for(String id:new String[]{"ash","ice","tide"})check(ItemCatalog.entry(ArenaReadiness.material(id))!=null,"Actual material shown for "+id);
        g.startNewStory();long revision=g.saveRevision();String original=JsonUtil.toJson(g.getCurrentState());g.saveSlots();g.saveSlots();check(g.saveRevision()==revision&&JsonUtil.toJson(g.getCurrentState()).equals(original),"Start preview does not load or commit a character");
        g.performAction("look");check(g.saveRevision()==revision+1&&g.lastFeedback().isBlank(),"Read action saves without fabricated reward toast");g.equipItem("rusty_sword");check(g.lastFeedback().startsWith("Equipped"),"Commit exposes actual result for feedback");
        revision=g.saveRevision();g.saveSlot(1);check(g.saveRevision()==revision+1&&g.lastFeedback().equals("Checkpoint saved."),"Manual checkpoint observed after success");
        revision=g.saveRevision();original=JsonUtil.toJson(g.getCurrentState());String feedback=g.lastFeedback();
        try(var connection=db.getConnection();var q=connection.createStatement()){q.executeUpdate("CREATE TRIGGER fail_notice BEFORE UPDATE ON save_slots BEGIN SELECT RAISE(ABORT,'no save'); END");}
        try{g.performAction("search");throw new AssertionError("Commit should fail");}catch(SaveGameRepository.SaveProblem expected){checks++;}
        check(g.saveRevision()==revision&&g.lastFeedback().equals(feedback),"Failed save never reports a new successful revision");check(JsonUtil.toJson(g.getCurrentState()).equals(original),"Failed save keeps active progression intact");
        try(var connection=db.getConnection();var q=connection.createStatement()){q.executeUpdate("DROP TRIGGER fail_notice");}
        g.performAction("search");check(g.saveRevision()==revision+1,"A retry reports success only after commit");
        var art=javax.imageio.ImageIO.read(PresentationChecks.class.getResource("/art/title-campfire.png"));check(art.getWidth()>1000&&art.getHeight()>600,"Title artwork bundled and decodable");
        System.out.println("PASS / "+checks+" presentation checks: exact readiness, readonly previews, commit notices, failed-save integrity and title art.");
    }
}
