/*
 *  SoSy-Lab Common is a library of useful utilities.
 *  This file is part of SoSy-Lab Common.
 *
 *  Copyright (C) 2007-2015  Dirk Beyer
 *  All rights reserved.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.sosy_lab.common;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Strings.isNullOrEmpty;
import static com.google.common.base.Verify.verify;
import static com.google.common.collect.ImmutableList.toImmutableList;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Throwables;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableMultiset;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Multiset.Entry;
import com.google.common.reflect.AbstractInvocationHandler;
import com.google.common.reflect.Invokable;
import com.google.common.reflect.Reflection;
import com.google.common.reflect.TypeToken;
import com.google.errorprone.annotations.CanIgnoreReturnValue;
import com.google.errorprone.annotations.CheckReturnValue;
import com.google.errorprone.annotations.FormatMethod;
import com.google.errorprone.annotations.Var;
import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.AnnotatedType;
import java.lang.reflect.Constructor;
import java.lang.reflect.Executable;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Parameter;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.WildcardType;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.function.Predicate;
import java.util.logging.Level;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.sosy_lab.common.ExtendedURLClassLoader.ExtendedURLClassLoaderConfiguration;
import org.sosy_lab.common.annotations.Unmaintained;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.log.LogManager;

/** Helper class for various methods related to handling Java classes and types. */
public final class Classes {

  private Classes() {}

  /** Exception thrown by {@link Classes#createInstance(Class, Class[], Object[], Class)}. */
  @Deprecated
  public static class ClassInstantiationException extends Exception {

    private static final long serialVersionUID = 7862065219560550275L;

    public ClassInstantiationException(String className, String msg, Throwable cause) {
      super("Cannot instantiate class " + className + ":" + msg, cause);
    }

    public ClassInstantiationException(String className, Throwable cause) {
      super("Cannot instantiate class " + className + ":" + cause.getMessage(), cause);
    }
  }

  /**
   * An exception that should be used if a checked exception is encountered in a situation where it
   * is not excepted (e.g., when getting the result from a {@link Callable} of which you know it
   * shouldn't throw such exceptions).
   */
  public static final class UnexpectedCheckedException extends RuntimeException {

    private static final long serialVersionUID = -8706288432548996095L;

    public UnexpectedCheckedException(String message, Throwable source) {
      super(
          "Unexpected checked exception "
              + source.getClass().getSimpleName()
              + (isNullOrEmpty(message) ? "" : " during " + message)
              + (isNullOrEmpty(source.getMessage()) ? "" : ": " + source.getMessage()),
          source);

      assert (source instanceof Exception) && !(source instanceof RuntimeException);
    }
  }

  /**
   * Return the {@link Path} to the location of the code of the given class, e.g., the JAR file. If
   * the class is in a {@code *.class} file, the base directory of the package structure is
   * returned.
   */
  public static Path getCodeLocation(Class<?> cls) {
    try {
      return Paths.get(cls.getProtectionDomain().getCodeSource().getLocation().toURI());
    } catch (URISyntaxException e) {
      throw new AssertionError(e);
    }
  }

  /**
   * Creates an instance of class cls, passing the objects from argumentList to the constructor and
   * casting the object to class type.
   *
   * @param cls The class to instantiate.
   * @param argumentTypes Array with the types of the parameters of the desired constructor.
   * @param argumentValues Array with the values that will be passed to the constructor.
   * @param type The return type (has to be a super type of the class, of course).
   * @throws ClassInstantiationException If something goes wrong (like class cannot be found or has
   *     no constructor).
   * @throws InvocationTargetException If the constructor throws an exception.
   */
  @Deprecated
  public static <T> T createInstance(
      Class<? extends T> cls,
      Class<?> @Nullable [] argumentTypes,
      Object @Nullable [] argumentValues,
      Class<T> type)
      throws ClassInstantiationException, InvocationTargetException {
    checkNotNull(type);
    try {
      Constructor<? extends T> ct = cls.getConstructor(argumentTypes);
      return ct.newInstance(argumentValues);

    } catch (SecurityException | InstantiationException | IllegalAccessException e) {
      throw new ClassInstantiationException(cls.getCanonicalName(), e);
    } catch (NoSuchMethodException e) {
      throw new ClassInstantiationException(
          cls.getCanonicalName(), "Matching constructor not found!", e);
    }
  }

