package com.watchtogether.autonomic.selforg.red5.manager.group;

import org.jgroups.Message;
import org.jgroups.Receiver;
import org.jgroups.ReceiverAdapter;

import com.watchtogether.common.StringMessages;

public class GroupReceiverAdapter extends ReceiverAdapter {

	private GroupManager groupManager;

	@Override
	public void receive(Message msg) {
		Object message = msg.getObject();
		
		if (message instanceof String && message.equals(StringMessages.JOIN_MESSAGE)) {
			
		}
		
		//pass messages to other receivers
		for (Receiver rec:groupManager.getReceivers()) {
			rec.receive(msg);
		}
	}

	public void setGroupManager(GroupManager groupManager) {
		this.groupManager = groupManager;
	}

}
