package protocolsupport.protocol.packet.middleimpl.clientbound.play.v_7;

import protocolsupport.protocol.ConnectionImpl;
import protocolsupport.protocol.packet.PacketType;
import protocolsupport.protocol.packet.middle.clientbound.play.MiddleSpawnPainting;
import protocolsupport.protocol.packet.middleimpl.ClientBoundPacketData;
import protocolsupport.protocol.packet.middleimpl.IPacketData;
import protocolsupport.protocol.serializer.PositionSerializer;
import protocolsupport.protocol.serializer.StringSerializer;
import protocolsupport.protocol.serializer.VarNumberSerializer;
import protocolsupport.protocol.typeremapper.legacy.LegacyPainting;
import protocolsupport.utils.recyclable.RecyclableCollection;
import protocolsupport.utils.recyclable.RecyclableSingletonList;

public class SpawnPainting extends MiddleSpawnPainting {

	public SpawnPainting(ConnectionImpl connection) {
		super(connection);
	}

	@Override
	public RecyclableCollection<? extends IPacketData> toData() {
		switch (direction) {
			case 0: {
				position.modifyZ(-1);
				break;
			}
			case 1: {
				position.modifyX(1);
				break;
			}
			case 2: {
				position.modifyZ(1);
				break;
			}
			case 3: {
				position.modifyX(-1);
				break;
			}
		}
		ClientBoundPacketData serializer = ClientBoundPacketData.create(PacketType.CLIENTBOUND_PLAY_SPAWN_PAINTING);
		VarNumberSerializer.writeVarInt(serializer, entity.getId());
		StringSerializer.writeVarIntUTF8String(serializer, LegacyPainting.getName(type));
		PositionSerializer.writeLegacyPositionI(serializer, position);
		serializer.writeInt(direction);
		return RecyclableSingletonList.create(serializer);
	}

}