  /**
   * Creates an instance of class cls, passing the objects from argumentList to the constructor and
   * casting the object to class type.
   *
   * <p>If there is no matching constructor or the the class cannot be instantiated, an
   * InvalidConfigurationException is thrown.
   *
   * @param type The return type (has to be a super type of the class, of course).
   * @param cls The class to instantiate.
   * @param argumentTypes Array with the types of the parameters of the desired constructor
   *     (optional).
   * @param argumentValues Array with the values that will be passed to the constructor.
   */
  @Deprecated
  public static <T> T createInstance(
      Class<T> type,
      Class<? extends T> cls,
      Class<?> @Nullable [] argumentTypes,
      Object[] argumentValues)
      throws InvalidConfigurationException {
    return createInstance(type, cls, argumentTypes, argumentValues, RuntimeException.class);
  }

  /**
   * Creates an instance of class cls, passing the objects from argumentList to the constructor and
   * casting the object to class type.
   *
   * <p>If there is no matching constructor or the the class cannot be instantiated, an
   * InvalidConfigurationException is thrown.
   *
   * @param type The return type (has to be a super type of the class, of course).
   * @param cls The class to instantiate.
   * @param argumentTypes Array with the types of the parameters of the desired constructor
   *     (optional).
   * @param argumentValues Array with the values that will be passed to the constructor.
   * @param exceptionType An exception type the constructor is allowed to throw.
   */
  @Deprecated
  public static <T, X extends Exception> T createInstance(
      Class<T> type,
      Class<? extends T> cls,
      @Var Class<?> @Nullable [] argumentTypes,
      Object[] argumentValues,
      Class<X> exceptionType)
      throws X, InvalidConfigurationException {
    checkNotNull(exceptionType);
    if (argumentTypes == null) {
      // fill argumenTypes array
      argumentTypes = Stream.of(argumentValues).map(Object::getClass).toArray(Class[]::new);
    } else {
      checkArgument(argumentTypes.length == argumentValues.length);
    }

    String className = cls.getSimpleName();
    String typeName = type.getSimpleName();

    // get constructor
    Constructor<? extends T> ct;
    try {
      ct = cls.getConstructor(argumentTypes);
    } catch (NoSuchMethodException e) {
      throw new InvalidConfigurationException(
          "Invalid " + typeName + " " + className + ", no matching constructor", e);
    }

    // verify signature
    String exception =
        Classes.verifyDeclaredExceptions(ct, exceptionType, InvalidConfigurationException.class);
    if (exception != null) {
      throw new InvalidConfigurationException(
          String.format(
              "Invalid %s %s, constructor declares unsupported checked exception %s.",
              typeName, className, exception));
    }

    // instantiate
    try {
      return ct.newInstance(argumentValues);

    } catch (InstantiationException e) {
      throw new InvalidConfigurationException(
          String.format(
              "Invalid %s %s, class cannot be instantiated (%s).",
              typeName, className, e.getMessage()),
          e);

    } catch (IllegalAccessException e) {
      throw new InvalidConfigurationException(
          "Invalid " + typeName + " " + className + ", constructor is not accessible", e);

    } catch (InvocationTargetException e) {
      Throwable t = e.getCause();
      Throwables.propagateIfPossible(t, exceptionType, InvalidConfigurationException.class);

      throw new UnexpectedCheckedException("instantiation of " + typeName + " " + className, t);
    }
  }

