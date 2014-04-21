package jnnet;

import static net.sourceforge.aprog.tools.Tools.ignore;
import static net.sourceforge.aprog.tools.Tools.unchecked;

import java.lang.reflect.Array;
import java.lang.reflect.Method;
import java.util.Collection;

import net.sourceforge.aprog.tools.IllegalInstantiationException;

/**
 * @author codistmonk (creation 2014-03-10)
 */
public final class Functional {
	
	private Functional() {
		throw new IllegalInstantiationException();
	}
	
	public static final Method CLONE = method(Object.class, "clone");
	
	@SuppressWarnings("unchecked")
	public static final <In, T> Collection<T>[] map(final In[] methodObjects, final Method method) {
		return map(Collection.class, methodObjects, method);
	}
	
	public static final <In, T> Collection<T>[] map(final Object methodObject, final Method method, final In[] singleArguments) {
		return map(Collection.class, methodObject, method, singleArguments);
	}
	
	@SuppressWarnings("unchecked")
	public static final <T> Collection<T>[] map(final Object methodObject, final Method method, final Object[][] multipleArguments) {
		return map(Collection.class, methodObject, method, multipleArguments);
	}
	
	@SuppressWarnings("unchecked")
	public static final <In, Out> Out[] map(final Class<Out> resultComponentType,
			final In[] methodObjects, final Method method) {
		try {
			final int n = methodObjects.length;
			final Out[] result = (Out[]) Array.newInstance(resultComponentType, n);
			
			for (int i = 0; i < n; ++i) {
				result[i] = (Out) method.invoke(methodObjects[i]);
			}
			
			return result;
		} catch (final Exception exception) {
			throw unchecked(exception);
		}
	}
	
	@SuppressWarnings("unchecked")
	public static final <In, Out, OutArray> OutArray map(final Class<Out> resultComponentType,
			final Object methodObject, final Method method, final In[] singleArguments) {
		try {
			final int n = singleArguments.length;
			final OutArray result = (OutArray) Array.newInstance(resultComponentType, n);
			
			for (int i = 0; i < n; ++i) {
				Array.set(result, i, method.invoke(methodObject, singleArguments[i]));
			}
			
			return result;
		} catch (final Exception exception) {
			throw unchecked(exception);
		}
	}
	
	@SuppressWarnings("unchecked")
	public static final <Out> Out[] map(final Class<Out> resultComponentType,
			final Object methodObject, final Method method, final Object[][] multipleArguments) {
		try {
			final int n = multipleArguments.length;
			final Out[] result = (Out[]) Array.newInstance(resultComponentType, n);
			
			for (int i = 0; i < n; ++i) {
				result[i] = (Out) method.invoke(methodObject, multipleArguments[i]);
			}
			
			return result;
		} catch (final Exception exception) {
			throw unchecked(exception);
		}
	}
	
	public static final Method method(final Class<?> cls, final String methodName, final Class<?>... parameterTypes) {
		Method result = null;
		
		try {
			result = cls.getMethod(methodName, parameterTypes);
		} catch (final Exception exception) {
			ignore(exception);
		}
		
		if (result == null) {
			try {
				result = cls.getDeclaredMethod(methodName, parameterTypes);
			} catch (final Exception exception) {
				throw unchecked(exception);
			}
		}
		
		result.setAccessible(true);
		
		return result;
	}
	
}
