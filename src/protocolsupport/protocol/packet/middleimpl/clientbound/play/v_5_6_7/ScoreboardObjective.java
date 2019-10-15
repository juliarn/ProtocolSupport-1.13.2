package protocolsupport.protocol.packet.middleimpl.clientbound.play.v_5_6_7;

import protocolsupport.protocol.ConnectionImpl;
import protocolsupport.protocol.packet.PacketType;
import protocolsupport.protocol.packet.middle.clientbound.play.MiddleScoreboardObjective;
import protocolsupport.protocol.packet.middleimpl.ClientBoundPacketData;
import protocolsupport.protocol.packet.middleimpl.IPacketData;
import protocolsupport.protocol.serializer.MiscSerializer;
import protocolsupport.protocol.serializer.StringSerializer;
import protocolsupport.protocol.typeremapper.legacy.chat.LegacyChat;
import protocolsupport.utils.recyclable.RecyclableCollection;
import protocolsupport.utils.recyclable.RecyclableSingletonList;

public class ScoreboardObjective extends MiddleScoreboardObjective {

	public ScoreboardObjective(ConnectionImpl connection) {
		super(connection);
	}

	@Override
	public RecyclableCollection<? extends IPacketData> toData() {
		ClientBoundPacketData serializer = ClientBoundPacketData.create(PacketType.CLIENTBOUND_PLAY_SCOREBOARD_OBJECTIVE);
		StringSerializer.writeString(serializer, version, name);
		StringSerializer.writeString(
			serializer, version,
			mode == Mode.REMOVE ? "" : LegacyChat.clampLegacyText(value.toLegacyText(cache.getAttributesCache().getLocale()), 32)
		);
		MiscSerializer.writeByteEnum(serializer, mode);
		return RecyclableSingletonList.create(serializer);
	}

}
