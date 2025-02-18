package fr.skytasul.quests.stages;

import java.util.Map;
import java.util.function.Supplier;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.inventory.ComplexRecipe;
import org.bukkit.inventory.CraftingInventory;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import fr.skytasul.quests.QuestsConfiguration;
import fr.skytasul.quests.api.comparison.ItemComparisonMap;
import fr.skytasul.quests.api.stages.AbstractStage;
import fr.skytasul.quests.api.stages.StageCreation;
import fr.skytasul.quests.gui.ItemUtils;
import fr.skytasul.quests.gui.creation.stages.Line;
import fr.skytasul.quests.gui.misc.ItemComparisonGUI;
import fr.skytasul.quests.gui.misc.ItemGUI;
import fr.skytasul.quests.players.PlayerAccount;
import fr.skytasul.quests.players.PlayersManager;
import fr.skytasul.quests.structure.QuestBranch;
import fr.skytasul.quests.structure.QuestBranch.Source;
import fr.skytasul.quests.utils.Lang;
import fr.skytasul.quests.utils.Utils;
import fr.skytasul.quests.utils.XMaterial;

/**
 * @author SkytAsul, ezeiger92, TheBusyBiscuit
 */
public class StageCraft extends AbstractStage {

	private ItemStack result;
	private final ItemComparisonMap comparisons;
	
	public StageCraft(QuestBranch branch, ItemStack result, ItemComparisonMap comparisons) {
		super(branch);
		this.result = result;
		this.comparisons = comparisons;
		if (result.getAmount() == 0) result.setAmount(1);
	}
	
	public ItemStack getItem(){
		return result;
	}

	@EventHandler (priority = EventPriority.MONITOR)
	public void onCraft(CraftItemEvent e){
		Player p = (Player) e.getView().getPlayer();
		PlayerAccount acc = PlayersManager.getPlayerAccount(p);
		ItemStack item = e.getRecipe().getResult();
		if (item.getType() == Material.AIR && e.getRecipe() instanceof ComplexRecipe) {
			String key = ((ComplexRecipe) e.getRecipe()).getKey().toString();
			if (key.equals("minecraft:suspicious_stew")) {
				item = XMaterial.SUSPICIOUS_STEW.parseItem();
			}
		}
		
		if (branch.hasStageLaunched(acc, this) && canUpdate(p)) {
			if (comparisons.isSimilar(result, item)) {
				
				int recipeAmount = item.getAmount();

				switch (e.getClick()) {
					case NUMBER_KEY:
						// If hotbar slot selected is full, crafting fails (vanilla behavior, even when items match)
						if (e.getWhoClicked().getInventory().getItem(e.getHotbarButton()) != null) recipeAmount = 0;
						break;

					case DROP:
					case CONTROL_DROP:
						// If we are holding items, craft-via-drop fails (vanilla behavior)
						ItemStack cursor = e.getCursor();
						if (cursor != null && cursor.getType() != Material.AIR) recipeAmount = 0;
						break;

					case SHIFT_RIGHT:
					case SHIFT_LEFT:
						if (recipeAmount == 0) break;

						int maxCraftable = getMaxCraftAmount(e.getInventory());
						int capacity = fits(item, e.getView().getBottomInventory());

						// If we can't fit everything, increase "space" to include the items dropped by crafting
						// (Think: Uncrafting 8 iron blocks into 1 slot)
						if (capacity < maxCraftable)
							maxCraftable = ((capacity + recipeAmount - 1) / recipeAmount) * recipeAmount;

						recipeAmount = maxCraftable;
						break;
					default:
				}

				// No use continuing if we haven't actually crafted a thing
				if (recipeAmount == 0) return;

				int amount = getPlayerAmount(acc) - recipeAmount;
				if (amount <= 0) {
					finishStage(p);
				}else {
					updateObjective(acc, p, "amount", amount);
				}
			}
		}
	}
	
	@Override
	protected void initPlayerDatas(PlayerAccount acc, Map<String, Object> datas) {
		super.initPlayerDatas(acc, datas);
		datas.put("amount", result.getAmount());
	}