  /**
   * Similar to {@link Class#forName(String)}, but if the class is not found this method re-tries
   * with a package name prefixed.
   *
   * @param name The class name.
   * @param prefix An optional package name as prefix.
   * @return The class object for name or prefix + "." + name
   * @throws ClassNotFoundException If none of the two classes can be found.
   * @throws SecurityException If a security manager denies access to the class loader
   */
  public static Class<?> forName(String name, @Nullable String prefix)
      throws ClassNotFoundException {
    return forName(name, prefix, null);
  }

  /**
   * Similar to {@link Class#forName(String)} and {@link ClassLoader#loadClass(String)}, but if the
   * class is not found this method re-tries with a package name prefixed.
   *
   * @param name The class name.
   * @param prefix An optional package name as prefix.
   * @param cl An optional class loader to load the class (may be null).
   * @return The class object for name or prefix + "." + name
   * @throws ClassNotFoundException If none of the two classes can be found.
   * @throws SecurityException If a security manager denies access to the class loader
   */
  private static Class<?> forName(
      String name, @Nullable String prefix, @Var @Nullable ClassLoader cl)
      throws ClassNotFoundException {
    if (cl == null) {
      // use the class loader of this class to simulate the behaviour
      // of Class#forName(String)
      cl = Classes.class.getClassLoader();
    }
    if (isNullOrEmpty(prefix)) {
      return cl.loadClass(name);
    }

    try {
      return cl.loadClass(name);

    } catch (ClassNotFoundException e) {
      try {
        return cl.loadClass(prefix + "." + name); // try with prefix added
      } catch (ClassNotFoundException e2) {
        e.addSuppressed(e2);
        throw e; // re-throw original exception to get correct error message
      }
    }
  }

  /**
   * Verify that a constructor or method declares no other checked exceptions except a given type.
   *
   * <p>Returns the name of any violating exception, or null if there is none.
   *
   * @param executable The executable to check.
   * @param allowedExceptionTypes The type of exception that is allowed.
   * @return Null or the name of a declared exception.
   */
  public static @Nullable String verifyDeclaredExceptions(
      Executable executable, Class<?>... allowedExceptionTypes) {
    return verifyDeclaredExceptions(
        Arrays.asList(executable.getExceptionTypes()), Arrays.asList(allowedExceptionTypes));
  }

  /**
   * Verify that a constructor or method declares no other checked exceptions except a given type.
   *
   * <p>Returns the name of any violating exception, or null if there is none.
   *
   * @param invokable The invokable to check.
   * @param allowedExceptionTypes The type of exception that is allowed.
   * @return Null or the name of a declared exception.
   */
  public static @Nullable String verifyDeclaredExceptions(
      Invokable<?, ?> invokable, Class<?>... allowedExceptionTypes) {
    return verifyDeclaredExceptions(
        FluentIterable.from(invokable.getExceptionTypes()).transform(TypeToken::getRawType),
        Arrays.asList(allowedExceptionTypes));
  }

  @VisibleForTesting
  static @Nullable String verifyDeclaredExceptions(
      Iterable<Class<?>> declaredExceptionTypes, Iterable<Class<?>> pAllowedExceptionTypes) {
    // RuntimeException and Error are always allowed
    FluentIterable<Class<?>> allowedExceptionTypes =
        FluentIterable.from(pAllowedExceptionTypes).append(RuntimeException.class, Error.class);

    for (Class<?> declaredException : declaredExceptionTypes) {
      if (!allowedExceptionTypes.anyMatch(
          allowedExceptionType -> allowedExceptionType.isAssignableFrom(declaredException))) {
        return declaredException.getSimpleName();
      }
    }
    return null;
  }

  /** See {@link #getSingleTypeArgument(Type)}. */
  public static TypeToken<?> getSingleTypeArgument(TypeToken<?> type) {
    return TypeToken.of(getSingleTypeArgument(type.getType()));
  }

