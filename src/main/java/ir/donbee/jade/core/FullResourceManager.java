/*****************************************************************
 JADE - Java Agent DEvelopment Framework is a framework to develop
 multi-agent systems in compliance with the FIPA specifications.
 Copyright (C) 2000 CSELT S.p.A.

 GNU Lesser General Public License

 This library is free software; you can redistribute it and/or
 modify it under the terms of the GNU Lesser General Public
 License as published by the Free Software Foundation,
 version 2.1 of the License.

 This library is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 Lesser General Public License for more details.

 You should have received a copy of the GNU Lesser General Public
 License along with this library; if not, write to the
 Free Software Foundation, Inc., 59 Temple Place - Suite 330,
 Boston, MA  02111-1307, USA.
 *****************************************************************/

package ir.donbee.jade.core;

//#J2ME_EXCLUDE_FILE

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

class FullResourceManager implements ResourceManager {

    public static final String DISABLE_THREAD_GROUP_INTERRUPT = "jade_core_FullResourceManager_disablethreadgroupinterrupt";
    private static final boolean DEFAULT_DISABLE_THREAD_GROUP_INTERRUPT = false;
    public static final String THREAD_GROUP_INTERRUPT_TIMEOUT = "jade_core_FullResourceManager_threadgroupinterrupttimeout";
    private static final String DEFAULT_THREAD_GROUP_INTERRUPT_TIMEOUT = "5000";
    private static final Map<String, Set<Thread>> THREAD_GROUPS = new ConcurrentHashMap<>();

    private boolean terminating = false;

    private Profile myProfile;
    private boolean disableThreadGroupInterrupt;
    private int threadGroupInterruptTimeout;

    public FullResourceManager() {
        THREAD_GROUPS.put("USER_AGENTS", ConcurrentHashMap.newKeySet());
        THREAD_GROUPS.put("SYSTEM_AGENTS", ConcurrentHashMap.newKeySet());
        THREAD_GROUPS.put("TIME_CRITICAL", ConcurrentHashMap.newKeySet());
    }

    public Thread getThread(int type, String name, Runnable r) {
        Thread t = Thread.ofVirtual().name(name)
                .uncaughtExceptionHandler((th, ex) -> {
                    if (!terminating) ex.printStackTrace();
                }).unstarted(r);
        switch (type) {
            case USER_AGENTS -> THREAD_GROUPS.get("USER_AGENTS").add(t);
            case SYSTEM_AGENTS -> THREAD_GROUPS.get("SYSTEM_AGENTS").add(t);
            case TIME_CRITICAL -> THREAD_GROUPS.get("TIME_CRITICAL").add(t);
            default -> throw new IllegalStateException("Unexpected value: " + type);
        }
        return t;
    }

    public void releaseResources() {
        terminating = true;

        if (!disableThreadGroupInterrupt) {
            Thread.ofVirtual().start(() -> {
                try {
                    Thread.sleep(threadGroupInterruptTimeout);
                } catch (InterruptedException ignored) {
                }

                THREAD_GROUPS.values().forEach(group -> group.forEach(Thread::interrupt));
                THREAD_GROUPS.clear();
            });
        }
    }

    public void initialize(Profile p) {
        myProfile = p;

        disableThreadGroupInterrupt = myProfile.getBooleanProperty(DISABLE_THREAD_GROUP_INTERRUPT, DEFAULT_DISABLE_THREAD_GROUP_INTERRUPT);

        String tmp = myProfile.getParameter(THREAD_GROUP_INTERRUPT_TIMEOUT, DEFAULT_THREAD_GROUP_INTERRUPT_TIMEOUT);
        threadGroupInterruptTimeout = Integer.parseInt(tmp);

        if (!myProfile.getBooleanProperty(Profile.NO_DISPLAY, false)) {
            // Start the AWT-Toolkit outside the JADE Thread Group to avoid annoying InterruptedException-s on termination
            // when some agent with a Swing or AWT based GUI is used
            try {
                Class.forName("java.awt.Frame").newInstance();
            } catch (Throwable t) {
                // Ignore failure (e.g. in case we don't have the display)
            }
        }
    }
}
