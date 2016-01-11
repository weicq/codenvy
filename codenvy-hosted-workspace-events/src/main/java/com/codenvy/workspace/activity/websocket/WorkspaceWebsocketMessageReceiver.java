/*
 * CODENVY CONFIDENTIAL
 * __________________
 *
 *  [2012] - [2016] Codenvy, S.A.
 *  All Rights Reserved.
 *
 * NOTICE:  All information contained herein is, and remains
 * the property of Codenvy S.A. and its suppliers,
 * if any.  The intellectual and technical concepts contained
 * herein are proprietary to Codenvy S.A.
 * and its suppliers and may be covered by U.S. and Foreign Patents,
 * patents in process, and are protected by trade secret or copyright law.
 * Dissemination of this information or reproduction of this material
 * is strictly forbidden unless prior written permission is obtained
 * from Codenvy S.A..
 */
package com.codenvy.workspace.activity.websocket;

import com.codenvy.workspace.activity.WsActivityEventSender;

import org.everrest.websockets.WSMessageReceiver;
import org.everrest.websockets.message.InputMessage;

/**
 * Handles WebSocket messages and updates tenant last access time.
 */
public class WorkspaceWebsocketMessageReceiver implements WSMessageReceiver {

    private final String                currentWs;
    private final boolean               isTemporary;
    private final WsActivityEventSender wsActivityEventSender;

    public WorkspaceWebsocketMessageReceiver(WsActivityEventSender wsActivityEventSender, String currentWs, boolean isTemporary) {
        this.wsActivityEventSender = wsActivityEventSender;
        this.isTemporary = isTemporary;
        this.currentWs = currentWs;
    }

    @Override
    public void onMessage(InputMessage input) {
        wsActivityEventSender.onActivity(currentWs, isTemporary);
    }

    @Override
    public void onError(Exception error) {
        wsActivityEventSender.onActivity(currentWs, isTemporary);
    }
}