  /**
   * From a type {@code X<Foo>}, extract the {@code Foo}. This is the value of {@link
   * ParameterizedType#getActualTypeArguments()}. This method also supports {@code X<? extends
   * Foo>}, {@code X<Foo<?>>} etc.
   *
   * <p>Example results:
   *
   * <pre>{@code
   * X<Foo>          : Foo
   * X<? extends Foo>: Foo
   * X<Foo<Bar>>     : Foo<Bar>
   * }</pre>
   *
   * @param type The type (needs to be parameterized with exactly one parameter)
   * @return A Type object.
   */
  public static Type getSingleTypeArgument(Type type) {
    checkNotNull(type);
    checkArgument(
        type instanceof ParameterizedType,
        "Cannot extract generic parameter from non-parameterized type %s",
        type);

    ParameterizedType pType = (ParameterizedType) type;
    Type[] parameterTypes = pType.getActualTypeArguments();

    checkArgument(
        parameterTypes.length == 1,
        "Cannot extract generic parameter from parameterized type %s"
            + " which has not exactly one parameter",
        type);

    return extractUpperBoundFromType(parameterTypes[0]);
  }

  /**
   * Simplify a {@link Type} instance: if it is a wildcard generic type, replace it with its upper
   * bound.
   *
   * <p>It does not support wildcards with several upper bounds.
   *
   * @param type A possibly generic type.
   * @return The type or its simplification.
   */
  public static Type extractUpperBoundFromType(@Var Type type) {
    checkNotNull(type);
    if (type instanceof WildcardType) {
      WildcardType wcType = (WildcardType) type;
      if (wcType.getLowerBounds().length > 0) {
        throw new UnsupportedOperationException(
            "Currently wildcard types with a lower bound like \"" + type + "\" are not supported ");
      }
      Type[] upperBounds = ((WildcardType) type).getUpperBounds();
      if (upperBounds.length != 1) {
        throw new UnsupportedOperationException(
            "Currently only type bounds with one upper bound are supported, not \"" + type + "\"");
      }
      type = upperBounds[0];
    }
    return type;
  }

  public static void produceClassLoadingWarning(
      LogManager logger, Class<?> cls, @Nullable Class<?> type) {
    checkNotNull(logger);
    Package pkg = cls.getPackage();
    String typeName = type == null ? "class" : type.getSimpleName();

    if (cls.isAnnotationPresent(Deprecated.class) || pkg.isAnnotationPresent(Deprecated.class)) {
      logger.logf(
          Level.WARNING,
          "Using %s %s, which is marked as deprecated and should not be used.",
          typeName,
          cls.getSimpleName());

    } else if (cls.isAnnotationPresent(Unmaintained.class)
        || pkg.isAnnotationPresent(Unmaintained.class)) {

      logger.logf(
          Level.WARNING,
          "Using %s %s, which is unmaintained and may not work correctly.",
          typeName,
          cls.getSimpleName());
    }
  }

  public static final com.google.common.base.Predicate<Class<?>> IS_GENERATED =
      pInput -> pInput.getSimpleName().startsWith("AutoValue_");

  /** A builder for class loaders with more features than {@link URLClassLoader}. */
  @CanIgnoreReturnValue
  public abstract static class ClassLoaderBuilder<B extends ClassLoaderBuilder<B>> {
    ClassLoaderBuilder() {}

    /**
     * Set parent of new class loader. If not set the default delegation parent class loader will be
     * used (like {@link URLClassLoader#URLClassLoader(URL[])}.
     */
    public abstract B setParent(ClassLoader parent);

    /**
     * Set sources for classes of new class loader just like for {@link URLClassLoader} (this or
     * {@link #setUrls(URL...)} are required).
     */
    public abstract B setUrls(Iterable<URL> urls);

    /**
     * Set sources for classes of new class loader just like for {@link URLClassLoader} (this or
     * {@link #setUrls(Iterable)} are required).
     */
    public abstract B setUrls(URL... urls);

    /**
     * Set an {@link URLClassLoader} as parent and its URLs from {@link URLClassLoader#getURLs()} as
     * sources for new class loader.
     */
    public B setParentAndUrls(URLClassLoader parent) {
      return setParent(parent).setUrls(parent.getURLs());
    }

