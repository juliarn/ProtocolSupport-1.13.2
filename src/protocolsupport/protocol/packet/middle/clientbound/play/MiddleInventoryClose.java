package protocolsupport.protocol.packet.middle.clientbound.play;

import io.netty.buffer.ByteBuf;
import protocolsupport.protocol.ConnectionImpl;
import protocolsupport.protocol.packet.middle.ClientBoundMiddlePacket;

public abstract class MiddleInventoryClose extends ClientBoundMiddlePacket {

	public MiddleInventoryClose(ConnectionImpl connection) {
		super(connection);
	}

	protected byte windowId;

	@Override
	public void readFromServerData(ByteBuf serverdata) {
		windowId = serverdata.readByte();
	}

	@Override
	public boolean postFromServerRead() {
		cache.getWindowCache().closeWindow();
		return true;
	}

}
