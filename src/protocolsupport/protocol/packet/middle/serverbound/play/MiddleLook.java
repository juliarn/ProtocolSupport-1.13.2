package protocolsupport.protocol.packet.middle.serverbound.play;

import protocolsupport.protocol.ConnectionImpl;
import protocolsupport.protocol.packet.PacketType;
import protocolsupport.protocol.packet.middle.ServerBoundMiddlePacket;
import protocolsupport.protocol.packet.middleimpl.IPacketData;
import protocolsupport.protocol.packet.middleimpl.ServerBoundPacketData;
import protocolsupport.utils.recyclable.RecyclableCollection;
import protocolsupport.utils.recyclable.RecyclableSingletonList;

public abstract class MiddleLook extends ServerBoundMiddlePacket {

	public MiddleLook(ConnectionImpl connection) {
		super(connection);
	}

	protected float yaw;
	protected float pitch;
	protected boolean onGround;

	@Override
	public RecyclableCollection<? extends IPacketData> toNative() {
		return RecyclableSingletonList.create(create(yaw, pitch, onGround));
	}

	public static ServerBoundPacketData create(float yaw, float pitch, boolean onGround) {
		ServerBoundPacketData creator = ServerBoundPacketData.create(PacketType.SERVERBOUND_PLAY_LOOK);
		creator.writeFloat(yaw);
		creator.writeFloat(pitch);
		creator.writeBoolean(onGround);
		return creator;
	}

}