    /**
     * Set a predicate that specifies which classes are forced to be loaded by the new class loader
     * and not its parent, even if the latter could load them.
     *
     * <p>The predicate should match the fully-qualified class name. The default is to not match any
     * classes.
     *
     * <p>Normally class loaders follow the parent-first strategy, so they never load classes which
     * their parent could also load. The new class loader follows the child-first strategy for a
     * specific set of classes (as specified by this predicate) and the parent-first strategy for
     * the rest.
     *
     * <p>This feature can be used if you want to load a component with its own class loader (so
     * that it can be garbage collected independently, for example), but the parent class loader
     * also sees the classes.
     */
    public abstract B setDirectLoadClasses(Predicate<String> classes);

    /** See {@link #setDirectLoadClasses(Predicate)}. */
    public B setDirectLoadClasses(Pattern classes) {
      return setDirectLoadClasses(matching(classes));
    }

    /**
     * Set a predicate that specifies for which native libraries we should use a custom lookup for
     * the binary as documented in {@link NativeLibraries}.
     *
     * <p>The predicate should match the library name as given to {@link
     * System#loadLibrary(String)}. The default is to not match any libraries.
     *
     * <p>Note that this is only effective if the new class loader is actually the one that is asked
     * to load the new library. Because Java's class loaders follow the parent-first strategy, it is
     * easy to end up with a parent class loader loading the library, if the parent can see the
     * class(es) that do the loading. In this case, use {@link #setDirectLoadClasses(Predicate)} to
     * ensure the new class loader loads all relevant classes itself.
     */
    public abstract B setCustomLookupNativeLibraries(Predicate<String> libraries);

    /** See {@link #setCustomLookupNativeLibraries(Predicate)}. */
    public B setCustomLookupNativeLibraries(Pattern nativeLibraries) {
      return setCustomLookupNativeLibraries(matching(nativeLibraries));
    }

    /** See {@link #setCustomLookupNativeLibraries(Predicate)}. */
    public B setCustomLookupNativeLibraries(String... nativeLibraries) {
      return setCustomLookupNativeLibraries(ImmutableSet.copyOf(nativeLibraries)::contains);
    }

    @SuppressWarnings("NoFunctionalReturnType")
    private static Predicate<String> matching(Pattern pattern) {
      return s -> pattern.matcher(s).matches();
    }

    abstract ExtendedURLClassLoaderConfiguration autoBuild();

    @CheckReturnValue
    public abstract URLClassLoader build();
  }

  /**
   * Create a class loader that is based on an {@link URLClassLoader} but implements some additional
   * features. This method returns a builder that can be used to configure the new class loader.
   */
  public static ClassLoaderBuilder<?> makeExtendedURLClassLoader() {
    return new AutoValue_ExtendedURLClassLoader_ExtendedURLClassLoaderConfiguration.Builder()
        .setDirectLoadClasses(c -> false)
        .setCustomLookupNativeLibraries(l -> false);
  }

  /**
   * Create a factory at runtime that implements the interface {@code factoryType} and delegates to
   * either a constructor or a static factory method of {@code cls}.
   *
   * <p>The factory interface needs to have exactly one method. The target class needs to have
   * either a single public static method name {@code create}, or a single public constructor. The
   * declared exceptions of the static method/constructor need to be a subset of those of the method
   * of the factory interface, and the same holds for the parameters. Parameters that are annotated
   * with an annotation named {@code Nullable} or {@code NullableDecl} may be missing in the factory
   * interface.
   *
   * @param factoryType The factory interface
   * @param cls The class which should be instantiated by the returned factory
   * @return An implementation of {@code factoryType} that instantiates {@code cls}
   * @throws UnsuitedClassException If the static method/constructor of {@code cls} does not fulfill
   *     the restrictions of the factory interface
   */
  public static <I> I createFactory(Class<I> factoryType, Class<?> cls)
      throws UnsuitedClassException {
    return createFactory(TypeToken.of(factoryType), cls);
  }

