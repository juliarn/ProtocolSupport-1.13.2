package protocolsupport.protocol.serializer;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.MessageFormat;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import org.bukkit.Bukkit;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufInputStream;
import io.netty.buffer.ByteBufOutputStream;
import io.netty.handler.codec.DecoderException;
import io.netty.handler.codec.EncoderException;
import protocolsupport.api.ProtocolType;
import protocolsupport.api.ProtocolVersion;
import protocolsupport.api.events.ItemStackWriteEvent;
import protocolsupport.protocol.typeremapper.itemstack.ItemStackRemapper;
import protocolsupport.protocol.utils.minecraftdata.MinecraftData;
import protocolsupport.zplatform.ServerPlatform;
import protocolsupport.zplatform.itemstack.ItemStackWrapper;
import protocolsupport.zplatform.itemstack.NBTTagCompoundWrapper;

public class ItemStackSerializer {

	public static ItemStackWrapper readItemStack(ByteBuf from, ProtocolVersion version, String locale) {
		int type = 0;
		if (version == ProtocolVersion.MINECRAFT_PE) {
			type = VarNumberSerializer.readSVarInt(from);
		} else {
			type = from.readShort();
		}
		if (type >= 0) {
			ItemStackWrapper itemstack = ServerPlatform.get().getWrapperFactory().createItemStack(type);
			int amount, data = 0;
			if(version == ProtocolVersion.MINECRAFT_PE) {
				int amountdata = VarNumberSerializer.readSVarInt(from);
				amount = (amountdata & 0x7F);
				data = ((amountdata >> 8) & 0xFFFF);
				itemstack.setTag(readTag(from, version));
				//TODO: Read the rest properly..
				ArraySerializer.readVarIntStringArray(from, version); //TODO: CanPlaceOn PE
				ArraySerializer.readVarIntStringArray(from, version); //TODO: CanDestroy PE
			} else {
				amount = from.readByte();
				data = from.readUnsignedShort();
				itemstack.setTag(readTag(from, version));
			}
			itemstack.setAmount(amount);
			itemstack.setData(data);
			return ItemStackRemapper.remapServerbound(version, locale, itemstack.cloneItemStack());
		}
		return ServerPlatform.get().getWrapperFactory().createNullItemStack();
	}

	public static void writeItemStack(ByteBuf to, ProtocolVersion version, String locale, ItemStackWrapper itemstack, boolean fireEvent) {
		if (itemstack.isNull()) {
			if (version == ProtocolVersion.MINECRAFT_PE) {
				VarNumberSerializer.writeVarInt(to, 0);
			}
			else to.writeShort(-1);
			return;
		}
		ItemStackWrapper remapped = itemstack.cloneItemStack();
		int itemstate = ItemStackRemapper.ITEM_ID_REMAPPING_REGISTRY.getTable(version).getRemap(MinecraftData.getItemStateFromIdAndData(remapped.getTypeId(), remapped.getData()));
		remapped.setTypeId(MinecraftData.getItemIdFromState(itemstate));
		remapped.setData(MinecraftData.getItemDataFromState(itemstate));
		if (fireEvent && (ItemStackWriteEvent.getHandlerList().getRegisteredListeners().length > 0)) {
			ItemStackWriteEvent event = new InternalItemStackWriteEvent(version, locale, itemstack, remapped);
			Bukkit.getPluginManager().callEvent(event);
		}
		remapped = ItemStackRemapper.remapClientbound(version, locale, itemstack.getTypeId(), remapped);
		if (version == ProtocolVersion.MINECRAFT_PE) {
			VarNumberSerializer.writeSVarInt(to, remapped.getTypeId()); //TODO: Remap PE itemstacks...
			VarNumberSerializer.writeSVarInt(to, ((remapped.getData() & 0xFFFF) << 8) | remapped.getAmount());
			writeTag(to, version, remapped.getTag()); //TODO: Remap PE NBT
			VarNumberSerializer.writeVarInt(to, 0); //TODO: CanPlaceOn PE
			VarNumberSerializer.writeVarInt(to, 0); //TODO: CanDestroy PE
		} else {
			to.writeShort(remapped.getTypeId());
			to.writeByte(remapped.getAmount());
			to.writeShort(remapped.getData());
			writeTag(to, version, remapped.getTag());
		}
	}

