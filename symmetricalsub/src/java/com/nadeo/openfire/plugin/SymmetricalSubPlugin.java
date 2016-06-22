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
import org.jivesoftware.openfire.roster.RosterManager;
import org.jivesoftware.openfire.roster.Roster;
import org.jivesoftware.openfire.roster.RosterItem;
import org.jivesoftware.openfire.user.UserNotFoundException;
import org.jivesoftware.openfire.session.Session;
import org.jivesoftware.util.JiveGlobals;
import org.xmpp.packet.JID;
import org.xmpp.packet.Packet;
import org.xmpp.packet.Presence;
import org.xmpp.packet.Presence.Type;
import org.jivesoftware.util.Log;

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
    private RosterManager rosterManager;
    
    public SymmetricalSubPlugin() {
        
        XMPPServer server = XMPPServer.getInstance();
        router = server.getPresenceRouter();
        serverName = server.getServerInfo().getXMPPDomain();
        rosterManager = server.getRosterManager();
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

            if ((packet instanceof Presence) && !processed) {
                Presence presencePacket = (Presence) packet;

                Type presenceType = presencePacket.getType();
                if (presenceType == null ) 
                    return;
                
                JID toJID = presencePacket.getTo();
                JID fromJID = presencePacket.getFrom();
 
                if (mode.equals(LOCAL)) {
                    String toDomain = toJID.getDomain();
                    String fromDomain = fromJID.getDomain();

                    if (!toDomain.equals(serverName) || !fromDomain.equals(serverName)) {
                        return;
                    }
                } 
                  
                if(incoming) {
                    if(presenceType.equals(Presence.Type.subscribe)) {
                        
                        RosterItem rosterItem = null;
                        try {
                            String toNode = toJID.getNode();
                            Roster roster = rosterManager.getRoster(toNode);
                            rosterItem = roster.getRosterItem(fromJID);
                        } catch( UserNotFoundException e) {
                            // user or contact not found, it's ok, just do nothing
                        }
                        
                        boolean userIsSuscribedOrPending = 
                            (rosterItem != null) &&
                            (   rosterItem.getSubStatus() == RosterItem.SUB_BOTH ||    // Should not happen (we're already subscribed, so why ask again ?)
                                rosterItem.getSubStatus() == RosterItem.SUB_TO ||      // User is subscribed to contact
                                rosterItem.getAskStatus() == RosterItem.ASK_SUBSCRIBE  // User has asked subscription to contact
                            );
                        if(userIsSuscribedOrPending) {
                            // Case 2 in readme
                            // user is suscribed (or pending) to this contact, and contact has sent a sub request : accept automatically
                            Log.info("SymmetricalSub #2 : " + toJID.toBareJID() + " has recieved an subscribe request from " + fromJID.toBareJID() + " and is subscribed : accept automatically" );
                            Presence presence = new Presence();
                            presence.setType(Presence.Type.subscribed);
                            presence.setTo(fromJID);
                            presence.setFrom(toJID);
                            router.route(presence);
                            throw new PacketRejectedException();
                        }
                    } else if (presenceType.equals(Presence.Type.unsubscribe)) {
                        // Case 3 in readme
                        // User has recieved an unsubscribe request : accept automatically + send unsubscribe back
                        Log.info("SymmetricalSub #3 : " + toJID.toBareJID() + " has recieved an unsubscribe request from " + fromJID.toBareJID() + " : accept automatically + send unsubscribe back" );
                        Presence presence2 = new Presence();
                        presence2.setType(Presence.Type.unsubscribe);
                        presence2.setTo(fromJID);
                        presence2.setFrom(toJID);
                        router.route(presence2);
                        
                        Presence presence1 = new Presence();
                        presence1.setType(Presence.Type.unsubscribed);
                        presence1.setTo(fromJID);
                        presence1.setFrom(toJID);
                        router.route(presence1);
                        
                        throw new PacketRejectedException();
                    }
                } else {
                    if ( presenceType.equals(Presence.Type.subscribed) ) {
                        // Case 1 in readme 
                        // User accepts a sub request : Also send an "opposite" sub request 
                        Log.info("SymmetricalSub #1 : " + fromJID.toBareJID() + " has accepted an subscribe request from " + toJID.toBareJID() + " : send an opposite sub request" );
                        Presence presence = new Presence();
                        presence.setType(Presence.Type.subscribe);
                        presence.setTo(fromJID);
                        presence.setFrom(toJID);
                        router.route(presence);
                    }
                }
            }
        }
    }
}