  /**
   * Create a factory at runtime that implements the interface {@code factoryType} and delegates to
   * either a constructor or a static factory method of {@code cls}.
   *
   * <p>The factory interface needs to have exactly one method. The target class needs to have
   * either a single public static method name {@code create}, or a single public constructor. The
   * declared exceptions of the static method/constructor need to be a subset of those of the method
   * of the factory interface, and the same holds for the parameters. Parameters that are annotated
   * with an annotation named {@code Nullable} or {@code NullableDecl} may be missing in the factory
   * interface.
   *
   * @param factoryType A type token that represents the factory interface
   * @param cls The class which should be instantiated by the returned factory
   * @return An implementation of {@code factoryType} that instantiates {@code cls}
   * @throws UnsuitedClassException If the static method/constructor of {@code cls} does not fulfill
   *     the restrictions of the factory interface
   */
  public static <I> I createFactory(TypeToken<I> factoryType, Class<?> cls)
      throws UnsuitedClassException {
    Class<? super I> factoryInterface = factoryType.getRawType();
    checkNotNull(cls);
    checkArgument(factoryInterface.isInterface());
    checkArgument(
        factoryInterface.getMethods().length == 1,
        "Factory interface %s does not declare exactly one method",
        factoryType);

    // Get the method we should implement and the relevant information from it.
    Method interfaceMethod = factoryInterface.getMethods()[0];
    TypeToken<?> returnType = factoryType.resolveType(interfaceMethod.getGenericReturnType());
    Class<?>[] allowedExceptions =
        resolve(factoryType, interfaceMethod.getGenericExceptionTypes())
            .map(TypeToken::getRawType)
            .toArray(Class[]::new);
    Parameter[] formalParams = interfaceMethod.getParameters();
    List<TypeToken<?>> formalParamTypes =
        resolve(factoryType, interfaceMethod.getGenericParameterTypes()).collect(toImmutableList());
    for (Entry<TypeToken<?>> entry : ImmutableMultiset.copyOf(formalParamTypes).entrySet()) {
      verify(
          entry.getCount() == 1,
          "Method %s of factory interface %s declares parameter of type %s multiple times",
          interfaceMethod.getName(),
          factoryType,
          entry.getElement());
    }

    // Get the method we should call and check whether it matches.
    if (!Modifier.isPublic(cls.getModifiers())) {
      throw new UnsuitedClassException("class is not public");
    }
    Executable target = getInstantiationMethodForClass(cls);
    if (!returnType.isSupertypeOf(TypeToken.of(target.getAnnotatedReturnType().getType()))) {
      throw new UnsuitedClassException("'%s' does not produce instances of %s", target, returnType);
    }
    String exception = Classes.verifyDeclaredExceptions(target, allowedExceptions);
    if (exception != null) {
      throw new UnsuitedClassException(
          "'%s' declares illegal checked exception %s", target, exception);
    }
    Parameter[] targetParameters = target.getParameters();
    List<TypeToken<?>> targetParamTypes =
        Arrays.stream(targetParameters)
            .map(Parameter::getAnnotatedType)
            .map(AnnotatedType::getType)
            .map(TypeToken::of)
            .collect(toImmutableList());

    // For each parameter of the constructor, this array contains the position of the value
    // in the parameters of the interface method.
    int[] parameterMapping = new int[targetParamTypes.size()];
    boolean[] parameterNullability = new boolean[targetParamTypes.size()];
    for (int i = 0; i < targetParamTypes.size(); i++) {
      boolean targetNullability = isNullable(targetParameters[i]);
      int sourceIndex = formalParamTypes.indexOf(targetParamTypes.get(i));
      boolean sourceNullability =
          (sourceIndex == -1) // parameter not present in interface
              || isNullable(formalParams[sourceIndex]);
      if (sourceNullability && !targetNullability) {
        throw new UnsuitedClassException(
            "'%s' requires parameter of type %s which is not present in factory interface",
            target, targetParamTypes.get(i));
      }
      parameterNullability[i] = sourceNullability && targetNullability;
      parameterMapping[i] = sourceIndex;
    }

    final class FactoryInvocationHandler extends AbstractInvocationHandler {

      @Override
      protected Object handleInvocation(Object pProxy, Method pMethod, Object[] pActualArgs)
          throws Throwable {
        verify(pMethod.equals(interfaceMethod));

        Object[] targetArgs = new Object[parameterMapping.length];
        for (int i = 0; i < parameterMapping.length; i++) {
          Object value = parameterMapping[i] == -1 ? null : pActualArgs[parameterMapping[i]];
          if (value == null && !parameterNullability[i]) {
            throw new NullPointerException(
                String.format(
                    "Value null for parameter %d of type %s in %s",
                    i, interfaceMethod.getGenericParameterTypes()[i], this));
          }
          targetArgs[i] = value;
        }

        try {
          if (target instanceof Method) {
            return ((Method) target).invoke(null, targetArgs);
          } else if (target instanceof Constructor<?>) {
            return ((Constructor<?>) target).newInstance(targetArgs);
          } else {
            throw new AssertionError("Unknown Executable " + target);
          }
        } catch (InvocationTargetException e) {
          throw e.getCause();
        }
      }

      @Override
      public String toString() {
        return factoryType + " implementation for '" + target + "'";
      }
    }

    @SuppressWarnings("unchecked")
    I factory = (I) Reflection.newProxy(factoryInterface, new FactoryInvocationHandler());
    return factory;
  }

