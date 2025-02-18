package fr.skytasul.quests.utils.types;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.bukkit.entity.Player;

import fr.skytasul.quests.QuestsConfiguration;
import fr.skytasul.quests.utils.Lang;
import fr.skytasul.quests.utils.Utils;

import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;

public class Message implements Cloneable {
	public String text;
	public Sender sender;
	public String sound;
	public int wait = -1;
	
	public Message(String msg, Sender sender){
		this.text = msg;
		this.sender = sender;
	}
	
	public int getWaitTime() {
		return wait == -1 ? QuestsConfiguration.getDialogsConfig().getDefaultTime() : wait;
	}

	public void sendMessage(Player p, String npc, int id, int size) {
		String sent = formatMessage(p, npc, id, size);
		if (QuestsConfiguration.getDialogsConfig().sendInActionBar()) {
			p.spigot().sendMessage(ChatMessageType.ACTION_BAR, TextComponent.fromLegacyText(sent.replace("{nl}", " ")));
		}else p.sendMessage(StringUtils.splitByWholeSeparator(sent, "{nl}"));
		
		if (!"none".equals(sound)) {
			String sentSound = sound;
			if (sentSound == null) {
				if (sender == Sender.PLAYER) {
					sentSound = QuestsConfiguration.getDialogsConfig().getDefaultPlayerSound();
				}else if (sender == Sender.NPC) {
					sentSound = QuestsConfiguration.getDialogsConfig().getDefaultNPCSound();
				}
			}
			if (sentSound != null) p.playSound(p.getLocation(), sentSound, 1, 1);
		}
	}

	public String formatMessage(Player p, String npc, int id, int size) {
		String sent = null;
		switch (sender) {
		case PLAYER:
			sent = Utils.finalFormat(p, Lang.SelfText.format(p.getName(), text, id + 1, size), true);
			break;
		case NPC:
			sent = Utils.finalFormat(p, Lang.NpcText.format(npc, text, id + 1, size), true);
			break;
		case NOSENDER:
			sent = Utils.finalFormat(p, Utils.format(text, id + 1, size), true);
			break;
		}
		return sent;
	}
	
	@Override
	public Message clone() {
		Message clone = new Message(text, sender);
		clone.sound = sound;
		clone.wait = wait;
		return clone;
	}
	
	public Map<String, Object> serialize(){
		Map<String, Object> map = new HashMap<>();
		map.put("text", text);
		map.put("sender", sender.name());
		if (sound != null) map.put("sound", sound);
		if (wait != -1) map.put("wait", wait);
		return map;
	}
	
	public static Message deserialize(Map<String, Object> map){
		Message msg = new Message((String) map.get("text"), Sender.fromString((String) map.get("sender")));
		if (map.containsKey("sound")) msg.sound = (String) map.get("sound");
		if (map.containsKey("wait")) msg.wait = (int) map.get("wait");
		return msg;
	}

	public enum Sender {
		PLAYER, NPC, NOSENDER;
		
		public static Sender fromString(String string){
			if (string.equalsIgnoreCase("NOTHING")) return NOSENDER;
			return valueOf(string.toUpperCase());
		}
	}
}