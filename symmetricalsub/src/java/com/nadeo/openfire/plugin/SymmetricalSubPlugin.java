/**
 * Copyright (C) 2005-2008 Nadeo. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.nadeo.openfire.plugin;

import org.jivesoftware.openfire.PresenceRouter;
import org.jivesoftware.openfire.XMPPServer;
import org.jivesoftware.openfire.container.Plugin;
import org.jivesoftware.openfire.container.PluginManager;
import org.jivesoftware.openfire.interceptor.InterceptorManager;
import org.jivesoftware.openfire.interceptor.PacketInterceptor;
import org.jivesoftware.openfire.interceptor.PacketRejectedException;
import org.jivesoftware.openfire.session.Session;
import org.jivesoftware.util.JiveGlobals;
import org.xmpp.packet.JID;
import org.xmpp.packet.Packet;
import org.xmpp.packet.Presence;
import org.xmpp.packet.Presence.Type;

import java.io.File;
import java.util.*;

/**
 * This plugin can be configuured to allow only bidirectional subscriptions. 
 * 
 * @author <a href="mailto:sylvain@nadeo.com">Gugli</a>
 */
public class SymmetricalSubPlugin implements Plugin {
    public static final String DISABLED = "disabled";
    public static final String LOCAL = "local";
    public static final String ALL = "all";

    private static final String SYMMETRICALSUB_MODE = "plugin.symmetricalsub.mode";
    
    private SuscriptionPacketInterceptor interceptor = new SuscriptionPacketInterceptor();

    private PresenceRouter router;
    private String serverName;
    
    public SymmetricalSubPlugin() {
        
        XMPPServer server = XMPPServer.getInstance();
        router = server.getPresenceRouter();
        serverName = server.getServerInfo().getXMPPDomain();
    }

    public void initializePlugin(PluginManager manager, File pluginDirectory) {
        InterceptorManager.getInstance().addInterceptor(interceptor);
    }

    public void destroyPlugin() {
        InterceptorManager.getInstance().removeInterceptor(interceptor);
        interceptor = null;

        router = null;
        serverName = null;
    }

    public void setSymmetricalSubMode(String mode) {
        JiveGlobals.setProperty(SYMMETRICALSUB_MODE, mode);
    }

    public String getSymmetricalSubMode() {
        return JiveGlobals.getProperty(SYMMETRICALSUB_MODE, LOCAL);
    }

    private class SuscriptionPacketInterceptor implements PacketInterceptor {
        public void interceptPacket(Packet packet, Session session, boolean incoming, boolean processed) throws PacketRejectedException {
            String mode = getSymmetricalSubMode();

            if (mode.equals(DISABLED)) {
                return;
            }

            if ((packet instanceof Presence) && !incoming && !processed) {
                Presence presencePacket = (Presence) packet;

                Type presenceType = presencePacket.getType();
                if (presenceType != null && presenceType.equals(Presence.Type.subscribe)) {
                    JID toJID = presencePacket.getTo();
                    JID fromJID = presencePacket.getFrom();
                    String toNode = toJID.getNode();

                    if (mode.equals(LOCAL)) {
                        String toDomain = toJID.getDomain();
                        String fromDomain = fromJID.getDomain();

                        if (!toDomain.equals(serverName) || !fromDomain.equals(serverName)) {
                            return;
                        }
                    } 
                      
                    // Simulate that the target user has accepted the presence subscription request
                    Presence presence = new Presence();
                    presence.setType(Presence.Type.subscribed);

                    presence.setTo(fromJID);
                    presence.setFrom(toJID);
                    router.route(presence);

                    throw new PacketRejectedException();
                }
            }
        }
    }
}
