package de.foellix.aql.jicer.soot;

import java.util.HashSet;
import java.util.Set;

import de.foellix.aql.jicer.Data;
import de.foellix.aql.jicer.config.Config;
import soot.Body;
import soot.Scene;
import soot.SootClass;
import soot.SootField;
import soot.SootMethod;
import soot.Type;
import soot.Unit;
import soot.Value;

public class SootHelper {
	private static Set<String> defaultExcludes;
	private static Set<String> exceptionalIncludes;

	public static Set<SootClass> getAllOuterClasses(SootClass sc) {
		final Set<SootClass> allClasses = new HashSet<>();
		while (sc != null) {
			// extends
			allClasses.add(sc);

			// Continue with outerClass
			sc = sc.getOuterClassUnsafe();
		}
		return allClasses;
	}

	public static Set<SootClass> getAllAccessibleClasses(SootClass sc) {
		final Set<SootClass> allClasses = new HashSet<>();
		while (sc != null) {
			// extends
			allClasses.add(sc);

			// Continue with superClass
			sc = sc.getSuperclassUnsafe();
		}
		return allClasses;
	}

	public static Set<SootClass> getAllAccessibleInterfaces(SootClass sc) {
		return getAllAccessibleInterfaces(sc, new HashSet<>());
	}

	private static Set<SootClass> getAllAccessibleInterfaces(SootClass sc, Set<SootClass> visited) {
		final Set<SootClass> allInterfaces = new HashSet<>();
		if (!visited.contains(sc)) {
			while (sc != null) {
				// extends
				if (sc.isInterface()) {
					allInterfaces.add(sc);
				}

				// implements
				for (final SootClass interfaceClass : sc.getInterfaces()) {
					allInterfaces.add(interfaceClass);
					allInterfaces.addAll(getAllAccessibleInterfaces(interfaceClass, allInterfaces));
				}

				// Continue with superClass
				sc = sc.getSuperclassUnsafe();
			}
		}
		return allInterfaces;
	}

	public static Set<SootClass> getAllAccessibleClassesAndInterfaces(SootClass sc) {
		final Set<SootClass> allClasses = new HashSet<>();
		while (sc != null) {
			// extends
			allClasses.add(sc);

			// implements
			for (final SootClass interfaceClass : sc.getInterfaces()) {
				allClasses.add(interfaceClass);
				allClasses.addAll(getAllAccessibleInterfaces(interfaceClass));
			}

			// Continue with superClass
			sc = sc.getSuperclassUnsafe();
		}
		return allClasses;
	}

	public static Set<SootMethod> getAllAccessibleMethods(Type type) {
		return getAllAccessibleMethods(Scene.v().getSootClass(type.toString()));
	}

	public static Set<SootMethod> getAllAccessibleMethods(SootClass sc) {
		final Set<SootMethod> allMethods = new HashSet<>();
		while (sc != null) {
			// extends
			allMethods.addAll(sc.getMethods());

			// implements
			for (final SootClass interfaceClass : sc.getInterfaces()) {
				allMethods.addAll(interfaceClass.getMethods());
			}

			// Continue with superClass(es)
			sc = sc.getSuperclassUnsafe();
		}
		return allMethods;
	}

	public static boolean isCallBackClass(Value useValue) {
		final String type = useValue.getType().toString();
		final SootClass sc = Scene.v().getSootClassUnsafe(type);
		return isCallBackClass(sc);
	}

	public static boolean isCallBackClass(SootClass sc) {
		while (sc != null) {
			// extends
			if (Data.getInstance().getCallBackClasses().contains(sc.getName())) {
				return true;
			}
			// implements
			for (final SootClass interfaceClass : sc.getInterfaces()) {
				if (Data.getInstance().getCallBackClasses().contains(interfaceClass.getName())) {
					return true;
				}
			}

			// Continue with superClass(es)
			sc = sc.getSuperclassUnsafe();
		}
		return false;
	}

	public static boolean isCallBackMethod(SootMethod sm) {
		// TODO: (Future work) Make this more accurate!
		if (sm.getName().startsWith("on")) {
			// Android Callback
			return true;
		}
		return false;
	}

	public static boolean isAnonymousClass(SootClass sc) {
		if (sc.isInnerClass()) {
			final String innerName = sc.getName().substring(sc.getName().lastIndexOf('$') + 1);
			if (innerName.matches("[0-9]+")) {
				return true;
			}
		}
		return false;
	}

	public static boolean isTemporaryField(SootField field) {
		final String innerName = field.getName().substring(field.getName().lastIndexOf('$') + 1);
		if (innerName.matches("[0-9]+")) {
			return true;
		}
		return false;
	}

	public static SootMethod getMethod(Unit unit) {
		if (Data.getInstance().getEntryMethod(unit) != null) {
			return Data.getInstance().getEntryMethod(unit);
		} else {
			for (final SootClass sc : Scene.v().getApplicationClasses()) {
				for (final SootMethod sm : sc.getMethods()) {
					if (sm.isConcrete()) {
						final Body body = sm.retrieveActiveBody();
						for (final Unit needle : body.getUnits()) {
							if (needle == unit
									|| Data.getInstance().getReplacedNodesOriginalToReplacement().get(needle) == unit) {
								return sm;
							}
						}
					}
				}
			}
			return null;
		}
	}

	public static Body getActiveBodyIfMethodExists(SootMethod sm) {
		try {
			return sm.retrieveActiveBody();
		} catch (final RuntimeException e) {
			return null;
		}
	}

	public static Set<String> getDefaultExcludes() {
		if (defaultExcludes == null) {
			defaultExcludes = new HashSet<>();
			for (final String defaultExclude : Config.getInstance().defaultExcludes.split(", ")) {
				defaultExcludes.add(defaultExclude);
			}
		}
		return defaultExcludes;
	}

	public static Set<String> getExceptionalIncludes() {
		if (exceptionalIncludes == null) {
			exceptionalIncludes = new HashSet<>();
		}
		return exceptionalIncludes;
	}

	public static boolean isOrdinaryLibraryClass(SootClass sootClass, boolean ignoreExceptionalIncludes) {
		final String classname = sootClass.getName();
		if (!ignoreExceptionalIncludes && getExceptionalIncludes().contains(classname)) {
			return false;
		}
		for (final String exclude : getDefaultExcludes()) {
			if (classname.startsWith(exclude)) {
				return true;
			}
		}
		return false;
	}

	public static boolean isOrdinaryLibraryMethod(SootMethod sootMethod, boolean ignoreExceptionalIncludes) {
		return isOrdinaryLibraryClass(sootMethod.getDeclaringClass(), ignoreExceptionalIncludes);
	}

	public static void reset() {
		exceptionalIncludes = null;
	}

	public static Set<SootMethod> findMethods(String name, int i) {
		final Set<SootMethod> methods = new HashSet<>();
		for (final SootClass sc : Scene.v().getApplicationClasses()) {
			for (final SootMethod sm : sc.getMethods()) {
				if (sm.getName().equals(name) && sm.getParameterCount() == i) {
					methods.add(sm);
				}
			}
		}
		return methods;
	}
}