  private static Stream<TypeToken<?>> resolve(TypeToken<?> context, Type[] types) {
    return Arrays.stream(types).<TypeToken<?>>map(type -> context.resolveType(type));
  }

  private static boolean isNullable(Parameter elem) {
    return isNullable((AnnotatedElement) elem) || isNullable(elem.getAnnotatedType());
  }

  private static boolean isNullable(AnnotatedElement elem) {
    for (Annotation annotation : elem.getAnnotations()) {
      String name = annotation.annotationType().getSimpleName();
      if (name.equals("Nullable") || name.equals("NullableDecl")) {
        return true;
      }
    }
    return false;
  }

  /**
   * Search for a method that we should use to instantiate the class. First, it looks for a public
   * static method named "create", second, for a public constructor.
   *
   * @throws UnsuitedClassException if no matching method can be found
   */
  private static Executable getInstantiationMethodForClass(Class<?> cls)
      throws UnsuitedClassException {
    List<Method> factoryMethods =
        Arrays.stream(cls.getDeclaredMethods())
            .filter(m -> m.getName().equals("create"))
            .filter(m -> Modifier.isStatic(m.getModifiers()))
            .filter(m -> Modifier.isPublic(m.getModifiers()))
            .filter(m -> !m.isSynthetic())
            .collect(toImmutableList());
    switch (factoryMethods.size()) {
      case 0:
        if (Modifier.isAbstract(cls.getModifiers())) {
          throw new UnsuitedClassException("class is abstract");
        }
        Constructor<?>[] constructors = cls.getConstructors();
        if (constructors.length != 1) {
          throw new UnsuitedClassException(
              "class does not have a static method \"create\" nor exactly one public constructor");
        }
        return constructors[0];

      case 1:
        return factoryMethods.get(0);

      default:
        throw new UnsuitedClassException("class has more than one static methods named \"create\"");
    }
  }

  /**
   * Exception thrown when {@link Classes#createFactory(TypeToken, Class)} is called with a class
   * that does not satisfy the requirements of the factory interface.
   */
  public static final class UnsuitedClassException extends Exception {

    private static final long serialVersionUID = 5091662820905162461L;

    @FormatMethod
    UnsuitedClassException(String msg, Object... args) {
      super(String.format(msg, args));
    }
  }
}
