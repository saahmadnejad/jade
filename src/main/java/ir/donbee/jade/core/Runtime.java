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


import ir.donbee.jade.util.leap.LinkedList;
import ir.donbee.jade.util.Logger;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
   The singleton instance (accessible through the 
   <code>instance()</code> static method) of this class allows
   controlling the JADE runtime system from an external application. 
   Two different modalities of controlling the JADE runtime system exist:
   <ul>
   <li> Multiple-container: Several containers (belonging to the same
   platform) can be executed in the local JVM. This modality is 
   activated by means of the <code>createAgentContainer()</code>
   and <code>createMainContainer()</code> methods plus the classes 
   included in the <code>ir.donbee.jade.wrapper</code> package.</li>
   <li> Single-container: Only one container can be executed in the
   local JVM. This modality is activated by means of the 
   <code>startUp()</code> and <code>shutDown()</code> methods</li>
   </ul>
   Once a modality has been activated (by calling one of the above
   methods) calling one of the methods for the other modality cause
   an <code>IllegalStateException</code> to be thrown.
   <p>
   It should be noted that the Single-container modality only provides
   a limited control of the JADE runtime system (e.g. it does not allow
   creating and killing agents), but is the only one supported both 
   in J2SE, PJAVA/CDC and MIDP when using the LEAP add-on. 

   @author Giovanni Rimassa - Universita' di Parma
   @author Giovanni Caire - TILAB
 */
public class Runtime {
	// JADE runtime execution modes: 
	// MULTIPLE --> Several containers can be activated in a JVM
	private static final int MULTIPLE_MODE = 0;
	// SINGLE --> Only one container can be activated in a JVM
	private static final int SINGLE_MODE = 1;
	// UNKNOWN --> Mode not yet set
	private static final int UNKNOWN_MODE = 2;

	private static Map<String, Set<Thread>> CRITICAL_THREAD_GROUP;
	private static Runtime theInstance;

	static {
		CRITICAL_THREAD_GROUP = new ConcurrentHashMap<>();
		theInstance = new Runtime();
	}

	private String version = "UNKNOWN";
	private String revision = "UNKNOWN";
	private String date = "UNKNOWN";
	
	private int activeContainers = 0;
	private LinkedList terminators = new LinkedList();
	private AgentContainerImpl theContainer = null;
	private int mode = UNKNOWN_MODE;

	private Logger myLogger = Logger.getMyLogger(getClass().getName());
	
	// Private constructor to forbid instantiation outside the class.
	private Runtime() {
		//#MIDP_EXCLUDE_BEGIN
		//#DOTNET_EXCLUDE_BEGIN
		VersionManager vm = new VersionManager();
		version = vm.getVersion();
		revision = vm.getRevision();
		date = vm.getDate();
		CRITICAL_THREAD_GROUP.put("CRITICAL_THREADS", ConcurrentHashMap.newKeySet());
		//#DOTNET_EXCLUDE_END
		//#MIDP_EXCLUDE_END
	}

	/**
	 * This method returns the singleton instance of this class
	 * that should be then used to create agent containers.
	 **/
	public static Runtime instance() {
		return theInstance;
	}

	//#MIDP_EXCLUDE_BEGIN
	/**
     Creates a new agent container in the current JVM, providing
     access through a proxy object.  
    <br>
    <b>NOT available in MIDP</b>
    <br>
     @param p the profile containing boostrap and configuration
     data for this container
     @return A proxy object, through which services can be requested
     from the real JADE container.
     @exception IllegalStateException if the Single-container modality
     was previously activated by calling the <code>startUp()</code> 
     method.
	 */
	public ir.donbee.jade.wrapper.AgentContainer createAgentContainer(Profile p) {
		if (mode == UNKNOWN_MODE || mode == MULTIPLE_MODE) {
			mode = MULTIPLE_MODE;
			p.setParameter(Profile.MAIN, "false"); // set to an agent container
			AgentContainerImpl impl = new AgentContainerImpl(p);
			beginContainer();
			if (impl.joinPlatform()) {
				return impl.getContainerController();
			}
			else {
				return null;
			}
		}
		else {
			throw new IllegalStateException("Single-container modality already activated");
		}
	}

	/**
     Creates a new main container in the current JVM, providing
     access through a proxy object.
     <br>
     <b>NOT available in MIDP</b>
     <br>
     @param p the profile containing boostrap and configuration
     data for this container
     @return A proxy object, through which services can be requested
     from the real JADE main container.
     @exception IllegalStateException if the Single-container modality
     was previously activated by calling the <code>startUp()</code> 
     method.
	 */
	public ir.donbee.jade.wrapper.AgentContainer createMainContainer(Profile p) {
		if (mode == UNKNOWN_MODE || mode == MULTIPLE_MODE) {
			mode = MULTIPLE_MODE;
			p.setParameter(Profile.MAIN, "true"); // set to a main container
			AgentContainerImpl impl = new AgentContainerImpl(p);
			beginContainer();
			if (impl.joinPlatform()) {
				return impl.getContainerController();
			}
			else {
				return null;
			}
		}
		else {
			throw new IllegalStateException("Single-container modality already activated");
		}
	}

	/**
     Causes the local JVM to be closed when the last container in this
     JVM terminates.
     <br>
     <b>NOT available in MIDP</b>
     <br>
	 */
	public void setCloseVM(boolean flag) {
		if (flag) {
			terminators.addLast(new Runnable() {
				public void run() {
					// Give one more chance to other threads to complete
					Thread.yield();
					myLogger.log(Logger.INFO, "JADE is closing down now.");
					System.exit(0);
				}
			} );
		}
	}
	//#MIDP_EXCLUDE_END


