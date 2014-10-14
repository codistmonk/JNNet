package jnnet;

import static net.sourceforge.aprog.tools.Tools.array;
import static net.sourceforge.aprog.tools.Tools.debugPrint;
import static net.sourceforge.aprog.tools.Tools.getResourceAsStream;
import static net.sourceforge.aprog.tools.Tools.writeAndClose;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Locale;

import net.sourceforge.aprog.tools.IllegalInstantiationException;
import net.sourceforge.aprog.tools.SystemProperties;

/**
 * @author codistmonk (creation 2013-11-11)
 */
public final class DLLTools {
	
	private DLLTools() {
		throw new IllegalInstantiationException();
	}
	
	public static final String OS_TYPE = computeOSType();
	
	public static final String ARCHITECTURE = computeArchitecture();
	
	public static final String PLATFORM = OS_TYPE + "_" + ARCHITECTURE;
	
	public static final String DLL_PREFIX = computeDLLPrefix(OS_TYPE);
	
	public static final String DLL_EXTENSION = computeDLLExtension(OS_TYPE);
	
	public static final String computeOSType() {
		final String osName = SystemProperties.getOSName().toLowerCase(Locale.ENGLISH);
		
		for (final String osType : array("windows", "linux")) {
			if (osName.contains(osType)) {
				return osType;
			}
		}
		
		if (osName.contains("mac os x")) {
			return "macosx";
		}
		
		return osName;
	}
	
	public static final String computeArchitecture() {
		final String osArch = System.getProperty("os.arch").toLowerCase();
		
		if (osArch.contains("64")) {
			return "x86_64";
		}
		
		return "x86";
	}
	
	public static final String computeDLLPrefix(final String osType) {
		if ("windows".equals(osType)) {
			return "";
		}
		
		if ("linux".equals(osType) || "macosx".equals(osType)) {
			return "lib";
		}
		
		return "";
	}
	
	public static final String computeDLLExtension(final String osType) {
		if ("windows".equals(osType)) {
			return ".dll";
		}
		
		if ("linux".equals(osType)) {
			return ".so";
		}
		
		if ("macosx".equals(osType)) {
			return ".dylib";
		}
		
		return "";
	}
	
	public static final void loadDLL(final String dllResourcePath, final String dllTemporaryFilePath) {
		final File dll = new File(dllTemporaryFilePath);
		
		try {
			debugPrint(dllResourcePath, "->", dll);
			
			try {
				writeAndClose(getResourceAsStream(dllResourcePath), true, new FileOutputStream(dll), true);
				// XXX Has no effect after System.load() on Windows
				dll.deleteOnExit();
			} catch (final IOException exception) {
				if (dll.canRead()) {
					debugPrint(exception);
					System.load(dll.getAbsolutePath());
				} else {
					exception.printStackTrace();
				}
			}
		} catch (final Throwable error) {
			error.printStackTrace();
		}
	}
	
	public static final void loadDLL(final String libraryName) {
		final String dllName = DLL_PREFIX + libraryName + "_" + ARCHITECTURE + DLL_EXTENSION;
		
		loadDLL(PLATFORM + "/" + dllName, dllName);
	}
	
}
