package com.fourspaces.featherdb.utils;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Lock class... Should allow the programmer to obtain a named lock (only by string or filename).  They can then operate on this named entity
 * for as long as they want.  After which, they can release the lock, and allow another section of code to obtain the lock.  This is good for
 * intra JVM locks on "operations", not objects... for example, if you want to lock on a file, new File(filename) will give you different objects,
 * so you can't operate synchronized.
 * <p>
 * You can also specify a timeout for the lock, after which time it will expire and release itself.  This is helpful for avoiding dead-locks.
 * <p>
 * For locks on file, it will create a absolute/file/name.lock file that can be used to lock resources outside of the JVM. 
 * (it is removed upon release).
 * <p>
 * Note: this class doesn't enforce locks, it just ensures that concurrent threads don't try to access the same named (string id) resource
 * at the same time.
 * 
 * @author mbreese
 *
 */

// TODO: Make this able to have shared (read-only) locks
final public class Lock {
	private Logger log = Logger.get(Lock.class);
	private static Map<String,Queue<Lock>> locks = new HashMap<String,Queue<Lock>> ();
	private static Map<String,Lock> currentLock = new HashMap<String,Lock> ();
	private static Boolean processing = false;
	
	final private String key;
	final private long timeout;
	
	private boolean released = false;
	private boolean expired = false;
	
	private File lockFile = null;
	
	private Lock(String s) {
		this.key = s;
		addLock(this);
		this.timeout = -1;
	}
	
	private Lock(File f) {
		this.key = f.getAbsolutePath();
		addLock(this);
		this.timeout = -1;
		createLockFile();
	}

	private Lock(String s, int timeoutInterval) {
		this.key = s;
		addLock(this);
		this.timeout = new Date().getTime() + timeoutInterval;
		startWatchdog();
	}
	
	private Lock(File f, int timeoutInterval) {
		this.key = f.getAbsolutePath();
		addLock(this);
		this.timeout = new Date().getTime() + timeoutInterval;
		
		createLockFile();
		startWatchdog();
	}

	private void createLockFile() {
		lockFile = new File(key+".lock");
		int timeout = 500; // wait upto 5 seconds for lock file.
		while (this.lockFile.exists() && timeout > 0) {
			try {
				Thread.sleep(10);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			timeout--;
		}
		try {
			this.lockFile.createNewFile();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private void startWatchdog() {
		Thread t = new Thread(new Runnable() {
			public void run() {
				while (new Date().getTime() > timeout && !Thread.interrupted() && !released && !expired) {
					try {
						Thread.sleep(10);
					} catch (InterruptedException e) {
						
					}
				}
				if (!released) {
					expire();
				}
			}
		});
		t.setDaemon(true);
		t.run();
	}
	
	/**
	 * Releases the lock...
	 *
	 */
	public void release() {
		if (!released) {
			log.debug("Lock on '{}' released ({})" , key,this);
			released=true;
			currentLock.remove(key);
			if (lockFile!=null && lockFile.exists()) {
				lockFile.delete();
			}
		}
	}
	
	private void expire() {
		if (!expired && !released && timeout > 0) {
			log.warn("Lock on '{}' expired at {} ({})" , key,timeout,this);
			expired=true;
			release();
		}
	}
	
	/**
	 * Has this lock expired?
	 * @return
	 */
	public boolean isExpired() {
		return expired;
	}

	/** 
	 * Has this lock been released?
	 * @return
	 */
	public boolean isReleased() {
		return released;
	}

	private void addLock(Lock lock) {
		if (!locks.containsKey(lock.key)) {
			locks.put(lock.key, new ConcurrentLinkedQueue<Lock>());
		}
		Queue<Lock> q = locks.get(lock.key);
		q.add(lock);
		waitForLock(lock);
		log.debug("Lock on '{}' obtained ({})" , key,lock);
	}
	
	private void waitForLock(Lock lock) {
		processQueue();
		log.debug("Waiting for lock on '{}' ({})" , key,lock);
		while (currentLock.containsKey(lock.key) && currentLock.get(lock.key)!=lock && (lock.timeout==-1 || lock.timeout > new Date().getTime())) {
			try {
				Thread.sleep(10);
			} catch (InterruptedException e) {
				return;
			}
		}
	}

	private void processQueue() {
		if (!processing) {
			new Thread(new Runnable() {
				public void run() {
					synchronized(processing) {
						if (!processing) {
							processing = true;
							
							/*
							 * Keep processing so long as there is a queue that still has locks pending.
							 * For each named lock, if there isn't a currentLock, then pull the head from 
							 * the queue and make it the current lock.
							 * 
							 * Remove keys from the lockmap that have empty queues.
							 */
							boolean found = true;
							while (found) {
								found = false;
								List<String> removeList = new ArrayList<String>();
								for (String key: locks.keySet()) {
									if (!currentLock.containsKey(key)) {
										Lock lock = locks.get(key).poll();
										if (lock!=null && (lock.timeout==-1 || lock.timeout > new Date().getTime())) {
											currentLock.put(key, lock);
										}
									}
									if (locks.get(key).isEmpty()) {
										removeList.add(key);
									} else {
										found = true;
									}
								}
								for (String key:removeList) {
									locks.remove(key);
								}
								if (found) {
									try {
										Thread.sleep(10);
									} catch (InterruptedException e) {
										found=false;
									}
								}
							}
							processing = false;
						}
					}
				}
			}).run();
		}
	}
	
	/**
	 * retrieve a named lock
	 * @param s
	 * @return
	 */
	public static Lock lock(String s) {
		return new Lock(s);
	}
	
	/**
	 * retrieve a file lock
	 * @param s
	 * @return
	 */
	public static Lock lock(File f) {
		return new Lock(f);
	}

	/**
	 * retrieve a named lock with a timeout
	 * @param s
	 * @param timeout - the timeout length in milliseconds
	 * @return
	 */
	public static Lock lock(String s, int timeout) {
		return new Lock(s, timeout);
	}
	
	/**
	 * retrieve a file lock with a timeout
	 * @param s
	 * @param timeout - the timeout length in milliseconds
	 * @return
	 */
	public static Lock lock(File f, int timeout) {
		return new Lock(f, timeout);
	}

}