	public static NBTTagCompoundWrapper readTag(ByteBuf from, ProtocolVersion version) {
		try {
			if (isUsingShortLengthNBT(version)) {
				final short length = from.readShort();
				if (length < 0) {
					return ServerPlatform.get().getWrapperFactory().createNullNBTCompound();
				}
				try (InputStream inputstream = new GZIPInputStream(new ByteBufInputStream(from.readSlice(length)))) {
					return ServerPlatform.get().getWrapperFactory().createNBTCompoundFromStream(inputstream);
				}
			} else if (isUsingDirectOrZeroIfNoneNBT(version)) {
				from.markReaderIndex();
				if (from.readByte() == 0) {
					return ServerPlatform.get().getWrapperFactory().createNullNBTCompound();
				}
				from.resetReaderIndex();
				try (DataInputStream datainputstream = new DataInputStream(new ByteBufInputStream(from))) {
					return ServerPlatform.get().getWrapperFactory().createNBTCompoundFromStream(datainputstream);
				}
			} else {
				throw new IllegalArgumentException(MessageFormat.format("Don't know how to read nbt of version {0}", version));
			}
		} catch (IOException e) {
			throw new DecoderException(e);
		}
	}

	public static void writeTag(ByteBuf to, ProtocolVersion version, NBTTagCompoundWrapper tag) {
		try {
			if (isUsingShortLengthNBT(version)) {
				if (tag.isNull()) {
					to.writeShort(-1);
				} else {
					int writerIndex = to.writerIndex();
					//fake length
					to.writeShort(0);
					//actual nbt
					try (OutputStream outputstream = new GZIPOutputStream(new ByteBufOutputStream(to))) {
						tag.writeToStream(outputstream);
					}
					//now replace fake length with real length
					to.setShort(writerIndex, to.writerIndex() - writerIndex - Short.BYTES);
				}
			} else if (isUsingDirectOrZeroIfNoneNBT(version)) {
				if (tag.isNull()) {
					to.writeByte(0);
				} else {
					try (OutputStream outputstream = new ByteBufOutputStream(to)) {
						tag.writeToStream(outputstream);
					}
				}
			} else if (isUsingPENBT(version)) {
				int writerIndex = to.writerIndex();
				//fake length
				to.writeShortLE(0);
				//actual nbt
				//tag.writeToStream(new PENetworkNBTDataOutputStream(to)); //TODO Remap and write PE NBT.
				//now replace fake length with real length
				to.setShortLE(writerIndex, to.writerIndex() - writerIndex - Short.BYTES);
			} else {
				throw new IllegalArgumentException(MessageFormat.format("Don't know how to write nbt of version {0}", version));
			}
		} catch (Throwable ioexception) {
			throw new EncoderException(ioexception);
		}
	}

	private static final boolean isUsingShortLengthNBT(ProtocolVersion version) {
		return (version.getProtocolType() == ProtocolType.PC) && version.isBeforeOrEq(ProtocolVersion.MINECRAFT_1_7_10);
	}

	private static final boolean isUsingDirectOrZeroIfNoneNBT(ProtocolVersion version) {
		return (version.getProtocolType() == ProtocolType.PC) && version.isAfterOrEq(ProtocolVersion.MINECRAFT_1_8);
	}

	private static final boolean isUsingPENBT(ProtocolVersion version) {
		return (version.getProtocolType() == ProtocolType.PE) && (version == ProtocolVersion.MINECRAFT_PE);
	}

	public static class InternalItemStackWriteEvent extends ItemStackWriteEvent {

		private final org.bukkit.inventory.ItemStack wrapped;
		public InternalItemStackWriteEvent(ProtocolVersion version, String locale, ItemStackWrapper original, ItemStackWrapper itemstack) {
			super(version, locale, original.asBukkitMirror());
			this.wrapped = itemstack.asBukkitMirror();
		}

		@Override
		public org.bukkit.inventory.ItemStack getResult() {
			return wrapped;
		}

	}

}
