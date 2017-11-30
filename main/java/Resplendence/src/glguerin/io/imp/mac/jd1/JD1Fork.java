/*** Copyright 1998, 1999, 2001 by Gregory L. Guerin.** Terms of use:**  - Briefly: OPEN SOURCE under Artistic License -- credit fairly, use freely, alter carefully.**  - Fully: <http://www.amug.org/~glguerin/sw/artistic-license.html>** This file is from the MacBinary Toolkit for Java:**   <http://www.amug.org/~glguerin/sw/#macbinary> */package glguerin.io.imp.mac.jd1;import java.io.*;import glguerin.io.*;import glguerin.io.imp.mac.*;// --- Revision History ---// 12Apr99 GLG  factor out as abstract class common to JDirect 1 & 2 implementations// 13Apr99 GLG  doc-comments// 22Jun99 GLG  cover change of forkOpen() to return short/**** A JD1Fork is an implementation of ForkRW that relies on JDirect 1 in MRJ.** It provides the abstract protected methods that call the Mac OS directly.**<p>** No methods in this class are synchronized, even when a thread-safety issue** is known to exist.** If you need thread-safety you must provide it yourself.  ** The most common uses of this class do not involve shared access** by multiple threads, so synchronizing seemed like a time-wasting overkill.** If you disagree, you have the source...**<p>** The Apple reference materials I used to build this class are:**<ul type="disc">**   <li> <i>Inside Macintosh: Files</i><br>**    Chapter 2 describes the high-level functions I used:**     FSClose(), FSRead(), FSWrite(),**    GetEOF(), SetEOF(), GetFPos(), SetFPos().**</ul>**** @author Gregory Guerin*/public final class JD1Fork  extends ForkRW  implements com.apple.NativeObject{	/**	** Only constructor.	*/	public	JD1Fork( boolean forWriting, int forkRefNum, String tag )	{		super( forWriting, forkRefNum, tag );	}	/**	** Return the length of the given refNum's fork, or throw an IOException.	*/	protected int	forkLength( short refNum, long[] length )	{		int osErr = GetEOF( refNum, intRef );		length[ 0 ] = 0xFFFFFFFFL & intRef[ 0 ];		return ( osErr );	}	/**	** Return the current R/W position of the given refNum's fork, or throw an IOException.	*/	protected int	forkAt( short refNum, long[] position )	{		int osErr = GetFPos( refNum, intRef );		position[ 0 ] = 0xFFFFFFFFL & intRef[ 0 ];		return ( osErr );	}	/**	** Seek to the given position in the given refNum's fork, or throw an IOException.	** The position is always relative to the beginning of the file.	*/	protected int	forkSeek( short refNum, long position )	{		if ( position > Integer.MAX_VALUE )			position = Integer.MAX_VALUE;		return ( SetFPos( refNum, (short) 1, (int) position ) );			// posMode 1 = fsFromStart	}	/**	** Set the length of the given refNum's fork, or throw an IOException.	** When extended, the new bytes in the fork may contain arbitrary	** and possibly sensitive data from reused disk blocks.	*/	protected int	forkSetLength( short refNum, long length )	{  		if ( length > Integer.MAX_VALUE )			length = Integer.MAX_VALUE;		return ( SetEOF( refNum, (int) length ) );	}	/**	** Read bytes from the current position in the given refNum's fork,	** for a byte-count given by count[ 0 ], placing the bytes in the buffer	** beginning at offset 0.	** Return the actual byte-count read in count[ 0 ].	** Reading to or past EOF should not throw an IOException, just return	** the actual number of bytes read, which may be zero.	*/	protected int	forkRead( short refNum, byte[] buffer, int requestCount, int[] actualCount )	{		actualCount[ 0 ] = requestCount;		return ( FSRead( refNum, actualCount, buffer ) ); 	}	/**	** Write bytes to the current position in the given refNum's fork,	** for a byte-count given by count[ 0 ], taking the bytes from the buffer	** beginning at offset 0.	*/	protected int	forkWrite( short refNum, byte[] buffer, int requestCount, int[] actualCount )	{  		actualCount[ 0 ] = requestCount;		return ( FSWrite( refNum, actualCount, buffer ) ); 	}	/**	** Close the given refNum.	*/	protected int	forkClose( short refNum )	{  return ( FSClose( refNum ) );  }	// ###  J D I R E C T - 1   F U N C T I O N   B I N D I N G S  ###	/**	 * @param refNum		in C: <CODE>short refNum</CODE>	 * @return				in C: <CODE>OSErr </CODE>	 */	private static native short 	FSClose(short refNum);	/**	 * @param refNum		in C: <CODE>short refNum</CODE>	 * @param count			in C: <CODE>long *count</CODE>	 * @param buffPtr		in C: <CODE>void *buffPtr</CODE>	 * @return				in C: <CODE>OSErr </CODE>	 */	private static native short 	FSRead(short refNum, int [] count, byte [] buffPtr);	/**	 * @param refNum		in C: <CODE>short refNum</CODE>	 * @param count			in C: <CODE>long *count</CODE>	 * @param buffPtr		in C: <CODE>const void *buffPtr</CODE>	 * @return				in C: <CODE>OSErr </CODE>	 */	private static native short 	FSWrite(short refNum, int [] count, byte [] buffPtr);	/**	 * @param refNum		in C: <CODE>short refNum</CODE>	 * @param logEOF		in C: <CODE>long *logEOF</CODE>	 * @return				in C: <CODE>OSErr </CODE>	 */	private static native short 	GetEOF(short refNum, int [] logEOF);	/**	 * @param refNum		in C: <CODE>short refNum</CODE>	 * @param logEOF		in C: <CODE>long logEOF</CODE>	 * @return				in C: <CODE>OSErr </CODE>	 */	private static native short 	SetEOF(short refNum, int logEOF);	/**	 * @param refNum		in C: <CODE>short refNum</CODE>	 * @param filePos		in C: <CODE>long *filePos</CODE>	 * @return				in C: <CODE>OSErr </CODE>	 */	private static native short 	GetFPos(short refNum, int [] filePos);	/**	 * @param refNum		in C: <CODE>short refNum</CODE>	 * @param posMode		in C: <CODE>short posMode</CODE>	 * @param posOff		in C: <CODE>long posOff</CODE>	 * @return				in C: <CODE>OSErr </CODE>	 */	private static native short 	SetFPos(short refNum, short posMode, int posOff);	private static String[] kNativeLibraryNames = { "InterfaceLib" };}