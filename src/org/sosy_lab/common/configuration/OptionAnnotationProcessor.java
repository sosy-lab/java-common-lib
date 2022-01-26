// This file is part of SoSy-Lab Common,
// a library of useful utilities:
// https://github.com/sosy-lab/java-common-lib
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.common.configuration;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Strings.nullToEmpty;
import static javax.lang.model.util.ElementFilter.constructorsIn;
import static javax.tools.Diagnostic.Kind.ERROR;
import static javax.tools.Diagnostic.Kind.WARNING;

import com.google.auto.service.AutoService;
import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.errorprone.annotations.Var;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Completion;
import javax.annotation.processing.Completions;
import javax.annotation.processing.Processor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.ArrayType;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.PrimitiveType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import javax.tools.Diagnostic;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Annotation processor for checking constraints on {@link Option} and {@link Options} annotations.
 * The compiler uses this class during compilation, and we can report compiler errors and warnings.
 *
 * <p>When reporting warnings, it honors the {@link SuppressWarnings} annotation either with a value
 * of "all" or "options".
 *
 * <p>This class needs to be public and have a public no-arg constructor. However, it is not
 * intended for clients usage.
 */
@SupportedAnnotationTypes("org.sosy_lab.common.configuration.*")
@AutoService(Processor.class)
public class OptionAnnotationProcessor extends AbstractProcessor {

  // The set of known option-detail annotations.
  // For those we can check that @Option is not missing if one of them is present.
  private static final ImmutableSet<Class<? extends Annotation>> KNOWN_OPTION_DETAIL_ANNOTATIONS =
      ImmutableSet.of(
          ClassOption.class, FileOption.class, IntegerOption.class, TimeSpanOption.class);

  @Override
  public SourceVersion getSupportedSourceVersion() {
    return SourceVersion.latestSupported();
  }

  @Override
  public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
    checkNotNull(annotations);
    checkNotNull(roundEnv);

    for (Class<? extends Annotation> annotation : KNOWN_OPTION_DETAIL_ANNOTATIONS) {
      for (Element elem : roundEnv.getElementsAnnotatedWith(annotation)) {
        if (elem.getAnnotation(annotation) == null) {
          // might happen for files with compile errors
          continue;
        }
        processOptionDetailAnnotation(elem, annotation);
      }
    }

    for (Element elem : roundEnv.getElementsAnnotatedWith(Options.class)) {
      if (elem.getAnnotation(Options.class) == null) {
        // might happen for files with compile errors
        continue;
      }
      processOptions(elem);
    }

