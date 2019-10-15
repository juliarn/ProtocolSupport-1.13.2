package protocolsupport.protocol.packet.middleimpl.serverbound.play.v_beta;

import io.netty.buffer.ByteBuf;
import protocolsupport.protocol.ConnectionImpl;
import protocolsupport.protocol.packet.middle.serverbound.play.MiddleUseEntity;
import protocolsupport.protocol.types.UsedHand;

public class UseEntity extends MiddleUseEntity {

	public UseEntity(ConnectionImpl connection) {
		super(connection);
		hand = UsedHand.MAIN;
	}

	@Override
	public void readFromClientData(ByteBuf clientdata) {
		clientdata.readInt();
		entityId = clientdata.readInt();
		action = clientdata.readBoolean() ? Action.ATTACK : Action.INTERACT;
	}

}