	/**
     Starts a JADE container in the Single-container modality. 
     Successive calls to this method will take no effect.
     @param p the profile containing boostrap and configuration
     data for this container
     @exception IllegalStateException if the Multiple-container modality
     was previously activated by calling the <code>createAgentContainer()</code>
     or <code>createMainContainer()</code> methods.
	 */
	public void startUp(Profile p) {    
		if (mode == MULTIPLE_MODE) {
			throw new IllegalStateException("Multiple-container modality already activated");
		}
		if (mode == UNKNOWN_MODE) {
			mode = SINGLE_MODE;
			theContainer = new AgentContainerImpl(p);
			beginContainer();
			theContainer.joinPlatform();
		}
	}

	/**
     Stops the JADE container running in the Single-container modality. 
	 */
	public void shutDown() { 
		if (theContainer != null) {
			theContainer.shutDown();
		}
	}

	/**
     Allows setting a <code>Runnable</code> that is executed when
     the last container in this JVM terminates.
	 */
	public void invokeOnTermination(Runnable r) {
		terminators.addFirst(r);
	}

	//#APIDOC_EXCLUDE_BEGIN
	/** 
	 * Reset the list of <code>Runnable</code> objects to be executed on JADE termination
	 * @see invokeOnTermination()
	 */
	public void resetTerminators() {
		terminators.clear();
	}
	//#APIDOC_EXCLUDE_END
	
	// Called by a starting up container.
	void beginContainer() {
		myLogger.log(Logger.INFO, "----------------------------------\n"+getCopyrightNotice()+"----------------------------------------");
		if(activeContainers == 0) {
			// Initialize and start up the timer dispatcher
			TimerDispatcher theDispatcher = TimerDispatcher.getTimerDispatcher();

			//#MIDP_EXCLUDE_BEGIN
			// Set up group and attributes for time critical threads
			Thread t = Thread.ofVirtual().name("JADE Timer dispatcher").unstarted(theDispatcher);
			CRITICAL_THREAD_GROUP.get("CRITICAL_THREADS").add(t);
			//#MIDP_EXCLUDE_END
			/*#MIDP_INCLUDE_BEGIN
			Thread t = Thread.ofVirtual().unstarted(theDispatcher);
			#MIDP_INCLUDE_END*/
			theDispatcher.setThread(t);
			//TimerDispatcher.setTimerDispatcher(theDispatcher);
			theDispatcher.start();
		}
		++activeContainers;
	}

	// Called by a terminating container.
	void endContainer() {
		--activeContainers;
		if(activeContainers == 0) {
			// Start a new Thread that calls all terminators one after 
			// the other
			Thread t = Thread.ofVirtual().unstarted(() -> {
				for (int i = 0; i < terminators.size(); ++i) {
					Runnable r = (Runnable) terminators.get(i);
					r.run();
				}
				// Clear the terminators list at the end
				terminators.clear();
			});
			//#MIDP_EXCLUDE_BEGIN
			//t.setDaemon(false);
			//#MIDP_EXCLUDE_END

			// Terminate the TimerDispatcher and release its resources
			TimerDispatcher.getTimerDispatcher().stop();

			// Reset mode
			mode = UNKNOWN_MODE;
			theContainer = null;

			//#MIDP_EXCLUDE_BEGIN
			try {
				Set<Thread> criticalThreads = CRITICAL_THREAD_GROUP.get("CRITICAL_THREADS");
				if (criticalThreads != null) {
					for (Thread thread : criticalThreads) {
						try {
							thread.interrupt(); // Recommended over stop() or destroy()
						} catch (Exception e) {
							myLogger.log(Logger.WARNING, "Error interrupting thread: " + thread.getName(), e);
						}
					}

					// Optionally wait for threads to finish if needed
					for (Thread thread : criticalThreads) {
						try {
							thread.join(1000); // timeout optional
						} catch (InterruptedException e) {
							Thread.currentThread().interrupt(); // restore interrupt flag
							myLogger.log(Logger.WARNING, "Interrupted while waiting for thread: " + thread.getName(), e);
						}
					}
				}
			} catch (Exception e) {
				myLogger.log(Logger.WARNING, "Time-critical threads still active or error occurred: ", e);
				Set<Thread> criticalThreads = CRITICAL_THREAD_GROUP.get("CRITICAL_THREADS");
				if (criticalThreads != null) {
					criticalThreads.forEach(thread -> System.out.println("Still active: " + thread.getName()));
				}
			} finally {
				CRITICAL_THREAD_GROUP.remove("CRITICAL_THREADS");
			}

			//#MIDP_EXCLUDE_END
			t.start();
		}
	}

	//#APIDOC_EXCLUDE_BEGIN
	public TimerDispatcher getTimerDispatcher() {
		return TimerDispatcher.getTimerDispatcher();
	}
	//#APIDOC_EXCLUDE_END


	//#APIDOC_EXCLUDE_BEGIN
	/**
	 * Return a String with copyright Notice, Name and Version of this version of JADE
	 */
	public static String getCopyrightNotice() {
		return("    This is "+getVersionInfo()+"\n    downloaded in Open Source, under LGPL restrictions,\n    at http://ir.donbee.jade.tilab.com/\n");
	}
	//#APIDOC_EXCLUDE_END

	/**
     Return the version number and date of this JADE Runtime.
	 */
	public static String getVersionInfo() {
		return "JADE "+getVersion() + " - revision "+getRevision()+" of "+getDate();
	}
	
	public static String getVersion() {
		return theInstance.version;
	}
	
	public static String getRevision() {
		return theInstance.revision;
	}
	
	public static String getDate() {
		return theInstance.date;
	}
}

