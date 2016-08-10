package com.watchtogether.autonomic.selforg.red5.manager.group;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import org.jgroups.JChannel;
import org.jgroups.Message;
import org.jgroups.Receiver;
import org.jgroups.ReceiverAdapter;
import org.jgroups.stack.IpAddress;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.watchtogether.common.ClientPolicyMessage;
import com.watchtogether.common.StringMessages;

public class GroupManager {

    private JChannel managementChannel;

    private static GroupManager instance;
    private List<Receiver> receivers = new ArrayList<>();

    private static final Logger logger = LoggerFactory.getLogger(GroupManager.class);
    private String envHost = System.getenv("red5_ip");
    private String envPort = System.getenv("red5_port");

    public static GroupManager getManager() {
        if (instance == null) {
            GroupReceiverAdapter adapter = new GroupReceiverAdapter();

            instance = new GroupManager(true, true, adapter);
            adapter.setGroupManager(instance);
        }

        return instance;
    }

    private GroupManager(boolean autoJoin, boolean receive, ReceiverAdapter receiver) {
        URL url = this.getClass().getClassLoader().getResource("jgroups_config.xml");

        try {
            managementChannel = new JChannel(url);
            managementChannel.setDiscardOwnMessages(true);
            if (receive)
                managementChannel.setReceiver(receiver);
            managementChannel.connect("red5_management");
            if (autoJoin) {
                managementChannel.send(new Message(null, null, StringMessages.JOIN_MESSAGE));
                logger.trace("Sent join message");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void broadcastMessage(ClientPolicyMessage msg) {
        try {
            managementChannel.send(new Message(null, new IpAddress(envHost + ":" + envPort), msg));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public List<Receiver> getReceivers() {
        return receivers;
    }
}