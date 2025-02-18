package fr.skytasul.quests.structure;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import org.apache.commons.lang.Validate;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

import fr.skytasul.quests.BeautyQuests;
import fr.skytasul.quests.api.QuestsAPI;
import fr.skytasul.quests.players.PlayerAccount;
import fr.skytasul.quests.players.PlayerQuestDatas;
import fr.skytasul.quests.structure.QuestBranch.Source;
import fr.skytasul.quests.utils.Lang;

public class BranchesManager{

	private Map<Integer, QuestBranch> branches = new LinkedHashMap<>();
	
	private Quest quest;
	
	public BranchesManager(Quest quest){
		this.quest = quest;
	}
	
	public Quest getQuest(){
		return quest;
	}
	
	public int getBranchesAmount(){
		return branches.size();
	}
	
	public void addBranch(QuestBranch branch){
		Validate.notNull(branch, "Branch cannot be null !");
		branches.put(branches.size(), branch);
	}
	
	public int getID(QuestBranch branch){
		for (Entry<Integer, QuestBranch> en : branches.entrySet()){
			if (en.getValue() == branch) return en.getKey();
		}
		return 666;
	}
	
	public List<QuestBranch> getBranches() {
		return branches.entrySet().stream().sorted(Comparator.comparingInt(Entry::getKey)).map(Entry::getValue).collect(Collectors.toList());
	}
	
	public QuestBranch getBranch(int id){
		return branches.get(id);
	}
	
	public QuestBranch getPlayerBranch(PlayerAccount acc) {
		if (!acc.hasQuestDatas(quest)) return null;
		return branches.get(acc.getQuestDatas(quest).getBranch());
	}
	
	public boolean hasBranchStarted(PlayerAccount acc, QuestBranch branch){
		if (!acc.hasQuestDatas(quest)) return false;
		return acc.getQuestDatas(quest).getBranch() == branch.getID();
	}
	
	public String getDescriptionLine(PlayerAccount acc, Source source) {
		if (!acc.hasQuestDatas(quest)) throw new IllegalArgumentException("Account do not have quest datas");
		PlayerQuestDatas datas = acc.getQuestDatas(quest);
		if (datas.isInQuestEnd()) return Lang.SCOREBOARD_ASYNC_END.toString();
		return branches.get(datas.getBranch()).getDescriptionLine(acc, source);
	}
	
	/**
	 * Called internally when the quest is updated for the player
	 * @param p Player
	 */
	public final void objectiveUpdated(Player p, PlayerAccount acc) {
		if (quest.hasStarted(acc)) {
			QuestsAPI.propagateQuestsHandlers(x -> x.questUpdated(acc, p, quest));
		}
	}

	public void startPlayer(PlayerAccount acc){
		acc.getQuestDatas(getQuest()).resetQuestFlow();
		branches.get(0).start(acc);
	}
	
	public void remove(PlayerAccount acc) {
		if (!acc.hasQuestDatas(quest)) return;
		QuestBranch branch = getPlayerBranch(acc);
		if (branch != null) branch.remove(acc, true);
	}
	
	public void remove(){
		for (QuestBranch branch : branches.values()){
			branch.remove();
		}
		branches.clear();
	}
	
	public Map<String, Object> serialize(){
		Map<String, Object> map = new LinkedHashMap<>();
		
		List<Map<String, Object>> st = new ArrayList<>();
		for (QuestBranch branch : branches.values()){
			try{
				Map<String, Object> datas = branch.serialize();
				if (datas != null) st.add(datas);
			}catch (Exception ex) {
				BeautyQuests.logger.severe("Error when serializing the branch " + getID(branch) + " for the quest " + quest.getID(), ex);
				BeautyQuests.savingFailure = true;
			}
		}
		map.put("branches", st);
		
		return map;
	}
	
	@Override
	public String toString() {
		return "BranchesManager{branches=" + branches.size() + "}";
	}
	
	public static BranchesManager deserialize(ConfigurationSection config, Quest qu) {
		BranchesManager bm = new BranchesManager(qu);
		
		List<Map<?, ?>> branches = config.getMapList("branches");
		branches.sort((x, y) -> {
			int xid = (Integer) x.get("order");
			int yid = (Integer) y.get("order");
			if (xid < yid) return -1;
			if (xid > yid) return 1;
			BeautyQuests.logger.warning("Two branches with same order in quest " + qu.getID());
			return 0;
		});
		branches.forEach((x) -> bm.addBranch(new QuestBranch(bm)));

		for (int i = 0; i < branches.size(); i++) {
			try{
				if (!bm.getBranch(i).load((Map<String, Object>) branches.get(i))) {
					BeautyQuests.getInstance().getLogger().severe("Error when deserializing the branch " + i + " for the quest " + qu.getID() + " (false return)");
					BeautyQuests.loadingFailure = true;
					return null;
				}
			}catch (Exception ex){
				BeautyQuests.logger.severe("Error when deserializing the branch " + i + " for the quest " + qu.getID(), ex);
				BeautyQuests.loadingFailure = true;
				return null;
			}
		}
		
		return bm;
	}
	
}