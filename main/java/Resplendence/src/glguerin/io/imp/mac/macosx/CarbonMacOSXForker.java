/*** Copyright 2002, 2003 by Gregory L. Guerin.** Terms of use:**  - Briefly: OPEN SOURCE under Artistic License -- credit fairly, use freely, alter carefully.**  - Fully: <http://www.amug.org/~glguerin/sw/artistic-license.html>** This file is from the MacBinary Toolkit for Java:**   <http://www.amug.org/~glguerin/sw/#macbinary> */package glguerin.io.imp.mac.macosx;import java.io.*;import glguerin.io.*;/**** CarbonMacOSXForker is a subclass of MacOSXForker that provides FileForker.Watcher support.** It needs an active Carbon event-loop to do this.** Since MacOSXForker needs Mac OS 10.1 or higher, so does this class.**<p>** In order for change-signals to be delivered to Watchers, the current process must be** running a Carbon event-loop.  ** If you're using AWT or Swing on 1.3.1, you'll have a Carbon event-loop.** If you're not using AWT or Swing on 1.3.1, you can start a Carbon event-loop by** executing some code that relies on JDirect-3.  For an example, see the "kluge-hammer"** code of the app.macbinary.test.TestWatcher class.**<p>** If you're running on 1.4.1 on Mac OS X, then I'm pretty sure (at the time I write this) a Carbon event-loop** will be fatal to your app, since 1.4.1 is Cocoa-based.  You should not use this imp, and instead** use MacOSXForker, which does not have Watcher support.  Sorry.**** @author Gregory Guerin*/public class CarbonMacOSXForker  extends MacOSXForker{	/**	** Vanilla constructor.	*/	public	CarbonMacOSXForker()	{  		super();	}	// These static variables could be named identical to those in superclass,	// but I find it clearer to give them different names.	private static int carbType = 0;	private static int carbCreator = 0;	/**	** Set the creator and file types that newly created files will have by default.	** Calling this method on any concrete FileForker instance sets the defaults for	** all concrete instances of the same class.	**<p>	** In this implementation, the built-in defaults are both set to 0,	** which is the most sensible thing to do on Mac OS X.	*/	public void	setDefaultTypes( int defaultFileType, int defaultFileCreator )	{		carbType = defaultFileType;		carbCreator = defaultFileCreator;	}	/**	** Called by makeForkOutputStream() and makeForkRandomRW(),	** or anywhere else a file needs to be created.  NOT called when an existing	** file is merely truncated.	*/	public int	getDefaultFileType()	{  return ( carbType );  }	/**	** Called by makeForkOutputStream() and makeForkRandomRW(),	** or anywhere else a file needs to be created.  NOT called when an existing	** file is merely truncated.	*/	public int	getDefaultFileCreator()	{  return ( carbCreator );  }	/**	** Make a Watcher that watches for any change-events or only specific change-events.	** The target to watch is designated by the current Pathname.	** The target must be an existing accessible directory.	** If it's inaccessible, or is any other kind of file-system object, an IOException is thrown.	**<p>	** A Carbon event-loop must be active in order for Watcher change-events to arrive.	** If you've started the AWT event-thread under Mac OS X Java 1.3.1, then a Carbon	** event-loop is active.  If a Carbon event-loop isn't active, change-events will be	** lost until after an event-loop becomes active.	**	** @exception java.io.IOException	**    is thrown when a Watcher can't be created for the current target.	** @return	**    A Watcher referring to the current target, or null if Watchers are not supported.	**	** @see Watcher	*/	public Watcher	makeWatcher( boolean onlySpecificChanges )	  throws IOException	{		// ## The following assumes that TinWatcher and its underlying FNSubscription callbacks work.		// ## If a Carbon event-loop isn't active, the FNSubscription callbacks are never called,		// ## and the TinWatcher never gets any change-signals.		// TinWatcher's native code, based on FNSubscriptions, can only watch directories.		// If target is not an existing accessible directory, an IOException is thrown.		// Don't finely distinguish among non-existent, inaccessible, and non-directory targets.		if ( ! briefInfo().isDirectory() )			throw new IOException( "Not an accessible directory: " + getPath() );		// Getting here, we need an alias-resolved pathname for TinWatcher.		// Should always be resolveable, since briefInfo() worked.		// If it can't be resolved, an IOException is thrown.		Pathname resolved = makeResolved();		// Create TinWatcher and check it.  If it's stillborn, throw an IOException.		TinWatcher watcher = new TinWatcher( resolved.getPath(), onlySpecificChanges );		if ( watcher.watchedPath() == null )			throw new IOException( "Can't watch: " + getPath() );		return ( watcher );	}}