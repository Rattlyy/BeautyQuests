package fr.skytasul.quests.utils.nms;

import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.lang.Validate;
import org.bukkit.Material;
import org.bukkit.craftbukkit.v1_13_R2.CraftParticle;
import org.bukkit.craftbukkit.v1_13_R2.entity.CraftPlayer;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

import fr.skytasul.quests.utils.ParticleEffect;

import net.minecraft.server.v1_13_R2.*;

import io.netty.buffer.ByteBuf;

public class v1_13_R2 extends NMS{
	
	@Override
	public Object bookPacket(ByteBuf buf){
		return new PacketPlayOutCustomPayload(PacketPlayOutCustomPayload.c, new PacketDataSerializer(buf));
	}

	@Override
	public Object worldParticlePacket(ParticleEffect effect, boolean paramBoolean, float paramFloat1, float paramFloat2,
			float paramFloat3, float paramFloat4, float paramFloat5, float paramFloat6, float paramFloat7, int paramInt,
			Object paramData) {
		return new PacketPlayOutWorldParticles(/*(ParticleParam) ReflectUtils.getFieldValue(Particles.class.getDeclaredField(effect.getFieldName()), null)*/CraftParticle.toNMS(effect.getBukkitParticle(), paramData), paramBoolean, paramFloat1, paramFloat2, paramFloat3, paramFloat4, paramFloat5, paramFloat6, paramFloat7, paramInt);
	}
	
	@Override
	public void sendPacket(Player p, Object packet){
		Validate.isTrue(packet instanceof Packet, "The object specified is not a packet.");
		((CraftPlayer) p).getHandle().playerConnection.sendPacket((Packet<?>) packet);
	}

	@Override
	public double entityNameplateHeight(LivingEntity en){
		return en.getHeight();
	}
	
	public Object getIChatBaseComponent(String text){
		return new ChatComponentText(text);
	}

	public Object getEnumChatFormat(int value){
		return EnumChatFormat.a(value);
	}
	
	@Override
	public List<String> getAvailableBlockProperties(Material material) {
		Block block = IRegistry.BLOCK.get(new MinecraftKey(material.getKey().getKey()));
		BlockStateList<Block, IBlockData> stateList = block.getStates();
		return stateList.d().stream().map(IBlockState::a).collect(Collectors.toList());
	}
	
	@Override
	public List<String> getAvailableBlockTags() {
		return MinecraftServer.getServer().getTagRegistry().a().a().stream().map(MinecraftKey::toString).collect(Collectors.toList());
	}
	
}