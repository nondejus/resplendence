/*** Copyright 2001, 2003 by Gregory L. Guerin.** Terms of use:**  - Briefly: OPEN SOURCE under Artistic License -- credit fairly, use freely, alter carefully.**  - Fully: <http://www.amug.org/~glguerin/sw/artistic-license.html>** This file is from the MacBinary Toolkit for Java:**   <http://www.amug.org/~glguerin/sw/#macbinary> */package glguerin.io.imp.mac.jd2;import java.io.*;import com.apple.mrj.macos.toolbox.Toolbox;import glguerin.io.*;import glguerin.io.imp.mac.*;// --- Revision History ---// 05Jul01 GLG  create by diverging from FSRefItem10// 16Jan2003 GLG  add mayResolve() that always returns F// 23Jan2003 GLG  change mayResolve() to resolve symlinks and other 'rhap' aliases/**** A FSRefItem9 is an FSRefItem that uses the FSRef-based API from InterfaceLib under Mac OS 9.** Because there are a few things that an FSRef can't accomplish under Mac OS 9.0,** this class also uses an FSSpec and a CommentAccess.**** @author Gregory Guerin*/public class FSRefItem9  extends FSRefItem  implements com.apple.mrj.macos.libraries.InterfaceLib{	/**	** My FSSpec, used only for operations an FSRef can't accomplish on 9.0:	** alias-resolving and comment access.	*/	private JD2FSSpec mySpec;	/** My CommentAccess. */	private CommentAccess myCommentor;	/**	** Construct an empty FSRefItem.	*/	public	FSRefItem9()	{		super();		mySpec = new JD2FSSpec();		myCommentor = new JD2Comment();	}	/**	** Concrete factory-method.	*/	protected ForkRW	newFork( boolean forWriting, int forkRefNum, String tag )	{  return ( new FSFork9( forWriting, forkRefNum, tag ) );  }	/**	** Get the comment, or return an empty String.	** Never returns null.	**<p>	** Make an FSSpec for the reference in myRef1, then use CommentAccess.	*/	public String	getComment()	  throws IOException	{		validAndRef();		check( refToSpec( myRef1, mySpec ) );		myCommentor.setTarget( mySpec );		return ( new String( myCommentor.getCommentBytes() ) );	}	/**	** Set or remove the current target's comment.  	** If comment is null or zero-length, any existing comment is removed.	** If comment is 1 or more bytes, the comment is set.	**<p>	** Make an FSSpec for the reference in myRef1, then use CommentAccess.	*/	public void	setComment( String comment )	  throws IOException	{		validAndRef();		check( refToSpec( myRef1, mySpec ) );		myCommentor.setTarget( mySpec );		byte[] bytes = null;		if ( comment != null )			bytes = comment.getBytes();		myCommentor.writeComment( bytes );	}	/**	** Point theSpec to what theFSRef references, returning a result-code.	*/	protected int	refToSpec( byte[] theFSRef, FSSpec theSpec )	{		int osErr;		synchronized ( Toolbox.LOCK )		{			osErr = FSGetCatalogInfo( theFSRef, 0, NULL, NULL, theSpec.getByteArray(), NULL );		}		return ( osErr );	}	/**	** Set this FSItem so it references the root item for the given Pathname,	** returning the index of the Pathname part	** whose String should first be passed to refItem().	*/	protected int	refRoot( Pathname path )	  throws IOException	{		// ## FIXME: An empty Pathname should pretend to be a read-only		// directory containing the current list of mounted volume names.		if ( path.count() == 0 )			throw new UnsupportedIOException( "Empty Pathname" );		// The first part in the Pathname represents a volume name.		// Make the primary FSRef refer to the root dir of that volume.		// The rootRef() method must also set myChars appropriately.		isReferenced = false;		check( rootRef( path.part( 0 ), myRef1 ) );		// Getting here, the data in myRef1 references an existing item.		isReferenced = true;		// Remainder of path starts at index of 1.		return ( 1 );	}	/**	** Set this FSItem so it references the named item relative to its current reference,	** resolving aliases as requested, and handling cases of non-existence properly.	**<p>	** The Mac OS 9 imp of this method must handle the semantics of	** "." and "..", since those items exist in the file-system under Mac OS X, but not 9.	** The semantics of "." and ".." don't care what the resolveAliases flag is,	** since they are tacitly assumed to never be aliases.	*/	protected void	refItem( String part, boolean resolveAliases )	  throws IOException	{		// Must already reference an existing item.		if ( ! isReferenced )			check( Errors.dirNFErr );			// will always throw an IOException, so no return needed		// The "." semantics are to do nothing, since myRef1 already holds valid reference.		if ( ".".equals( part ) )			return;		// The ".." semantics are to reference parent directory.		if ( "..".equals( part ) )		{			// Get the name and parent FSRef of what's now in myRef1.			// The parent FSRef is left in myRef1 itself.			check( getRefInfo( myRef1, FSCatInfo.GET_EXIST, myInfo.getByteArray(), myChars, myRef1 ) );			return;		}		// If not handled yet, handle with superclass method.		super.refItem( part, resolveAliases );	}	/**	** Called by refPart(), which is called by refItem().	** On entry, myRef1 references an "apparent" directory, symlink, or alias-file.	** Return T if caller should attempt to resolve it, F if not.	**<p>	** An implementation of this method may use myRef1 but must not change it.	** It may use or change myRef2, and also myInfo, ignored[], and hadAlias[].	**<p>	** This imp returns T for symlinks ('slnk') and mounts ('lcmt').	** It returns F for all other kinds of files, even if they're alias-files.	** The actual test is just for alias-bit set and a creator-ID of 'rhap',	** which covers the known types ('slnk' and 'lcmt') and any types I don't know of.	** Overall, this is safer than requiring specific types, since it's better to have	** refPart() attempt resolving them than to not attempt it.	**<p>	** A symlink has alias-flag set, a file-type of 'slnk', and a creator of 'rhap'.	** It also has empty res-fork and non-empty data-fork.	**<p>	** A mount has alias-flag set, a file-type of  'lcmt', and a creator of 'rhap'.	** Both forks are empty, at least in all cases observed.	*/	protected boolean	mayResolve()	{		// Distinguish symlinks from alias-files using a FileInfo obtained for myRef1.		// The GETSET_FINFO data has core FinderInfo (flags, type, creator), but nothing else.		// To avoid IOExceptions, call getRefInfo() directly.		// Failure means myRef1 is inaccessible for some reason.		if ( getRefInfo( myRef1, FSCatInfo.GETSET_FINFO, myInfo.getByteArray(), null, null ) == 0 )		{			// If myRef1's FileInfo indicates a symlink or a mount, caller should try to resolve it.			// Rather than look for specific file-types, accept any alias-file with 'rhap' as creator.			// If myRef1 is anything else, eventually returns F.			if ( myInfo.isAlias()  &&  myInfo.getFileCreator() == 0x72686170 )  // 'rhap'				return ( true );		}//		System.err.println( " * FSRefItem9.mayResolve(): false" );		// Getting here, tell caller not to resolve myRef1.		return ( false );	}	/**	** Fill in the name and root FSRef of the indexed volume.	*/	protected int	volRef( int index, short[] volRefNum, char[] nameBuf, byte[] rootFSRef )	{		int osErr;		synchronized ( Toolbox.LOCK )		{			osErr = FSGetVolumeInfo( kFSInvalidVolumeRefNum, index, NULL,  0, NULL, nameBuf, rootFSRef );		}		return ( osErr );	}	/**	** Make the resultFSRef refer to the given file or directory.	** Return an OSErr value as the result.	** None of the items may be null.	**<p>	** If the targeted item doesn't exist, an error-code is returned.	** Unlike with an FSSpec, an FSRef can't refer to a non-existent item.	** The rest of the code in FSRefItem is responsible for handling non-existent targets,	** so they can be encapsulated with behavior similar to a non-existent FSSpec.	*/	protected int	makeRef( byte[] parentFSRef, String name, byte[] resultFSRef )	{		// Do this before acquiring Toolbox lock.		char[] chars = name.toCharArray();		int osErr;		synchronized ( Toolbox.LOCK )		{			osErr = FSMakeFSRefUnicode( parentFSRef, chars.length, chars, kTextEncodingUnknown, resultFSRef );				// textEncodingHint = kTextEncodingUnknown		}		return ( osErr );	}	/**	** Resolve the given FSRef as a possible alias-file.	** Return an OSError value as the result.	**<p>	** See "Alias Manager" Carbon docs.	**<p>	** FSResolveAliasFile does not exist on 9.0, so this implementation uses functions that do	** exist on 9.0 to accomplish the same goal.	** First, we check whether the FSRef really refers to an alias or not.	** Use myInfo and FSGetCatalogInfo() to do this.  If target exists and it's an alias, we proceed.	** Second, we turn the FSRef into an FSSpec, resolve it, then turn the FSSpec back into an FSRef.	** Use mySpec to do this, leaving the result in theFSRef.	** Any errors along the way stop the entire process, and are returned.	** In particular, a fnfErr occurring here will be returned to the caller.	*/	protected int	resolveRef( byte[] theFSRef, boolean resolveChains, byte[] targetIsFolder, byte[] wasAliased )	{		int osErr;		synchronized ( Toolbox.LOCK )		{			// Use myInfo to hold FinderInfo telling us about alias-ness.			// This determines whether we go through the subsequent gyrations to resolve an alias-file.			osErr = FSGetCatalogInfo( theFSRef, myInfo.GETSET_FINFO, myInfo.getByteArray(), NULL, NULL, NULL );			if ( osErr == 0  &&  myInfo.isAlias() )			{				// Next, turn the FSRef into an FSSpec, and then resolve it.				// Any errors along the way stop the process, and will prove fatal.				// 				osErr = FSGetCatalogInfo( theFSRef, 0, NULL, NULL, mySpec.getByteArray(), NULL );				if ( osErr == 0 )				{					osErr = mySpec.resolveMe( resolveChains, targetIsFolder, wasAliased );					// All non-zero result-codes are errors.  A non-existent leaf (fnfErr) is not acceptable.					if ( osErr == 0 )						osErr = FSpMakeFSRef( mySpec.getByteArray(), theFSRef );				}			}		}		return ( osErr );	}		/**	** Get the FSCatalogInfo for theFSRef.	** Return an OSErr value as the result.	** The nameBuf or parentFSRef may be null.	*/	protected int	getRefInfo( byte[] theFSRef, int whichInfo, byte[] catInfo, char[] nameBuf, byte[] parentFSRef )	{		int osErr;		synchronized ( Toolbox.LOCK )		{			if ( nameBuf != null  &&  parentFSRef != null )				osErr = FSGetCatalogInfo( theFSRef, whichInfo, catInfo, nameBuf, NULL, parentFSRef );			else				osErr = FSGetCatalogInfo( theFSRef, whichInfo, catInfo, NULL, NULL, NULL );		}		return ( osErr );	}	/**	** Set the FSCatalogInfo for theFSRef.	** Return an OSErr value as the result.	*/	protected int	setRefInfo( byte[] theFSRef, int whichInfo, byte[] catInfo )	{		int osErr;		synchronized ( Toolbox.LOCK )		{			osErr = FSSetCatalogInfo( theFSRef, whichInfo, catInfo );		}		return ( osErr );	}	/**	** Create the file or directory referenced by the FSRef and other args.	** Return an OSErr value as the result.	** None of the items may be null.	*/	protected int	createRef( byte[] parentFSRef, String name, boolean isDirectory, byte[] resultFSRef )	{		char[] chars = name.toCharArray();		int osErr;		synchronized ( Toolbox.LOCK )		{			if ( isDirectory )				osErr = FSCreateDirectoryUnicode( parentFSRef, chars.length, chars,						0, NULL, resultFSRef, NULL, NULL );			else				osErr = FSCreateFileUnicode( parentFSRef, chars.length, chars,						0, NULL, resultFSRef, NULL );				// 0 = empty infoBitMap		}		return ( osErr );	}	/**	** Delete the file or directory referenced by the FSRef,	** without resolving any aliases.	** Return an OSError value as the result.	*/	protected int	deleteRef( byte[] theFSRef )	{		int osErr;		synchronized ( Toolbox.LOCK )		{			osErr = FSDeleteObject( theFSRef );		}		return ( osErr );	}	/**	** Move the file or directory referenced by the FSRef,	** without resolving any aliases.	** Return an OSError value as the result.	** The destination must reference an existing directory.	*/	protected int	moveRef( byte[] theFSRef, byte[] destinationFSRef )	{		int osErr;		synchronized ( Toolbox.LOCK )		{			osErr = FSMoveObject( theFSRef, destinationFSRef, NULL );		}		return ( osErr );	}	/**	** Rename the file or directory referenced by the FSRef,	** without resolving any aliases.	** Return an OSError value as the result.	*/	protected int	renameRef( byte[] theFSRef, String newName, byte[] resultFSRef )	{		char[] chars = newName.toCharArray();		int osErr;		synchronized ( Toolbox.LOCK )		{			osErr = FSRenameUnicode( theFSRef, chars.length, chars, kTextEncodingUnknown, resultFSRef );				// text encoding hint = kTextEncodingUnknown		}		return ( osErr );	}	/**	** Open the item's named fork.	** Return an OSError value as the result.	*/	protected int	openRef( byte[] theFSRef, char[] forkName, byte permissions, short[] refNum )	{		int osErr;		synchronized ( Toolbox.LOCK )		{			osErr = FSOpenFork( theFSRef, forkName.length, forkName, permissions, refNum );		}		return ( osErr );	}	/**	** Return an opaque iterator Object for iterating over theFSRef.	** Returns an instance of IOException, appropriately prepared, if there was an error.	** Otherwise returns an instance of an arbitrary Object representing an iterator.	*/	protected Object	begin( byte[] theFSRef )	{		// The opaque Object is an int[2] array.		//   - array[0] is temporary storage for next() to use.		//   - array[1] holds the FSIterator itself.		int[] iteratorRef = new int[ 2 ];		int osErr;		synchronized ( Toolbox.LOCK )		{			osErr = FSOpenIterator( theFSRef, 0, iteratorRef );		}		// If an error occurred, return an IOException instead of an opaque iterator Object.		try		{  check( osErr );  }		catch ( IOException why )		{  return ( why );  }		// Move the actual FSIterator into slot[1].		// This is so next() can use the array[0] slot to hold actual-count.		iteratorRef[ 1 ] = iteratorRef[ 0 ];		// Getting here, return the iteratorRef.		return ( iteratorRef );	}	/**	** Get the name of the next item iterated, or	** return null when nothing left to iterate.	** Errors also cause a null return, which simply halts the iteration.	**<p>	** The name must be a literalized (i.e. accent-composed) name, 	** suitable for adding directly to a Pathname.	**<p>	** Changes the contents of myInfo, myChars, and myRef2, which are 	** used calling FSGetCatalogInfoBulk() over the course of the iteration.	*/	protected String	next( Object iterator )	{		String result = null;		if ( iterator instanceof int[] )		{			// The int at array[1] is the actual FSIterator.			// We use the slot at array[0] to hold the actual-count returned in			// pointer-to-int passed to FSGetCatalogInfoBulk.  Yes, it's wacky,			// but since it's all opaque, so what?  And it's easier than making			// another int[] just to hold an int we don't really even care about.			int[] array = (int[]) iterator;			int osErr;			synchronized ( Toolbox.LOCK )			{				// All we really want is the name, so the GET_EXIST mask is more than adequate.				// Could even use zero for the mask, but what the heck.				// Use myRef2 as a temporary byte[] for containerChanged signal.				// It's visible, it's killable, it's available, so why not?				osErr = FSGetCatalogInfoBulk( array[ 1 ], 1, array, myRef2,						FSCatInfo.GET_EXIST, myInfo.getByteArray(), NULL, NULL, myChars );				// Could check whether array[0] is 1 (actual count) or 0 (no more items).				// Could check whether myRef2[0] is non-zero, indicating that container changed.			}			// osErr: errFSNoMoreItems = -1417 indicates end of FSIterator			// Not that we distinguish it from any other errors.			// Since we used myChars as the name-buffer, getName() returns			// the appropriately literalized form of the name.			if ( osErr == 0 )				result = getName();		}		return ( result );	}	/**	** Stop iterating using the FSIterator started by begin().	*/	protected void	end( Object iterator )	{		if ( iterator instanceof int[] )		{			int[] array = (int[]) iterator;			synchronized ( Toolbox.LOCK )			{  FSCloseIterator( array[ 1 ] );  }		// don't care about any errors			array[ 1 ] = 0;		}		return;	}	/**	** InterfaceLib 9.0+, CarbonLib 1.0+, Mac OS 10.0+	*/	private static native short 	FSGetVolumeInfo( short volume, int index, int volRefNumPtr,			int infoBitMap, int infoPtr, char[] hfsUniStr255, byte[] rootFSRef );	/**	** InterfaceLib 9.0+, CarbonLib 1.0+, Mac OS 10.0+	*/	private static native short 	FSMakeFSRefUnicode( byte[] parentFSRef, int nameLen, char[] uniCharName,			int textEncodingHint, byte[] resultFSRef );	/**	** InterfaceLib 9.0+, CarbonLib 1.0+, Mac OS 10.0+	*/	private static native short 	FSGetCatalogInfo( byte[] theFSRef, int whichInfo, byte[] catInfo,			char[] outName, int noFSSpec, byte[] parentFSRef );	/**	** InterfaceLib 9.0+, CarbonLib 1.0+, Mac OS 10.0+	** Same function as the other binding, but with 'int' args holding NULL (0).	** This is necessary because Java's 'null' reference is not equivalent to C's NULL.	*/	private static native short 	FSGetCatalogInfo( byte[] theFSRef, int whichInfo, byte[] catInfo,			 int noName, int noFSSpec, int noParentRef );	/**	** InterfaceLib 9.0+, CarbonLib 1.0+, Mac OS 10.0+	*/	private static native short 	FSSetCatalogInfo( byte[] theFSRef, int whichInfo, byte[] catInfo );	/**	** InterfaceLib 9.0+, CarbonLib 1.0+, Mac OS 10.0+	** Same function as the other bindings, but with a byte[] type for the FSSpec.	*/	private static native short 	FSGetCatalogInfo( byte[] theFSRef, int whichInfo, int noCatInfo,			 int noName, byte[] anFSSpec, int noParentRef );	/**	** InterfaceLib 9.0+, CarbonLib 1.0+, Mac OS 10.0+	*/	private static native short 	FSpMakeFSRef( byte[] theFSSpec, byte[] resultFSRef );	/**	** InterfaceLib 9.0+, CarbonLib 1.0+, Mac OS 10.0+	*/	private static native short 	FSCreateFileUnicode( byte[] parentFSRef, int nameLen, char[] uniCharName,			int infoBitMap, int infoPtr, byte[] resultFSRef, int nullFSSpec );	/**	** InterfaceLib 9.0+, CarbonLib 1.0+, Mac OS 10.0+	*/	private static native short 	FSCreateDirectoryUnicode( byte[] parentFSRef, int nameLen, char[] uniCharName,			int infoBitMap, int infoPtr, byte[] resultFSRef, int nullFSSpec, int uint32Ptr );	/**	** InterfaceLib 9.0+, CarbonLib 1.0+, Mac OS 10.0+	*/	private static native short 	FSDeleteObject( byte[] theFSRef );	/**	** InterfaceLib 9.0+, CarbonLib 1.0+, Mac OS 10.0+	*/	private static native short 	FSMoveObject( byte[] theFSRef, byte[] destFSRefDir, int nullNewFSRef );	/**	** InterfaceLib 9.0+, CarbonLib 1.0+, Mac OS 10.0+	*/	private static native short 	FSRenameUnicode( byte[] theFSRef, int nameLen, char[] uniCharName,			int textEncodingHint, byte[] resultFSRef );	/**	** InterfaceLib 9.0+, CarbonLib 1.0+, Mac OS 10.0+	*/	private static native short 	FSOpenFork( byte[] theFSRef, int nameLen, char[] forkName, byte permissions, short[] forkRefNum );	/**	** InterfaceLib 9.0+, CarbonLib 1.0+, Mac OS 10.0+	*/	private static native short 	FSOpenIterator( byte[] theFSRef, int iteratorFlags, int[] iteratorRef );	/**	** InterfaceLib 9.0+, CarbonLib 1.0+, Mac OS 10.0+	*/	private static native short 	FSGetCatalogInfoBulk( int fsIterator, int maxObj, int[] actual, byte[] containerChanged,			int whichInfo, byte[] catInfos, int noFSRefs, int noFSSpecs, char[] names );	/**	** InterfaceLib 9.0+, CarbonLib 1.0+, Mac OS 10.0+	*/	private static native short 	FSCloseIterator( int fsIterator );}