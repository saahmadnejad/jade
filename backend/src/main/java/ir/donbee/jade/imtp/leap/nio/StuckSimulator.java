package ir.donbee.jade.imtp.leap.nio;

//#J2ME_EXCLUDE_FILE

// Class used for debugging purpose only
class StuckSimulator {
	
	private static Object lock = new Object();
	
	static void init() {
		Thread.ofVirtual().start(() -> {
			synchronized (lock) {
				System.err.println("LOCK acquired");
				try {
					while (true) {
						Thread.sleep(10000);
					}
				}
				catch (Exception e) {
					e.printStackTrace();
				}
			}
			System.err.println("LOCK released");
		});
	}
	
	static void stuck() {
		System.err.println("Thread "+Thread.currentThread().getName()+" STUCK!!!!");
		synchronized (lock) {
			System.err.println("THIS IS IMPOSSIBLE!!!!!!!!!!!!");
		}
	}

}