    for (Element elem : roundEnv.getElementsAnnotatedWith(Option.class)) {
      if (elem.getAnnotation(Option.class) == null) {
        // might happen for files with compile errors
        continue;
      }
      processOption(elem);
      checkOptionDetailAnnotations(elem);
    }
    return false; // recommended by https://errorprone.info/bugpattern/DoNotClaimAnnotations
  }

  /**
   * Check whether an option-detail annotation such as {@link IntegerOption} is accompanied with an
   * {@link Option} annotation.
   */
  private void processOptionDetailAnnotation(Element elem, Class<? extends Annotation> annotation) {
    if (elem.getAnnotation(Option.class) == null) {
      message(
          ERROR,
          elem,
          annotation,
          "Option-detail annotation @"
              + annotation.getSimpleName()
              + " is not valid without an @Option annotation at the same element.");
    }
  }

  /**
   * Checks constraints for {@link Options} annotations. This contains:
   *
   * <ul>
   *   <li>Only classes are allowed to be annotated.
   *   <li>Non-private constructors without a Configuration parameter produce a warning (maybe
   *       injection was forgotten?).
   *   <li>{@link Options} is irrelevant without at least one {link Option}.
   * </ul>
   */
  private void processOptions(Element elem) {
    if (elem.getKind() != ElementKind.CLASS) {
      message(ERROR, elem, Options.class, "@Options annotation can only be used on classes.");
      return;
    }
    TypeElement element = (TypeElement) elem;

    // Check for constructor without Configuration parameter.
    // Private classes and constructors are ignored, these are often used for tests.
    if (!element.getModifiers().contains(Modifier.PRIVATE)) {
      List<ExecutableElement> constructors = constructorsIn(element.getEnclosedElements());
      for (ExecutableElement constructor : constructors) {
        if (constructor.getModifiers().contains(Modifier.PRIVATE)) {
          continue;
        }
        if (constructor.getParameters().isEmpty() && constructors.size() == 1) {
          // Ignore single no-args constructors.
          // These are typically used on small classes that contain only options,
          // and the creator is responsible for injecting them.
          // Even better would be to ignore only implicit default constructors,
          // but I do not know how to detect them.
          continue;
        }

        boolean foundConfigurationParameter =
            constructor.getParameters().stream()
                .anyMatch(
                    param -> typeToString(param.asType()).equals(Configuration.class.getName()));
        if (!foundConfigurationParameter && warningsEnabled(constructor)) {
          message(
              WARNING,
              constructor,
              "Constructor does not receive Configuration instance"
                  + " and may not be able to inject configuration options of this class.");
        }
      }
    }

    // check if there is any @Option inside or in its super classes (for recursive inject)
    @Var boolean foundOption = false;
    @Var TypeElement currentClass = element;
    do {
      if (hasChildWithAnnotation(currentClass, Option.class)) {
        foundOption = true;
        break;
      }
      currentClass = (TypeElement) ((DeclaredType) currentClass.getSuperclass()).asElement();
    } while (currentClass.getSuperclass().getKind() != TypeKind.NONE);

    if (!foundOption && warningsEnabled(element)) {
      message(
          WARNING,
          element,
          Options.class,
          "@Options annotation on class without @Option fields or methods is useless.");
    }
  }

  /**
   * Checks constraints for {@link Option} annotations. This contains:
   *
   * <ul>
   *   <li>Surrounding class needs to have {@link Options}.
   *   <li>Illegal modifiers (static, final for fields).
   *   <li>Correct method signature.
   *   <li>Empty description is discouraged.
   * </ul>
   */
  private void processOption(Element elem) {
    Option option = elem.getAnnotation(Option.class);

    Element cls = elem.getEnclosingElement();
    if (cls.getAnnotation(Options.class) == null) {
      message(
          ERROR,
          elem,
          Option.class,
          "Annotation @Option is meaningless in class"
              + " that does not use configuration-option injection."
              + " Add @Options to surrounding class"
              + " and call Configuration.inject(Object) in constructor.");
    }

    switch (elem.getKind()) {
      case FIELD:
        if (elem.getModifiers().contains(Modifier.FINAL)) {
          message(
              ERROR,
              elem,
              "Modifier final on field annotated with @Option is illegal,"
                  + " as it will be written via reflection.");
        }
        break;
      case METHOD:
        // check signature (parameter count, declared exceptions)
        ExecutableElement method = (ExecutableElement) elem;
        if (method.getParameters().size() != 1) {
          message(
              ERROR, method, "Methods annotated with @Option need to have exactly one parameter.");
        }
        for (TypeMirror exceptionType : method.getThrownTypes()) {
          boolean allowedException =
              isSubtypeOf(exceptionType, RuntimeException.class.getName())
                  || isSubtypeOf(exceptionType, Error.class.getName())
                  || isSubtypeOf(exceptionType, InvalidConfigurationException.class.getName());
          if (!allowedException) {
            message(
                ERROR,
                method,
                "Methods annotated with @Option may not throw " + exceptionType + ".");
          }
        }
        break;
      default:
        message(
            ERROR,
            elem,
            Option.class,
            "Annotation @Option is only allowed for fields and methods.");
    }

    if (elem.getModifiers().contains(Modifier.STATIC)) {
      message(ERROR, elem, "Annotation @Option is not allowed for static members.");
    }

    if (option.description().isEmpty() && warningsEnabled(elem)) {
      AnnotationMirror optionAnnotation = findAnnotationMirror(Option.class, elem).get();
      AnnotationValue value =
          findAnnotationValue(Option.class, "description", optionAnnotation).orElse(null);
      message(
          WARNING,
          elem,
          optionAnnotation,
          value,
          "@Option annotation should not have empty description.");
    }
  }

  /**
   * This method checks constraints of additional option-detail annotations such as {@link
   * IntegerOption}}. The following constraints are checked:
   *
   * <ul>
   *   <li>At most one of theese annotations may be present.
   *   <li>The type of the option must match the applicable types of the annotaton.
   * </ul>
   */
  private void checkOptionDetailAnnotations(Element elem) {
    List<String> usedDetailAnnotations = new ArrayList<>(2);

    for (AnnotationMirror am : elem.getAnnotationMirrors()) {
      // The @SomeTypeOption annotation at the current element
      Element annotation = am.getAnnotationType().asElement();

      // The @OptionDetailAnnotation at the declaration of @SomeTypeOption
      Optional<? extends AnnotationMirror> optionDetailAnnotation =
          findAnnotationMirror(OptionDetailAnnotation.class, annotation);
      if (!optionDetailAnnotation.isPresent()) {
        continue; // not an option-detail annotation
      }

      String annotationName = "@" + annotation.getSimpleName();
      usedDetailAnnotations.add(annotationName);

      // Now we want to compare the type of the option against the types
      // that this option-detail annotation declares as compatible
      // (cf. OptionDetailAnnotation.applicableTo()).

      // Determine type of option as declared in source.
      @Var TypeMirror optionType;
      switch (elem.getKind()) {
        case FIELD:
          optionType = elem.asType();
          break;
        case METHOD:
          ExecutableElement method = (ExecutableElement) elem;
          if (method.getParameters().size() != 1) {
            continue; // error, already reported above
          }
          optionType = method.getParameters().get(0).asType();
          break;
        default:
          continue; // error, prevented by compiler
      }

      // If this option is an array or a collection, get the component type.
      @Var boolean isArray = false;
      @Var boolean isCollection = false;
      if (optionType.getKind() == TypeKind.ARRAY) {
        isArray = true;
        optionType = ((ArrayType) optionType).getComponentType();

      } else {
        String rawTypeName = getRawTypeName(optionType);
        for (Class<?> collectionClass : Configuration.COLLECTIONS.keySet()) {
          // String comparison for type equality (cf. isSubtypeOf())
          if (rawTypeName.equals(collectionClass.getName())) {
            List<? extends TypeMirror> params = ((DeclaredType) optionType).getTypeArguments();
            if (params.size() != 1) {
              // all collections have 1 type parameter, error is reported by compiler itself
              continue;
            }
            isCollection = true;
            optionType = params.get(0);
            break;
          }
        }
      }

      // This is the string we will use for type matching.
      // It is raw and boxed, because we have only class literals in the acceptedClasses list.
      @Var String optionTypeName = getRawTypeName(optionType);

      // Unwrap AnnotatedValue
      if (optionTypeName.equals(AnnotatedValue.class.getName())) {
        List<? extends TypeMirror> params = ((DeclaredType) optionType).getTypeArguments();
        if (params.size() == 1) {
          optionType = params.get(0);
          optionTypeName = getRawTypeName(optionType);
        }
      }

      // acceptedClasses is a List<AnnotationValue>
      // where each AnnotationValue has a TypeMirror/DeclaredType instance as value,
      // because applicableTo is defined as array of Class instances.
      Iterable<?> acceptedClasses =
          (Iterable<?>)
              findAnnotationValue(
                      OptionDetailAnnotation.class, "applicableTo", optionDetailAnnotation.get())
                  .get()
                  .getValue();

      @Var boolean foundMatchingType = false;
      Set<String> acceptedTypeNames = new HashSet<>();

      for (Object listEntry : acceptedClasses) {
        String acceptedType = typeToString((DeclaredType) ((AnnotationValue) listEntry).getValue());
        acceptedTypeNames.add(acceptedType);

        // String comparison for type equality (cf. isSubtypeOf())
        if (optionTypeName.equals(acceptedType)) {
          foundMatchingType = true;
          break;
        }
      }

      if (!Iterables.isEmpty(acceptedClasses) && !foundMatchingType) {
        String msgPrefix;
        if (isArray) {
          msgPrefix = "Array option with incompatible element type";
        } else if (isCollection) {
          msgPrefix = "Option of collection type with incompatible element type";
        } else {
          msgPrefix = "Option with incompatible type";
        }
        message(
            ERROR,
            elem,
            am,
            String.format(
                "%s %s for annotation %s, this annotation is only for types %s.",
                msgPrefix, optionType, annotationName, Joiner.on(", ").join(acceptedTypeNames)));
      }
    }

    if (usedDetailAnnotations.size() > 1) {
      message(
          ERROR,
          elem,
          "Elements annotated with @Option"
              + " might have at most one additional option-detail annotation,"
              + " but this element has the following annotations: "
              + Joiner.on(", ").join(usedDetailAnnotations)
              + ".");
    }
  }

  /**
   * Check whether a type is a sub-type of another one. A type is considered a sub-type of itself.
   *
   * <p>There is the method processingEnv.getTypeUtils().isAssignable(TypeMirror, TypeMirror) that
   * could be used to check whether two types are assignable, but it returns false if the two types
   * are from different compilation units (e.g., one is from this library and the other from the
   * currently compiled source). So we have to do the compatibility check ourselves. We just assume
   * that two types with the same fully qualified name are actually the same type.
   */
  private static boolean isSubtypeOf(@Var TypeMirror type, String superType) {
    checkArgument(type instanceof DeclaredType);
    checkNotNull(superType);

    // We check whether the type is compatible by checking for equality
    // of it and all its super types.
    do {
      // String comparison for type equality as explained above
      if (typeToString(type).equals(superType)) {
        return true;
      }
      type = ((TypeElement) ((DeclaredType) type).asElement()).getSuperclass();
    } while (type.getKind() != TypeKind.NONE);
    return false;
  }

  private static String typeToString(TypeMirror type) {
    if (type.getKind() == TypeKind.DECLARED) {
      // type.toString() can contain type annotations like @Nullable
      return ((TypeElement) ((DeclaredType) type).asElement()).getQualifiedName().toString();
    } else {
      return type.toString();
    }
  }

  /**
   * Check whether there is a {@link SuppressWarnings} annotation on the given element or any of its
   * enclosing elements (class, package) with either the value "all" or "options". Returns true if
   * warnings are not suppressed.
   */
  private static boolean warningsEnabled(@Var Element element) {
    do {
      SuppressWarnings suppress = element.getAnnotation(SuppressWarnings.class);
      if (suppress != null) {
        List<String> values = Arrays.asList(suppress.value());
        if (values.contains("all") || values.contains("options")) {
          return false;
        }
      }
      element = element.getEnclosingElement();
    } while (element != null);

    return true;
  }

  /**
   * Check whether the given element contains any directly enclosed element with a specific
   * annotation.
   */
  private boolean hasChildWithAnnotation(Element element, Class<? extends Annotation> annotation) {
    return element.getEnclosedElements().stream()
        .anyMatch(child -> child.getAnnotation(annotation) != null);
  }

  /**
   * Find the {@link AnnotationMirror} for a given annotation on an {@link Element}. An {@link
   * AnnotationMirror} is the representation of the actual occurence of the annotation in the source
   * code.
   *
   * @param annotation The annotation to look for.
   * @param elem The element that should have the annotation.
   * @return The corresponding {@link AnnotationMirror}, or <code>Optional.empty()</code> if this
   *     element is not annotated with this annotation.
   */
  private Optional<? extends AnnotationMirror> findAnnotationMirror(
      Class<? extends Annotation> annotation, Element elem) {
    String annotationName = annotation.getName();
    return elem.getAnnotationMirrors().stream()
        .filter(am -> am.getAnnotationType().toString().equals(annotationName))
        .findFirst();
  }

  /**
   * Given an {@link AnnotationMirror}, get the value specified for one field of that annotation (if
   * a value for this field was given by the programmer).
   *
   * @param annotationClass The annotation.
   * @param fieldName The name of the field (needs to exist in the annotation).
   * @param annotation The {@link AnnotationMirror} that represents one use of the annotation.
   * @return The corresponding {@link AnnotationValue}, or <code>null</code> if this field was not
   *     specified.
   */
  private static Optional<? extends AnnotationValue> findAnnotationValue(
      Class<? extends Annotation> annotationClass, String fieldName, AnnotationMirror annotation) {
    checkArgument(annotation.getAnnotationType().toString().equals(annotationClass.getName()));

    // check whether annotation declares field
    try {
      @SuppressWarnings("unused")
      Method unused = annotationClass.getDeclaredMethod(checkNotNull(fieldName));
    } catch (NoSuchMethodException e) {
      throw new IllegalArgumentException(e);
    }

    return annotation.getElementValues().entrySet().stream()
        .filter(entry -> entry.getKey().getSimpleName().contentEquals(fieldName))
        .map(Map.Entry::getValue)
        .findFirst();
  }

  /**
   * Given a TypeMirror representing some type, this method produces a String representation of the
   * raw type. It also eliminates primitive types by boxing them.
   */
  private String getRawTypeName(TypeMirror t) {
    @Var TypeMirror type = typeUtils().erasure(t);
    if (type.getKind().isPrimitive()) {
      type = typeUtils().boxedClass((PrimitiveType) type).asType();
    }
    @Var String typeName = typeToString(type);

    // Unfortunately, there is an Eclipse bug in the erasure() method called above:
    // https://bugs.eclipse.org/bugs/show_bug.cgi?id=340635
    // We have to get the raw type ourselves:
    int i = typeName.indexOf('<');
    if (i > 0) {
      typeName = typeName.substring(0, i);
    }
    return typeName;
  }

  @Override
  public Iterable<? extends Completion> getCompletions(
      @Nullable Element element,
      @Nullable AnnotationMirror annotation,
      @Nullable ExecutableElement field,
      @Nullable String userText) {

    if (element == null || annotation == null || field == null) {
      return super.getCompletions(element, annotation, field, userText);
    }

    // This should help to get better auto completion,
    // currently at least for ClassOption.packagePrefix.
    // However, Eclipse seems to not support this, so it is untested.
    if (annotation.getAnnotationType().toString().equals(ClassOption.class.getName())
        && field.getSimpleName().contentEquals("packagePrefix")) {

      return returnPackagePrefixCompletions(element, nullToEmpty(userText));
    }

    return super.getCompletions(element, annotation, field, userText);
  }

  private List<? extends Completion> returnPackagePrefixCompletions(
      Element element, String userText) {
    List<Completion> packages = new ArrayList<>();
    PackageElement pkg = elementUtils().getPackageOf(element);
    if (!pkg.isUnnamed()) {
      @Var String name = pkg.getQualifiedName().toString();
      do {
        if (!name.startsWith(userText)) {
          break;
        }
        packages.add(Completions.of(name));
        int pos = name.lastIndexOf('.');
        name = name.substring(0, Math.max(pos, 0));
      } while (!name.isEmpty());
    }

    return packages;
  }

  private Elements elementUtils() {
    return processingEnv.getElementUtils();
  }

  private Types typeUtils() {
    return processingEnv.getTypeUtils();
  }

  private void message(Diagnostic.Kind level, Element elem, String message) {
    processingEnv.getMessager().printMessage(level, message, elem);
  }

  private void message(
      Diagnostic.Kind level, Element elem, Class<? extends Annotation> annotation, String message) {
    message(level, elem, findAnnotationMirror(annotation, elem).orElse(null), message);
  }

  private void message(
      Diagnostic.Kind level, Element elem, AnnotationMirror annotationMirror, String message) {
    processingEnv.getMessager().printMessage(level, message, elem, annotationMirror);
  }

  private void message(
      Diagnostic.Kind level,
      Element elem,
      AnnotationMirror annotation,
      AnnotationValue value,
      String message) {
    processingEnv.getMessager().printMessage(level, message, elem, annotation, value);
  }
}
