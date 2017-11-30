/*** Copyright 1998, 1999, 2001 by Gregory L. Guerin.** Terms of use:**  - Briefly: OPEN SOURCE under Artistic License -- credit fairly, use freely, alter carefully.**  - Fully: <http://www.amug.org/~glguerin/sw/artistic-license.html>** This file is from the MacBinary Toolkit for Java:**   <http://www.amug.org/~glguerin/sw/#macbinary> */package glguerin.io.imp.mac;import java.io.*;import glguerin.io.*;// --- Revision History ---// 13Apr99 GLG  factor out of JD2 code-base as abstract interface// 14Jun99 GLG  add FINFO_VERT_AT and FINFO_HORZ_AT offsets// 12Jun01 GLG  cut file-lock methods// 24Jun01 GLG  cut ensureExistence() -- functionality moved to FSSpec.create()/**** CatalogAccess is an extension of FileInfo that provides operations ** to read and write the catalog-info of the Mac's file-system, and to set or clear** the file-lock (i.e. the write-prevention flag).** It does not provide access to the comment for its target, but always returns values** making it appear that there is never a comment.** Use CommentAccess to access comments.** This dichotomy is intentional, since the class that binds together FileInfo and** comments is a FileForker sub-class, i.e. BaseForker, itself an abstract class.  ** Implementing separate accessors keeps things simpler at this level of behavior,** especially when the API's for catalog-info and comments are not very closely related.**<p>** Named constants defined here determine the offsets of the struct-members** referenced by implementations.** The Apple reference materials I used to make this interface are:**<ul type="disc">**   <li> <i>Inside Macintosh: Files</i><br>**    Page 2-100 has the CInfoPBRec with descriptions on subsequent pages.**    Page 2-190 thru 2-194 describe the PBHGetInfo and PBHSetInfo calls.**    HCreate(), HSetFLock(), and HRstFLock() are described in Chapter 2, too.**   <li> <i>Inside Macintosh: Macintosh Toolbox Essentials</i><br>**    Page 7-47 describes the FInfo struct, which holds file-type, file-creator, Finder-flags, etc.**    Page 7-49 describes the FXInfo struct, which holds the extended flags and script-code.**</ul>**** @author Gregory Guerin**** @see glguerin.io.FileInfo** @see CommentAccess** @see BaseForker*/public interface CatalogAccess  extends FileInfo{	/**	** Overall size of the underlying PBCatInfo struct, measured in bytes.	*/	public static final int SIZE = 108 + 256;		// 108 = size of CInfoPBRec		// 256 = size of Str255 buffer	/**	** Offset within struct where the Str255-buffer is located,	** i.e. after the CInfoPBRec located at offset 0.	*/	public static final int STRBUF_AT = 108;	public static final int		NAMEPTR_AT = 18,		VREFNUM_AT = 22,		DIR_INDEX_AT = 28,		ATTRIB_AT = 30,		DIR_ID_AT = 48,		PARENT_DIR_ID_AT = 100;			public static final int		FINFO_AT = 32,	// an FInfo or DInfo embedded struct		FILE_TYPE_AT = FINFO_AT + 0,	// a rect's TL in a DInfo		FILE_CREATOR_AT = FINFO_AT + 4,	// a rect's BR in a DInfo		FINFO_FLAGS_AT = FINFO_AT + 8,		FINFO_VERT_AT = FINFO_AT + 10,		FINFO_HORZ_AT = FINFO_AT + 12,		LEN_DATA_FORK_AT = 54,		LEN_RES_FORK_AT = 64,		WHEN_CREATED_AT = 72,		WHEN_MODIFIED_AT = 76,		FXINFO_AT = 84,	// an FXInfo or DXInfo embedded struct		FXINFO_SCRIPT_AT = FXINFO_AT + 8,		FXINFO_FLAGS_AT = FXINFO_AT + 9;	/**	** Set the target information from the given FSSpec's values,	** without affecting any other contents of this CatalogAccess.	** This method should call setTargetWhat() and setTargetName().	** The dirIndex to setTargetWhat() should be zero.	*/	public void	setTarget( FSSpec fsSpec );	/**	** Set what the context of the assigned name will be by specifying	** a vol-ID, a dir-ID, and a dir-index.	** The name is unaffected, as is all the other non-target information.	**<ul type="disc">	**   <li>When dirIndex is any negative value, the name is ignored and	**     the resulting leaf-name will be the name of dirID on volRefNum.	**   <li>When dirIndex is zero, the name is interpreted relative to dirID on volRefNum.	**   <li>When dirIndex is greater than zero, the name is ignored and	**     the resulting leaf-name will be the name of the dirIndex'th item within dirID on volRefNum.	**     You can use this to scan a directory's entries, and stop when a FileNotFoundException is thrown.	**</ul>	*/	public void	setTargetWhat( int volRefNum, int dirID, int dirIndex );	/**	** Set the name without affecting any other fields.	** The name is presumed to be in literal Mac-native format, 	** i.e. with :'s as path-separators.	** The name may be a relative path-name, referenced to the last values	** set by setTargetWhere().	*/	public void 	setTargetName( String name );	/**	** Return the VRefNum, i.e. the volume reference number.	*/	public int 	getVRefNum();		/**	** Return the parent DirID, i.e. the DirID where the target is located.	*/	public int 	getParentDirID();	/**	** Return the input dirID, i.e. the input-value of where the target is located,	** which also returns from fill() holding the file-ID or dirID of the target itself.	*/	public int 	getIODirID();	// ###  O P E R A T O R S  ###	/**	** Read all-new values from disk using the current target values,	** without resolving any aliases.	** If the target does not exist, a FileNotFoundException is thrown.	** If a directory leading to the target does not exist, a FileNotFoundException is also thrown.	** Other errors throw a generic IOException.	*/	public void	fill()	  throws IOException;	/**	** Write the current values to disk.  The target must already exist.	**<p>	** The isLocked() state has no effect here.  You can't set or clear the file-lock this way.	** You have to call FSSpec.setFileLock() directly.	**	** @see FSSpec#setFileLock	*/	public void	write()	  throws IOException;}