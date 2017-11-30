/*** Copyright 1998, 1999, 2001 by Gregory L. Guerin.** Terms of use:**  - Briefly: OPEN SOURCE under Artistic License -- credit fairly, use freely, alter carefully.**  - Fully: <http://www.amug.org/~glguerin/sw/artistic-license.html>** This file is from the MacBinary Toolkit for Java:**   <http://www.amug.org/~glguerin/sw/#macbinary> */package glguerin.io.imp.mac.jd2;import java.io.*;import com.apple.mrj.macos.toolbox.Toolbox;import glguerin.io.*;import glguerin.io.imp.mac.*;import glguerin.util.SmallPoint;import glguerin.util.MacTime;// --- Revision History ---// 17Mar99 GLG  first stub version using JDirect2// 25Mar99 GLG  make non-stub version// 26Mar99 GLG  add isAlias()// 29Mar99 GLG  rework, replacing prior methods with setWhere(), setName(), setTarget()// 30Mar99 GLG  add ensureExistence()// 01Apr99 GLG  cover class-name change to MacFiling// 01Apr99 GLG  fix setTargetWhere() to set parentDirID to ioDirID// 05Apr99 GLG  expand doc-comments// 06Apr99 GLG  add support for FXInfo flags-byte// 06Apr99 GLG  add references in doc-comments// 13Apr99 GLG  refactor to implement CatalogAccess// 22Apr99 GLG  cover class-name change to MacEscaping// 26Apr99 GLG  cover some name changes// 29Apr99 GLG  add Toolbox.LOCK code// 14Jun99 GLG  add new get/setFinderIconAt() methods// 21May01 GLG  remove direct escaping support// 05Jun01 GLG  cut getCommentLength()// 12Jun01 GLG  cut file-lock methods// 16Jun01 GLG  remove setLocked()// 24Jun01 GLG  cut ensureExistence()// 16Jan2003 GLG  add hasFinderFlags()/**** A CatalogAccessor is an implementation of CatalogAccess that accesses the Mac's** file-system in order to read and write the FileInfo items, and to set or clear** the file-lock (i.e. the write-prevention flag).** It does not provide access to the comment for its target, but always returns values** making it appear that there is never a comment.** Use CommentAccessor to access comments.** This dichotomy is intentional, since the class that binds together FileInfo and** comments is MRJForker.  Implementing separate accessors keeps things simpler at this** level of behavior.**<p>** The Apple reference materials I used to build this class are:**<ul type="disc">**   <li> <i>Inside Macintosh: Files</i><br>**    Page 2-100 has the CInfoPBRec with descriptions on subsequent pages.**    Page 2-190 thru 2-194 describe the PBHGetInfo and PBHSetInfo calls.**    HCreate(), HSetFLock(), and HRstFLock() are described in Chapter 2, too.**   <li> <i>Inside Macintosh: Macintosh Toolbox Essentials</i><br>**    Page 7-47 describes the FInfo struct, which holds file-type, file-creator, Finder-flags, etc.**    Page 7-49 describes the FXInfo struct, which holds the extended flags and script-code.**</ul>**** @author Gregory Guerin**** @see glguerin.io.FileInfo** @see CommentAccessor** @see MRJForker*/public class JD2Cat  extends JD2Hand  implements CatalogAccess{	/**	** Create an empty instance, also creating an internal handle of proper size.	*/	public	JD2Cat()	{  super( SIZE );  }	/**	** Check the resultCode for success or failure.	*//*	protected void	checkIOErr( int resultCode )	  throws IOException	{  Errors.checkIOError( resultCode, null, this );  }*/	/**	** Override Object.toString(), returning getLeafName().	** The returned String may be zero-length, but should never be null.	*/	public String	toString()	{		if ( false )			return ( getLeafName() );		else		{			StringBuffer build = new StringBuffer();			build.append( "CatInfo[" ).append( getVRefNum() );			build.append( "," ).append( getParentDirID() );			build.append( "," ).append( getLeafName() ).append( "]" );			return ( build.toString() );		}	}	/**	** Set the target information from the given FSSpec's values,	** without affecting any other contents of this CatalogAccess.	** This method calls setTargetWhat() and setTargetName().	** The dirIndex to setTargetWhat() is zero.	*/	public final void	setTarget( FSSpec theSpec )	{		setTargetWhat( theSpec.getVRefNum(), theSpec.getParentDirID(), 0 );		setTargetName( theSpec.getName() );	}	/**	** Set what the context of the assigned name will be by specifying	** a vol-ID, a dir-ID, and a dir-index.	** The name is unaffected, as is all the other non-target information.	** The value of dirIndex determines what fill() will do:	**<ul type="disc">	**   <li>When dirIndex is any negative value, the name is ignored and	**     the resulting leaf-name will be the name of dirID on volRefNum.	**   <li>When dirIndex is zero, the name is interpreted relative to dirID on volRefNum.	**   <li>When dirIndex is greater than zero, the name is ignored and	**     the resulting leaf-name will be the name of the dirIndex'th item within dirID on volRefNum.	**     You can use this to scan a directory's entries, and stop when a FileNotFoundException is thrown.	**</ul>	*/	public final void	setTargetWhat( int volRefNum, int dirID, int dirIndex )	{		setShortAt( VREFNUM_AT, (short) volRefNum );  		setIntAt( DIR_ID_AT, dirID );  		setShortAt( DIR_INDEX_AT, (short) dirIndex );  		// If the target doesn't exist but the directories leading to it do,		// we must ensure that the parent-dir-ID on output correctly reflects the		// input-dir-ID value when any operator-method was called		// and an IOException is thrown.		// That's what this fragment does.  Without it, the parent-dir-ID will just be		// whatever it last was when a fill() succeeded, which will be VERY misleading.		setIntAt( PARENT_DIR_ID_AT, dirID );  	}	/**	** Set the name without affecting any other fields.	** The name is presumed to be in unescaped Mac-native format, 	** i.e. with :'s as path-separators.	** The name may be a relative path-name, referenced to the last values	** set by setTargetWhere().	*/	public final void 	setTargetName( String name )	{		if ( name == null )			setByteAt( STRBUF_AT, (byte) 0 );		else			setBytesAt( STRBUF_AT, NameString.toPStr( name, NameString.LIMIT_PSTR ) );	}	/**	** Return the attribute-flags.	*/	protected final int 	getAttributes()	{  return ( 0x0FF & getByteAt( ATTRIB_AT ) );  }		/**	** Return the VRefNum, i.e. the volume reference number.	*/	public final int 	getVRefNum()	{  return ( getShortAt( VREFNUM_AT ) );  }		/**	** Return the parent DirID, i.e. the DirID where the target is located.	*/	public final int 	getParentDirID()	{  return ( getIntAt( PARENT_DIR_ID_AT ) );  }	/**	** Return the input dirID, i.e. the input-value of where the target is located.	*/	public final int 	getIODirID()	{  return ( getIntAt( DIR_ID_AT ) );  }	// ###  O P E R A T O R S  ###	/**	** Read all-new values from disk using the current target values,	** without resolving any aliases.	** If the target does not exist, a FileNotFoundException is thrown.	** If a directory leading to the target does not exist, a FileNotFoundException is also thrown.	** Other errors throw a generic IOException.	*/	public final void	fill()	  throws IOException	{		try		{  			int pbPtr = lockedPointer();			setIntAt( NAMEPTR_AT, pbPtr + STRBUF_AT );				int osErr;			synchronized ( Toolbox.LOCK )			{  osErr = PBGetCatInfoSync( pbPtr );  }			checkIOErr( osErr ); 		}		finally		{  unlock();  }	}	/**	** Write the current values to disk.  The target must already exist	**<p>	** The isLocked() state has no effect here.  You can't set or clear the file-lock this way.	** You have to call FSSpec.setFileLock() directly.	**	** @see FSSpec#setFileLock	*/	public final void	write()	  throws IOException	{		try		{  			int pbPtr = lockedPointer();			setIntAt( NAMEPTR_AT, pbPtr + STRBUF_AT );				int osErr;			synchronized ( Toolbox.LOCK )			{  osErr = PBSetCatInfoSync( pbPtr );  }			checkIOErr( osErr ); 		}		finally		{  unlock();  }	}	// ###  A S   M A C - C A T A L O G - I N F O  ###	/**	** Clear all the current info, or set to some reasonably "empty" default.	** For example, the file-type and file-creator may be set to OSTYPE_UNKNOWN	** rather than cleared to zero.	**<p>	** This implementation clears the entire handle's contents,	** and sets the internal name to zero-length.	*/	public void 	clear()	{		eraseAll();	}	/**	** Copy all of theInfo into this, or as much as possible.	** If this object can set its internal isDirectory flag, then it should do so.	** Otherwise, lacking a setDirectory() method, the isDirectory()	** state of theInfo is not copied to this object.	*/	public void 	copyFrom( FileInfo theInfo )	{		clear();		if ( theInfo == null )			return;		setFileType( theInfo.getFileType() );		setFileCreator( theInfo.getFileCreator() );		setFinderFlags( theInfo.getFinderFlags() );		setTimeCreated( theInfo.getTimeCreated() );		setTimeModified( theInfo.getTimeModified() );		setFinderIconAt( theInfo.getFinderIconAt() );	}	// ###  A C C E S S O R S  ###	/**	** Return the literal leaf name of the file, absent any path or location information.	*/	public String 	getLeafName()	{		String name = new String( getBytesAt( STRBUF_AT + 1, 0x0FF & getByteAt( STRBUF_AT ) ) );		return ( name );	}	/**	** Return the length of the designated fork.	*/	public long 	getForkLength( boolean resFork )	{  return ( getIntAt( resFork ? LEN_RES_FORK_AT : LEN_DATA_FORK_AT ) );  }	/**	** Return the 32-bit OSType value representing the file-type.	** Though conventionally interpreted as 4 MacRoman characters, this is	** actually an integer value.  If you need to display it as characters, you	** should translate it into UniCode form.	*/	public int 	getFileType()	{  return ( getIntAt( FILE_TYPE_AT ) );  }	/**	** Set the file-type to the given 32-bit OSType value.	*/	public void 	setFileType( int osType )	{  setIntAt( FILE_TYPE_AT, osType );  }	/**	** Return the 32-bit OSType value representing the file-creator.	** Though conventionally interpreted as 4 MacRoman characters, this is	** actually an integer value.  If you need to display it as characters, you	** should translate it into UniCode form.	*/	public int 	getFileCreator()	{  return ( getIntAt( FILE_CREATOR_AT ) );  }	/**	** Set the file-creator to the given 32-bit OSType value.	*/	public void 	setFileCreator( int osType )	{  setIntAt( FILE_CREATOR_AT, osType );  }	/**	** The "usual" Finder flags (FInfo.fdFlags) are in bits 0-15;	** The extended flags (FXInfo.fdXFlags) are in bits 16-23.	** The remainder of the value is reserved, and should be zero for now.	** Unused or reserved flags within each sub-part should also be zero.	*/	public int 	getFinderFlags()	{  		int combined = getShortAt( FINFO_FLAGS_AT ) & 0x0FFFF;		combined |= (getByteAt( FXINFO_FLAGS_AT ) & 0x0FF) << 16;		return ( combined );	}	/**	** Return T only if all the 1-bits in flagsMask are set in this FileInfo's Finder flags.	** Return F otherwise.	*/	public boolean	hasFinderFlags( int flagsMask )	{  return ( (getFinderFlags() & flagsMask) == flagsMask );  }	/**	** Set the Finder flags.	** This implementation separates the combined flags and stores the pieces	** in the appropriate places in the overall struct.	*/	public void 	setFinderFlags( int flags )	{  		setShortAt( FINFO_FLAGS_AT, (short) flags );  		setByteAt( FXINFO_FLAGS_AT, (byte) (flags >> 16) );  	}	/**	** Return true if locked (not writable), false if unlocked (writable).	*/	public boolean 	isLocked()	{  return ( (getAttributes() & 0x01) != 0 );  }		/**	** Return true if this refers to a directory, false if it refers to a file.	** This is a read-only attribute, and has no corresponding setter method.	*/	public boolean 	isDirectory()	{  return ( (getAttributes() & 0x10) != 0 );  }	/**	** Return true if this refers to an alias of some sort, false if not.	** This is intended as a convenience equivalent to:	** <br>&nbsp;&nbsp;&nbsp;	** <code> hasFinderFlags( MASK_FINDER_ISALIAS )</code>	** <br>An implementation is free to expand on this	** by evaluating other criteria it may have access to.	*/	public boolean 	isAlias()	{  return ( hasFinderFlags( MASK_FINDER_ISALIAS ) );  }	/**	** Return the creation-date,	** measured as milliseconds before or after 01 Jan 1970 midnight UTC/GMT.	**	** @see glguerin.util.MacTime	*/	public long 	getTimeCreated()	{  return ( MacTime.macSecsToJavaMillis( getIntAt( WHEN_CREATED_AT ) ) );  }	/**	** Set the date when the file was created,	** measured as milliseconds before or after 01 Jan 1970 midnight UTC/GMT.	**	** @see glguerin.util.MacTime	*/	public void	setTimeCreated( long millis )	{  setIntAt( WHEN_CREATED_AT, (int) MacTime.javaMillisToMacSecs( millis ) );  }	/**	** Return the modification-date, 	** measured as milliseconds before or after 01 Jan 1970 midnight UTC/GMT.	**	** @see glguerin.util.MacTime	*/	public long	getTimeModified()	{  return ( MacTime.macSecsToJavaMillis( getIntAt( WHEN_MODIFIED_AT ) ) );  }	/**	** Set the date when the file was last modified,	** measured as milliseconds before or after 01 Jan 1970 midnight UTC/GMT.	**	** @see glguerin.util.MacTime	*/	public void	setTimeModified( long millis )	{  setIntAt( WHEN_MODIFIED_AT, (int) MacTime.javaMillisToMacSecs( millis ) );  }	/**	** Return a new SmallPoint holding	** the XY location of the item's icon in its Finder window.	*/	public SmallPoint 	getFinderIconAt()	{  return ( new SmallPoint( getShortAt( FINFO_HORZ_AT ), getShortAt( FINFO_VERT_AT ) ) );  }	/**	** Set the XY location of the item's icon in its Finder window.	** A location at 0,0 lets the Finder know that it should position	** the icon automatically.	** Unless you are restoring a backup, you should normally use auto-positioning.	** If you pass a SmallPoint of null, the icon position is set to 0,0.	*/	public void 	setFinderIconAt( SmallPoint atXY )	{		short x = 0, y = 0;		if ( atXY != null )		{  x = atXY.x;  y = atXY.y;  }		setShortAt( FINFO_HORZ_AT, x );  		setShortAt( FINFO_VERT_AT, y );  	}	/**	** Return an empty String.	*/	public String	getComment()	{  return ( "" );  }	// ###  J D I R E C T - 2   B I N D I N G S  ###	/**	 * @param paramBlockPtr	in C: <CODE>CInfoPBPtr paramBlockPtr</CODE>	 * @return				in C: <CODE>OSErr </CODE>	 */	private static native short PBGetCatInfoSync( int paramBlockPtr );	/**	 * @param paramBlockPtr	in C: <CODE>CInfoPBPtr paramBlockPtr</CODE>	 * @return				in C: <CODE>OSErr </CODE>	 */	private static native short PBSetCatInfoSync( int paramBlockPtr );}