	private int getPlayerAmount(PlayerAccount acc) {
		Integer amount = getData(acc, "amount");
		return amount == null ? 0 : amount.intValue();
	}

	@Override
	protected String descriptionLine(PlayerAccount acc, Source source){
		return Lang.SCOREBOARD_CRAFT.format(Utils.getStringFromNameAndAmount(ItemUtils.getName(result, true), QuestsConfiguration.getItemAmountColor(), getPlayerAmount(acc), result.getAmount(), false));
	}

	@Override
	protected Supplier<Object>[] descriptionFormat(PlayerAccount acc, Source source) {
		return new Supplier[] { () -> Utils.getStringFromNameAndAmount(ItemUtils.getName(result, true), QuestsConfiguration.getItemAmountColor(), getPlayerAmount(acc), result.getAmount(), false) };
	}
	
	@Override
	protected void serialize(Map<String, Object> map){
		map.put("result", result.serialize());
		
		if (!comparisons.getNotDefault().isEmpty()) map.put("itemComparisons", comparisons.getNotDefault());
	}
	
	public static StageCraft deserialize(Map<String, Object> map, QuestBranch branch) {
		return new StageCraft(branch, ItemStack.deserialize((Map<String, Object>) map.get("result")), map.containsKey("itemComparisons") ? new ItemComparisonMap((Map<String, Boolean>) map.get("itemComparisons")) : new ItemComparisonMap());
	}
	
	public static int getMaxCraftAmount(CraftingInventory inv) {
		if (inv.getResult() == null) return 0;

		int resultCount = inv.getResult().getAmount();
		int materialCount = Integer.MAX_VALUE;

		for (ItemStack is : inv.getMatrix())
			if (is != null && is.getAmount() < materialCount)
				materialCount = is.getAmount();

		return resultCount * materialCount;
	}

	public static int fits(ItemStack stack, Inventory inv) {
		int result = 0;

		for (ItemStack is : inv.getContents())
			if (is == null)
				result += stack.getMaxStackSize();
			else if (is.isSimilar(stack))
				result += Math.max(stack.getMaxStackSize() - is.getAmount(), 0);

		return result;
	}

	public static class Creator extends StageCreation<StageCraft> {
		
		private static final int ITEM_SLOT = 6, COMPARISONS_SLOT = 7;
		
		private ItemStack item;
		private ItemComparisonMap comparisons = new ItemComparisonMap();
		
		public Creator(Line line, boolean ending) {
			super(line, ending);
			
			line.setItem(ITEM_SLOT, ItemUtils.item(XMaterial.CHEST, Lang.editItem.toString()), (p, item) -> {
				new ItemGUI((is) -> {
					setItem(is);
					reopenGUI(p, true);
				}, () -> reopenGUI(p, true)).create(p);
			});
			line.setItem(COMPARISONS_SLOT, ItemUtils.item(XMaterial.PRISMARINE_SHARD, Lang.stageItemsComparison.toString()), (p, item) -> {
				new ItemComparisonGUI(comparisons, () -> {
					setComparisons(comparisons);
					reopenGUI(p, true);
				}).create(p);
			});
		}

		public void setItem(ItemStack item) {
			this.item = item;
			line.editItem(ITEM_SLOT, ItemUtils.lore(line.getItem(ITEM_SLOT), Lang.optionValue.format(Utils.getStringFromItemStack(item, "§8", true))));
		}
		
		public void setComparisons(ItemComparisonMap comparisons) {
			this.comparisons = comparisons;
			line.editItem(COMPARISONS_SLOT, ItemUtils.lore(line.getItem(COMPARISONS_SLOT), Lang.optionValue.format(this.comparisons.getEffective().size() + " comparison(s)")));
		}

		@Override
		public void start(Player p) {
			super.start(p);
			new ItemGUI(is -> {
				setItem(is);
				reopenGUI(p, true);
			}, removeAndReopen(p, true)).create(p);
		}

		@Override
		public void edit(StageCraft stage) {
			super.edit(stage);
			setItem(stage.getItem());
			setComparisons(stage.comparisons.clone());
		}
		
		@Override
		public StageCraft finishStage(QuestBranch branch) {
			return new StageCraft(branch, item, comparisons);
		}
	}

}
